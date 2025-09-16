package tg.vocabu.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import tg.vocabu.bot.TelegramBot;

@Slf4j
@Configuration
public class TelegramBotRegistration {

  @Bean
  public TelegramBotsApi telegramBotsApi(TelegramBot telegramBot) throws TelegramApiException {

    TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
    botsApi.registerBot(telegramBot);

    log.debug("Telegram bot registered successfully!");

    return botsApi;
  }
}
