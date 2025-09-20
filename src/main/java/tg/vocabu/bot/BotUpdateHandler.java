package tg.vocabu.bot;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import tg.vocabu.config.BotConfig;
import tg.vocabu.model.enums.CallbackQuery;
import tg.vocabu.model.enums.Command;
import tg.vocabu.service.CallbackQueryHandler;
import tg.vocabu.service.CommandHandler;

@Slf4j
@Service
@RequiredArgsConstructor
public class BotUpdateHandler {

  private final CommandHandler commandHandler;
  private final CallbackQueryHandler callbackQueryHandler;

  private final BotConfig botConfig;

  public void handle(Update update, TelegramBot bot) {

    if (update.hasMessage() && update.getMessage().hasText()) {

      String text = update.getMessage().getText();
      long chatId = update.getMessage().getChatId();

      SendMessage message = new SendMessage();
      message.setChatId(String.valueOf(chatId));

      Command command = Command.fromString(text);
      boolean isAdmin = chatId == botConfig.getAdminChatId();

      if (command.isAdminOnly() && !isAdmin) {

        log.warn("Unauthorized access attempt by chat: {}", chatId);
        message.setText("❌ You are not authorized to use this command.");

        try {
          bot.execute(message);
        } catch (TelegramApiException e) {
          log.error("Error sending unauthorized message: {}", e.getMessage());
          e.printStackTrace();
        }
        return;
      }

      if (!commandHandler.havePendingCommand(message, chatId, text)) {
        switch (Objects.requireNonNull(command)) {
          case START -> commandHandler.handleStartCommand(isAdmin, message, chatId);
          case HELP -> commandHandler.handleHelpCommand(isAdmin, message, chatId);
          case VOCABULARY -> commandHandler.handleVocabulary(message, chatId);
          case USERS -> commandHandler.handleUsersStatus(message);
          case STATUS -> commandHandler.handleStatusCheckCommand(message, chatId);
          case STATS -> commandHandler.handleStatsCheckCommand(message, chatId);
          case CLEAR -> commandHandler.handleClearCacheCommand(message, chatId);
          default -> commandHandler.handleTranslateCommand(message, chatId, text);
        }
      }

      try {
        bot.execute(message);
      } catch (TelegramApiException e) {
        log.error("Error sending message: {}", e.getMessage());
        e.printStackTrace();
      }

    } else if (update.hasCallbackQuery()) {

      String callbackData = update.getCallbackQuery().getData();
      long chatId = update.getCallbackQuery().getMessage().getChatId();

      log.debug("Received callback query: {}", callbackData);

      switch (CallbackQuery.valueOf(callbackData)) {
        case ADD_TO_VOCABULARY -> callbackQueryHandler.handleAddToVocabulary(bot, chatId);
        case ADD_OWN_TRANSLATION -> callbackQueryHandler.handleAddOwnTranslation(bot, chatId);
      }
    }
  }
}
