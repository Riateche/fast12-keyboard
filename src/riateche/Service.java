package riateche;


//

import android.inputmethodservice.InputMethodService;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputConnection;
 
public class Service extends InputMethodService {

  /**
   * Main initialization of the input method component.  Be sure to call
   * to super class.
   */
  @Override public void onCreate() {
    Log.i("", "onCreate");
    super.onCreate();
  }

  /**
   * Called by the framework when your view for creating input needs to
   * be generated.  This will be called the first time your input method
   * is displayed, and every time it needs to be re-created such as due to
   * a configuration change.
   */
  @Override public View onCreateInputView() {    
    return new KeyboardView(this);
  }
  
  public void typeLetter(String string) {
    InputConnection c = getCurrentInputConnection();
    if (c == null) return;
    for(int i = 0; i < string.length(); i++) {
      sendKeyChar(string.charAt(i));
    }
  }

}
