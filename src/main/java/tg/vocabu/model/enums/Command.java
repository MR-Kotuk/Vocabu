package tg.vocabu.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Command {
  START("/start", false),
  HELP("/help", false),
  VOCABULARY("/vocabulary", false),
  LEARNED("/learned", false),
  EXERCISE("/exercise", false),
  DICTIONARY("/dictionary", false),
  USERS("/users", true),
  USERS_LIST("/users_list", true),
  STATUS("/status", true),
  STATS("/stats", true),
  CLEAR_VOCABULARY("/clear_vocabulary", false),
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
