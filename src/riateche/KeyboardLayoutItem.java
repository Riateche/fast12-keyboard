package riateche;

public class KeyboardLayoutItem {
  private String letter;
  
  enum Command {
    LETTER,
    SPACE,
    HIDE_KEYBOARD, 
    BACKSPACE,
    ENTER
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
  
  public String keyLabel() {
    switch (command) {
    case LETTER:
      return letter;
    case BACKSPACE:
      return "←";
    case ENTER:
      return "↵";
    case HIDE_KEYBOARD:
      return "∅";
    }
    return "?";
  }

  public Command getCommand() {
    return command;
  }

  public String getLetter() {
    return letter;
  }

}
