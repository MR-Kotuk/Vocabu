package tg.vocabu.bot;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import tg.vocabu.config.BotConfig;
import tg.vocabu.model.entity.dto.CallbackQueryDto;
import tg.vocabu.model.enums.CallbackQuery;
import tg.vocabu.model.enums.Command;
import tg.vocabu.service.command.handler.AdminCommandHandler;
import tg.vocabu.service.command.handler.CallbackQueryHandler;
import tg.vocabu.service.command.handler.CommandHandler;
import tg.vocabu.service.command.handler.ExerciseCommandHandler;
import tg.vocabu.service.command.handler.GoogleTranslatorHandler;

@Slf4j
@Service
@RequiredArgsConstructor
public class BotUpdateHandler {

  private final CommandHandler commandHandler;
  private final ExerciseCommandHandler exerciseCommandHandler;
  private final AdminCommandHandler adminCommandHandler;
  private final GoogleTranslatorHandler googleTranslatorHandler;
  private final CallbackQueryHandler callbackQueryHandler;

  private final BotConfig botConfig;

  public void handle(Update update, TelegramBot bot) {

    SendMessage message = new SendMessage();

    if (update.hasMessage() && update.getMessage().hasText()) {

      String text = update.getMessage().getText();
      long chatId = update.getMessage().getChatId();

      message.setChatId(String.valueOf(chatId));

      Command command = Command.fromString(text);
      boolean isAdmin = chatId == botConfig.getAdminChatId();

      if (command.isAdminOnly() && !isAdmin) {

        log.warn("Unauthorized access attempt by chat: {}", chatId);
        message.setText("❌ You are not authorized to use this command.");

      } else if (!commandHandler.havePendingCommand(bot, message, chatId, text)) {

        switch (Objects.requireNonNull(command)) {
          case START -> commandHandler.handleStartCommand(isAdmin, update, message, chatId);
          case HELP -> commandHandler.handleHelpCommand(isAdmin, message, chatId);

          case VOCABULARY -> commandHandler.handleVocabulary(message, chatId);
          case LEARNED -> commandHandler.handleLearned(message, chatId);

          case EXERCISE -> exerciseCommandHandler.handleExercise(message, chatId);

          case DICTIONARY -> commandHandler.handleDictionaryInfoCommand(message, chatId);

          case STATS -> googleTranslatorHandler.handleStatsCheckCommand(message, chatId);
          case STATUS -> googleTranslatorHandler.handleStatusCheckCommand(message, chatId);

          case CLEAR -> googleTranslatorHandler.handleClearCacheCommand(message, chatId);
          case CLEAR_VOCABULARY -> commandHandler.handleClearVocabulary(message, chatId);

          case USERS -> adminCommandHandler.handleUsersStatus(message);
          case USERS_LIST -> adminCommandHandler.handleUserList(message);

          default -> commandHandler.handleTranslateCommand(message, chatId, text);
        }
      }
    } else if (update.hasCallbackQuery()) {

      String callbackData = update.getCallbackQuery().getData();
      long chatId = update.getCallbackQuery().getMessage().getChatId();

      message.setChatId(String.valueOf(chatId));

      log.trace("Received callback query: {}", callbackData);

      CallbackQueryDto callbackQueryDto = new CallbackQueryDto();
      Long data = null;

      if (callbackData.contains(":")) {
        callbackQueryDto = CallbackQueryDto.extract(callbackData);
        data = Long.parseLong(callbackQueryDto.getData());
      } else {
        callbackQueryDto.setCallbackQuery(CallbackQuery.valueOf(callbackData));
      }

      switch (callbackQueryDto.getCallbackQuery()) {
        case ADD_TO_VOCABULARY -> callbackQueryHandler.handleAddToVocabulary(bot, message, chatId, data);
        case ADD_TO_DICTIONARY -> callbackQueryHandler.handleAddSuggestedWordToDictionary(message, data);

        case ADD_OWN_TRANSLATION -> callbackQueryHandler.handleAddOwnTranslation(message, chatId, data);

        case BAN_USER -> callbackQueryHandler.handleBanUser(message, data);
        case UNBAN_USER -> callbackQueryHandler.handleUnbanUser(message, data);

        case START_EXERCISE -> exerciseCommandHandler.handleExercise(message, chatId);
        case SKIP_EXERCISE -> callbackQueryHandler.handleSkipExercise(message, chatId);
        case CORRECT_EXERCISE_ANSWER, WRONG_EXERCISE_ANSWER -> callbackQueryHandler.handleExerciseAnswer(message, chatId, callbackData);

        default -> commandHandler.handleSomethingWentWrong(message);
      }
    }

    try {
      bot.execute(message);
    } catch (TelegramApiException e) {
      log.error("Error sending message: {}", e.getMessage());
      e.printStackTrace();
    }
  }
}
