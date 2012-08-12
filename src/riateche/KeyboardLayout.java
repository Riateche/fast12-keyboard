package riateche;

import android.util.Log;

public class KeyboardLayout {
	private final int width, height;
	private final int arraySize, subArraySize;
	private KeyboardLayoutItem[][] items;
	
	private int pushPositionX = 0;
	private int pushPositionY = 0;

	KeyboardLayout(int width, int height) {
		this.width = width;
		this.height = height;
		arraySize = 4 + (width - 2) * 3;
		subArraySize = 4 + (height - 2) * 3;
		items = new KeyboardLayoutItem[arraySize][subArraySize]; 
	}
	
	private int convert(int button, int posInButton) {
	  return posInButton + button * 3 - 1;
	}
	
	public KeyboardLayoutItem getItemForButton(int buttonX, int buttonY, int posInButtonX, int posInButtonY) {
	  //Log.i("KeyboardLayout", "getLetterForButton " + buttonX + buttonY + posInButtonX + posInButtonY);
	  int x = convert(buttonX, posInButtonX);
    int y = convert(buttonY, posInButtonY);
    if (x < 0 || y < 0 || x >= arraySize || y >= subArraySize) return null;
    //Log.i("KeyboardLayout", "  found x=" + x + " y=" + y);
    return items[x][y];
	}
	 
	public void pushItem(KeyboardLayoutItem letter) throws InvalidLayoutXmlException {
		if (pushPositionY == subArraySize) {
			throw new InvalidLayoutXmlException("KeyboardLayout: too many letters");
		}
		items[pushPositionX][pushPositionY] = letter;
		//Log.i("KeyboardLayout", "push " + letter + " to " + pushPositionX + ", " + pushPositionY);
		pushPositionX++;
		if (pushPositionX == arraySize) {
			pushPositionX = 0;
			pushPositionY++;
		}
	}
}
