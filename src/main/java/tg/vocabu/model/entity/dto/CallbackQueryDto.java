package tg.vocabu.model.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tg.vocabu.model.enums.CallbackQuery;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CallbackQueryDto {

  private CallbackQuery callbackQuery;

  private String data;

  public static CallbackQueryDto extract(String callbackQuery) {

    String[] parts = callbackQuery.split(":", 2);

    return parts.length == 2
        ? new CallbackQueryDto(CallbackQuery.valueOf(parts[0]), parts[1])
        : new CallbackQueryDto(CallbackQuery.valueOf(parts[0]), null);
  }
}
