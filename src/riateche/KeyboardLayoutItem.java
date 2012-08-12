package riateche;

public class KeyboardLayoutItem {
  private String letter;
  
  enum Command {
    LETTER,
    SPACE,
    HIDE_KEYBOARD, 
    BACKSPACE,
    ENTER,
    CAPS_LOCK
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
  
  public String keyLabel(boolean capsEnabled) {
    switch (command) {
    case LETTER:
      return getLetter(capsEnabled);
    case BACKSPACE:
      return "←";
    case SPACE:
      return "□";
    case ENTER:
      return "↵";
    case HIDE_KEYBOARD:
      return "∅";
    case CAPS_LOCK:
      return "⇑";
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

  public String getLetter(boolean capsEnabled) {
    return capsEnabled? letter: letter.toLowerCase();
  }

}
