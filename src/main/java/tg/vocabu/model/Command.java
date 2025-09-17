package tg.vocabu.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Command {
  START("/start", false),
  HELP("/help", false),
  STATUS("/status", true),
  STATS("/stats", true),
  CLEAR("/clear", true),
  UNKNOWN("", false);

  private final String command;
  private final boolean adminOnly;

  public static Command fromString(String text) {

    String commandText = text.split(" ")[0];

    for (Command cmd : Command.values()) {
      if (cmd.command.equalsIgnoreCase(commandText)) {
        return cmd;
      }
    }

    return UNKNOWN;
  }
}
