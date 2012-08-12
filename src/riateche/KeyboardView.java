package riateche;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import riateche.KeyboardLayoutItem.Command;
import riateche.keyboard.R;
import android.content.Context;
import android.content.res.XmlResourceParser;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Paint.Align;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

public class KeyboardView extends View  {
  private static final int singleGestureMaxTime = 200; //milliseconds
  private final Service service;
  private int preferredButtonSize;
  private final int padding = 2;
  private final Point buttonsCount = new Point(4, 3);
  private final Paint paint = new Paint();    
  private final Paint paintBackground = new Paint();    
  private final Paint paintText = new Paint();  
  private boolean hasCurrentButton = false;
  private int currentButtonX, currentButtonY;
  private float currentButtonPositionX, currentButtonPositionY;
  private long gestureStartTime;

  private int buttonSize = 36;
  private int paddingLeft = 0, paddingTop = 0;
  
  private boolean capsEnabled = false;

  //int dpi = DisplayMetrics.DENSITY_DEFAULT;
  //float scalingFactor = 1;
  DisplayMetrics displayMetrics = new DisplayMetrics();
  
  
  private boolean hasCandidateButton = false;
  private int candidateButtonX, candidateButtonY;

  private ArrayList<KeyboardLayout> layouts = new ArrayList<KeyboardLayout>();
  KeyboardLayout currentLayout;

  private GestureDetector gd;
  //private final Point mStartPosition = new Point();
  
  private KeyboardLayoutItem getItemForCandidate(int buttonX, int buttonY) {
    if (!hasCurrentButton) return null;
    int posInButtonX = buttonX - currentButtonX + 1;
    int posInButtonY = buttonY - currentButtonY + 1;
    return currentLayout.getItemForButton(currentButtonX, currentButtonY, posInButtonX, posInButtonY);
  }


  private Rect getButtonRect(int x, int y) {
    int left = paddingLeft + x * (buttonSize + padding);
    int top = paddingTop + y * (buttonSize + padding);
    return new Rect(left, top, left + buttonSize, top + buttonSize);   
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

  public KeyboardView(Service service) {
    super(service);
    this.service = service;
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
    
    WindowManager wm = (WindowManager) service.getSystemService(Context.WINDOW_SERVICE);
    
    if (wm != null) {
      wm.getDefaultDisplay().getMetrics(displayMetrics);
    } else {
      Log.e("KeyboardView", "Failed to get window manager");
    }
    
    preferredButtonSize = (int) (displayMetrics.densityDpi * 0.4);
    buttonSize = preferredButtonSize;
    

    paintBackground.setARGB(255, 255, 255, 255);
    paintBackground.setStyle(Style.FILL); 
    paintText.setTextAlign(Align.CENTER);
    Typeface typeface = Typeface.createFromAsset(service.getAssets(), "font2.ttf"); 
    //paintText.setTypeface(typeface);
    gd = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
      @Override
      public boolean onDown (MotionEvent e) {
        Log.i("", "ondown!");
        if (!hasCurrentButton) {
          for(int i = 0; i < buttonsCount.x; i++) {
            for(int j = 0; j < buttonsCount.y; j++) {
              Rect rect = getButtonRect(i, j);
              if (rect.contains((int) e.getX(), (int) e.getY())) {
                hasCurrentButton = true;
                currentButtonX = i;
                currentButtonY = j;
                currentButtonPositionX = rect.left;
                currentButtonPositionY = rect.top;
                gestureStartTime = System.nanoTime();
                invalidate();
                //bringToFront(); //?
                return true;                            
              }                       
            }
          }
        }
        return false;
      }



      @Override
      public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if (hasCurrentButton) {
          currentButtonPositionX -= distanceX; 
          currentButtonPositionY -= distanceY; 
          double minDistanse = buttonSize * 100;
          Rect currentButtonDefault = getButtonRect(currentButtonX, currentButtonY);
          double selfDistanse = Math.pow(currentButtonDefault.left - currentButtonPositionX, 2) +
              Math.pow(currentButtonDefault.top - currentButtonPositionY, 2);
          if (Math.sqrt(selfDistanse) < buttonSize / 4.0) {
            hasCandidateButton = false;
          } else {
            hasCandidateButton = true;
            for(int i = currentButtonX - 1; i <= currentButtonX + 1; i++) {                     
              for(int j = currentButtonY - 1; j <= currentButtonY + 1; j++) {
                if (i < 0 || j < 0) continue;
                if (i >= buttonsCount.x || j >= buttonsCount.y) continue;
                if (i == currentButtonX && j == currentButtonY) continue;
                Rect rect = getButtonRect(i, j);
                double distanse = Math.pow(rect.left - currentButtonPositionX, 2) +
                    Math.pow(rect.top - currentButtonPositionY, 2);
                Log.i("onScroll", "distance = " + distanse);
                if (distanse < minDistanse) {
                  minDistanse = distanse;
                  candidateButtonX = i;
                  candidateButtonY = j;

                }

              }
            }
          }                   
          invalidate();
        }
        return true;
      } 
    });
  }


  @Override
  protected void onDraw(Canvas canvas) {
    canvas.drawPaint(paintBackground);
    for(int i = 0; i < buttonsCount.x; i++) {
      for(int j = 0; j < buttonsCount.y; j++) {
        boolean candidate = false, currentCandidate = false;
        if (hasCurrentButton) {
          if (i == currentButtonX && j == currentButtonY) {
            continue;
          } else if (Math.abs(i - currentButtonX) <= 1 &&
              Math.abs(j - currentButtonY) <= 1) {
            candidate = true;
          }
        }
        if (hasCandidateButton) {
          if (candidateButtonX == i && candidateButtonY == j) currentCandidate = true;
        }
        if (currentCandidate) {
          paint.setARGB(255, 255, 255, 0);
        } else if (candidate) {
          paint.setARGB(255, 0, 150, 0);          
        } else {
          paint.setARGB(255, 0, 0, 0);                    
        }
        Rect rect = getButtonRect(i, j);
        canvas.drawRect(rect, paint);           
        int td = buttonSize / 3;
        
        if (currentCandidate) {
          paintText.setARGB(255, 0, 0, 0);
        } else {
          paintText.setARGB(255, 255, 255, 255);          
        }
        if (candidate) {
          paintText.setTextSize(buttonSize / 2);
          KeyboardLayoutItem item = getItemForCandidate(i, j);
          canvas.drawText(item.keyLabel(capsEnabled), 
              rect.left + buttonSize / 2,  
              rect.top + buttonSize * 3 / 4, 
              paintText);                       
          
        } else {
          int textSize = buttonSize / 4;
          paintText.setTextSize(textSize);
          int textPaddingY = (int) (( buttonSize - 3 * textSize ) / 4);
          /*Log.i("KeyboardView", "=======");
          Log.i("KeyboardView", "textSize=" + textSize  + " real=" + paint.getTextSize());
          Log.i("KeyboardView", "dpi=" + displayMetrics.densityDpi);
          Log.i("KeyboardView", "density=" + displayMetrics.density);
          Log.i("KeyboardView", "textPaddingY=" + textPaddingY);
          Log.i("KeyboardView", "buttonSize=" + buttonSize);
          Log.i("KeyboardView", "descent=" + paintText.descent()); */
          for(int ti = 0; ti < 3; ti++) {  
            for(int tj = 0; tj < 3; tj++) {
              KeyboardLayoutItem item = currentLayout.getItemForButton(i,  j, ti, tj);
              if (item != null) {
                if (ti == 1 && tj == 1) {
                  paintText.setARGB(255, 0, 0, 200);
                } else if (item.getCommand() != Command.LETTER) {
                  paintText.setARGB(255, 200, 0, 0);                  
                } else {
                  paintText.setARGB(255, 200, 200, 200);                  
                }
                canvas.drawText(item.keyLabel(capsEnabled), 
                    rect.left + (ti + 1) * buttonSize / 4, 
                    rect.top + (tj + 1) * (textPaddingY + textSize) - paintText.descent(), 
                    paintText);                       
              }
            }
          }
        }
      }
    }
    /*if (hasCurrentButton) {
      int left = (int) currentButtonPositionX;
      int top =  (int) currentButtonPositionY;
      Rect rect = getButtonRect(currentButtonX, currentButtonY);
      int d = buttonSize + padding;
      if (left < rect.left - d) left = rect.left - d;
      if (left > rect.left + d) left = rect.left + d;
      if (top < rect.top - d) top = rect.top - d;
      if (top > rect.top + d) top = rect.top + d;
      if (left < paddingLeft) left = paddingLeft;
      int max_left = getButtonRect(buttonsCount.x - 1, 0).left;
      if (left > max_left) left = max_left;
      if (top < paddingTop) top = paddingTop;
      int max_top = getButtonRect(buttonsCount.y - 1, 0).top;
      if (top > max_top) top = max_top;  
      paint.setARGB(50, 0, 0, 0);
      canvas.drawRect(left, top, left + buttonSize, top + buttonSize, paint);                          
    }*/
  }
  
  private int getMaxButtonSize(int maxTotalWidth, int maxTotalHeight) {
    int maxButtonSizeX = (maxTotalWidth - padding * (buttonsCount.x + 1)) / buttonsCount.x; 
    int maxButtonSizeY = (maxTotalHeight - padding * (buttonsCount.y + 1)) / buttonsCount.y;
    int maxButtonSize = maxButtonSizeX > maxButtonSizeY ? maxButtonSizeY : maxButtonSizeX;
    return preferredButtonSize > maxButtonSize ? maxButtonSize : preferredButtonSize;    
  }

  @Override
  protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
    int x = MeasureSpec.getSize(widthMeasureSpec);
    int y = MeasureSpec.getSize(heightMeasureSpec);
    int newButtonSize = getMaxButtonSize(x, y / 2);        
    setMeasuredDimension(x, padding + buttonsCount.y * (padding + newButtonSize));
    //setMeasuredDimension(padding + buttonsCount.x * (padding + buttonSize), 
    //    padding + buttonsCount.y * (padding + buttonSize));
  }
  
  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    buttonSize = getMaxButtonSize(w, h);
    paddingLeft = (w - buttonSize * buttonsCount.x - padding * (buttonsCount.x - 1)) / 2;
    paddingTop =  (h - buttonSize * buttonsCount.y - padding * (buttonsCount.y - 1)) / 2;
    
    invalidate();    
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    boolean r = gd.onTouchEvent(event);
    if (!r && event.getAction() == MotionEvent.ACTION_UP) {
      KeyboardLayoutItem item = null;
      if (hasCandidateButton) {
        item = getItemForCandidate(candidateButtonX, candidateButtonY);
      } else {
        if (System.nanoTime() - gestureStartTime < singleGestureMaxTime * 1e6) {
          item = currentLayout.getItemForButton(currentButtonX, currentButtonY, 1, 1);          
        }
      }      
      if (item != null) {
        switch (item.getCommand()) {
        case LETTER:
          service.typeLetter(item.getLetter(capsEnabled));           
          break;
        case SPACE:
          service.typeLetter(" ");           
          break;
        case ENTER:
          service.typeLetter("\n");           
          break;
        case CAPS_LOCK:
          capsEnabled = !capsEnabled;
          invalidate();
          break;
        case BACKSPACE:
          service.sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
          break;
        case HIDE_KEYBOARD:
          service.requestHideSelf(0);
          break;
        }
      }
      hasCurrentButton = false;
      hasCandidateButton = false;
      invalidate();
      return true;
    }
    return r;
  }

}
