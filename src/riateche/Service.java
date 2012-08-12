package riateche;


//

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import riateche.keyboard.R;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.XmlResourceParser;
import android.inputmethodservice.InputMethodService;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputConnection;
 
public class Service extends InputMethodService {

  private ArrayList<KeyboardLayout> layouts = new ArrayList<KeyboardLayout>();
  private KeyboardLayout currentLayout;
  
  KeyboardView keyboardView = null;
  
  /**
   * Main initialization of the input method component.  Be sure to call
   * to super class.
   */
  @Override public void onCreate() {
    //Log.i("", "onCreate");
    super.onCreate();
    readLayouts();
  }

  /**
   * Called by the framework when your view for creating input needs to
   * be generated.  This will be called the first time your input method
   * is displayed, and every time it needs to be re-created such as due to
   * a configuration change.
   */
  @Override public View onCreateInputView() {    
    keyboardView = new KeyboardView(this);
    return keyboardView;
  }
  
  public void typeLetter(String string) {
    InputConnection c = getCurrentInputConnection();
    if (c == null) return;
    for(int i = 0; i < string.length(); i++) {
      sendKeyChar(string.charAt(i));
    }
  }
  
  private Map<String,String>  getAttributes(XmlPullParser parser) {
    Map<String,String> attrs=null;
    int acount=parser.getAttributeCount();
    if(acount != -1) {
      //Log.d(MY_DEBUG_TAG,"Attributes for ["+parser.getName()+"]");
      attrs = new HashMap<String,String>(acount);
      for(int x=0;x<acount;x++) {
        //Log.d(MY_DEBUG_TAG,"\t["+parser.getAttributeName(x)+"]=" +
        //       "["+parser.getAttributeValue(x)+"]");
        attrs.put(parser.getAttributeName(x), parser.getAttributeValue(x));
      }
    }
    return attrs;
  }
  
  private void readLayouts() {
    try {
      XmlResourceParser parser = getResources().getXml(R.xml.keyboard_layouts);
      int eventType = parser.getEventType();
      KeyboardLayout layout = null;
      boolean inKey = false;
      while (eventType != XmlPullParser.END_DOCUMENT) {
        //System.out.println("loop" + eventType);
        //if(eventType == XmlPullParser.START_DOCUMENT) {
        //    System.out.println("Start document");
        //} else 
        if (eventType == XmlPullParser.START_TAG) {
          if (parser.getName().equals("layout")) {
            Map<String,String> attrs = getAttributes(parser);
            if (!attrs.containsKey("width") || !attrs.containsKey("height")) {
              Log.e("KeyboardView", "Layout attributes missing");
              throw new InvalidLayoutXmlException("Layout attributes missing");
            }
            layout = new KeyboardLayout(Integer.parseInt(attrs.get("width")), 
                Integer.parseInt(attrs.get("height")));
          } else if (parser.getName().equals("key")) {
            Map<String,String> attrs = getAttributes(parser);
            if (attrs.containsKey("command")) {
              layout.pushItem(new KeyboardLayoutItem(KeyboardLayoutItem.Command.valueOf(attrs.get("command"))));
            }
            inKey = true;
          }

          // System.out.println("Start tag "+parser.getName());
        } else if(eventType == XmlPullParser.END_TAG) {
          if (parser.getName().equals("layout")) {
            layouts.add(layout);
            layout = null;
          } else if (parser.getName().equals("key")) {
            inKey = false;
          }    
          //System.out.println("End tag "+parser.getName());
        } else if(eventType == XmlPullParser.TEXT) {
          //System.out.println("Text "+parser.getText());
          if (inKey) {
            if (layout == null) {
              throw new InvalidLayoutXmlException("Unexpected 'key' tag");                            
            }
            layout.pushItem(new KeyboardLayoutItem(parser.getText()));
          }
        }
        eventType = parser.next();
      }
      if (layouts.isEmpty()) {
        throw new InvalidLayoutXmlException("No layouts found");                            
      }
      currentLayout = layouts.get(0);
    } catch (IOException e) {
      Log.e("KeyboardView", "Failed to parse XML: " + e);
    } catch (XmlPullParserException e) {
      Log.e("KeyboardView", "Failed to parse XML: " + e);
    } catch (InvalidLayoutXmlException e) {
      Log.e("KeyboardView", "Failed to parse XML: " + e);     
    }       
  }
  
  public void executeCommand(KeyboardLayoutItem item, boolean capsEnabled, boolean shiftPressed) {
    switch (item.getCommand()) {
    case LETTER:
      boolean capitalization = capsEnabled;
      if (shiftPressed) {
        capitalization = !capitalization;
      }
      typeLetter(item.getLetter(capitalization));           
      break;
    case SPACE:
      typeLetter(" ");           
      break;
    case ENTER:
      typeLetter("\n");           
      break;
    case BACKSPACE:
      if (shiftPressed) {
        AlertDialog ad = new AlertDialog.Builder(keyboardView.getContext())
        .setIcon(android.R.drawable.ic_dialog_alert)
        .setTitle(R.string.app_name)
        .setMessage(R.string.clear_all_warning)
        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              InputConnection ic = getCurrentInputConnection();
              ic.performContextMenuAction(android.R.id.selectAll);
              sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);        
            }
        }).setNegativeButton(R.string.no, null).create();
        Window window = ad.getWindow(); 
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.token = keyboardView.getWindowToken();
        lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
        window.setAttributes(lp);
        window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        ad.show();

      } else {
        sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
      }
      break;
    case HIDE_KEYBOARD:
      requestHideSelf(0);
      break;
    }
  }

  public ArrayList<KeyboardLayout> getLayouts() {
    return layouts;
  }

  public KeyboardLayout getCurrentLayout() {
    return currentLayout;
  }

  public void setCurrentLayout(KeyboardLayout currentLayout) {
    this.currentLayout = currentLayout;
  }
}
