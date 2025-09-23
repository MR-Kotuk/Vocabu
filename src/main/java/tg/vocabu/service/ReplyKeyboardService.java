package tg.vocabu.service;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReplyKeyboardService {

  public void getDefaultKeyboard(SendMessage message) {

    if (message.getReplyMarkup() == null) {

      ReplyKeyboardMarkup replyKeyboard = new ReplyKeyboardMarkup();
      replyKeyboard.setResizeKeyboard(true);
      replyKeyboard.setOneTimeKeyboard(false);

      KeyboardRow row1 = new KeyboardRow();
      row1.add("/vocabulary");
      row1.add("/learned");

      KeyboardRow row2 = new KeyboardRow();
      row2.add("/exercise");

      List<KeyboardRow> rows = new ArrayList<>();
      rows.add(row1);
      rows.add(row2);

      replyKeyboard.setKeyboard(rows);
      message.setReplyMarkup(replyKeyboard);
    }
  }
}
