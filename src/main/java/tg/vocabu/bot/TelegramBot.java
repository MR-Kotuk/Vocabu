package tg.vocabu.bot;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import tg.vocabu.config.BotConfig;

@Component
@RequiredArgsConstructor
public class TelegramBot extends TelegramLongPollingBot {

  private final BotUpdateHandler updateHandler;
  private final BotConfig botConfig;

  @Override
  public void onUpdateReceived(Update update) {
    updateHandler.handle(update, this);
  }

  @Override
  public String getBotUsername() {
    return botConfig.getUsername();
  }

  @Override
  public String getBotToken() {
    return botConfig.getToken();
  }
}
