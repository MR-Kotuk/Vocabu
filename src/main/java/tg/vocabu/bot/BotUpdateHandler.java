package tg.vocabu.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Service
public class BotUpdateHandler {

  public void handle(Update update, TelegramBot bot) {

    if (update.hasMessage() && update.getMessage().hasText()) {

      String text = update.getMessage().getText();
      long chatId = update.getMessage().getChatId();

      SendMessage message = new SendMessage();
      message.setChatId(String.valueOf(chatId));

      if (text.equals("/start")) {
        log.debug("Received start command from chat: {}", chatId);
        message.setText("Hello! Welcome to Vocabu bot!\n\nI can help you learn new vocabulary!");
      } else {
        log.debug("Received message: {}, from chat: {}", text, chatId);
        message.setText("You said: " + text + "\n\nI'm still learning! Soon I'll help you with vocabulary practice!");
      }

      try {
        bot.execute(message);
      } catch (TelegramApiException e) {
        log.error("Error sending message: {}", e.getMessage());
        e.printStackTrace();
      }
    } else if (update.hasCallbackQuery()) {
      log.debug("Received callback query: {}", update.getCallbackQuery().getData());
    }
  }
}
