package tg.vocabu.model.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import tg.vocabu.model.enums.CallbackQuery;

@Getter
@AllArgsConstructor
public class CallbackQueryDto {

  private final CallbackQuery callbackQuery;

  private final String data;

  public static CallbackQueryDto extract(String callbackQuery) {

    String[] parts = callbackQuery.split(":", 2);

    return parts.length == 2
        ? new CallbackQueryDto(CallbackQuery.valueOf(parts[0]), parts[1])
        : new CallbackQueryDto(CallbackQuery.valueOf(parts[0]), null);
  }
}
