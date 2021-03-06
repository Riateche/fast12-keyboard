package riateche;

public class KeyboardLayoutItem {
  private String letter;
  private int layoutNumber;
  
  enum Command {
    LETTER,
    SPACE,
    HIDE_KEYBOARD, 
    BACKSPACE,
    ENTER,
    CAPS_LOCK,
    SWITCH_LAYOUT,
    SETTINGS,
    BLANK
  }
  
  private Command command;
  
  public KeyboardLayoutItem(String letter) {
    this.letter = letter;
    this.command = Command.LETTER;
  }
  
  public KeyboardLayoutItem(Command command) {
    this.command = command;
  }
  
  @Override
  public String toString() {
    if (command == Command.LETTER) {
      return "KeyboardLayoutItem(letter: '" + letter + "')";      
    } else {
      return "KeyboardLayoutItem(command: " + command.name() + ")";
    }
  }
  
  public String keyLabel(boolean capsEnabled, boolean shiftPressed) {
    switch (command) {
    case LETTER:
      boolean upperCase = capsEnabled;
      if (shiftPressed) upperCase = !upperCase;
      return getLetter(upperCase);
    case BACKSPACE:
      if (shiftPressed) {
        return "CL";      
      } else {
        return "←";
      }
    case SPACE:
      return "□";
    case ENTER:
      return "↵";
    case HIDE_KEYBOARD:
      return "∅";
    case CAPS_LOCK:
      return "⇑";
    case SWITCH_LAYOUT:
      return "L";
    case BLANK:
      return "";
    }
    return "?";
  }
  
  public boolean canCaps() {
    if (command != Command.LETTER) return false;
    return ! letter.toLowerCase().equals(letter);
  }

  public Command getCommand() {
    return command;
  }

  public String getLetter(boolean upperCase) {
    return upperCase? letter: letter.toLowerCase();
  }

  public int getLayoutNumber() {
    return layoutNumber;
  }

  public void setLayoutNumber(int layoutNumber) {
    this.layoutNumber = layoutNumber;
  }

}
