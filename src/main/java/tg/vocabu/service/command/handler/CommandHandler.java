package tg.vocabu.service.command.handler;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import tg.vocabu.bot.TelegramBot;
import tg.vocabu.config.BotConfig;
import tg.vocabu.config.FilePathConfig;
import tg.vocabu.exception.TranslationException;
import tg.vocabu.model.entity.dto.CallbackQueryDto;
import tg.vocabu.model.entity.temp.CallbackQueryTemp;
import tg.vocabu.model.entity.temp.WordTemp;
import tg.vocabu.model.entity.user.User;
import tg.vocabu.model.entity.user.Word;
import tg.vocabu.model.enums.CallbackQuery;
import tg.vocabu.model.enums.LanguageCode;
import tg.vocabu.model.enums.TranslationMethod;
import tg.vocabu.repository.CallbackQueryRepository;
import tg.vocabu.repository.UserRepository;
import tg.vocabu.repository.WordRepository;
import tg.vocabu.repository.WordTempRepository;
import tg.vocabu.service.GoogleTranslationService;
import tg.vocabu.service.ReplyKeyboardService;
import tg.vocabu.service.TranslationService;
import tg.vocabu.util.FileReader;
import tg.vocabu.util.LanguageUtil;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommandHandler {

  private final BotConfig botConfig;

  private final TranslationService translationService;
  private final GoogleTranslationService googleTranslatorService;
  private final ReplyKeyboardService replyKeyboardService;

  private final UserRepository userRepository;
  private final WordRepository wordRepository;
  private final WordTempRepository wordTempRepository;
  private final CallbackQueryRepository callbackQueryRepository;

  public void handleWordSuggestion(TelegramBot bot, Word word, Long chatId) {

    if (userRepository.isBanned(chatId)) {
      return;
    }

    log.trace("Received word suggestion command from chat: {}", chatId);

    User user = userRepository.findById(chatId).orElse(null);

    if (user == null) {
      log.error("User not found for chat ID: {}", chatId);
      return;
    }

    SendMessage message = new SendMessage();
    message.setChatId(String.valueOf(botConfig.getAdminChatId()));

    InlineKeyboardButton addToDictionary = new InlineKeyboardButton("Add to dictionary");
    addToDictionary.setCallbackData(CallbackQuery.ADD_TO_DICTIONARY + ":" + word.getId());

    InlineKeyboardButton banUser = new InlineKeyboardButton("Ban user");
    banUser.setCallbackData(CallbackQuery.BAN_USER + ":" + user.getId());

    List<InlineKeyboardButton> firstRow = List.of(addToDictionary);
    List<InlineKeyboardButton> secondRow = List.of(banUser);

    List<List<InlineKeyboardButton>> rows = new ArrayList<>(List.of(firstRow, secondRow));

    InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
    markup.setKeyboard(rows);

    message.setReplyMarkup(markup);
    message.setText(
        "New word suggestion from user: @"
            + user.getUserName()
            + "\nID: "
            + user.getId()
            + "\n\n"
            + LanguageUtil.formatTranslationPair(word.getEnglish(), word.getUkrainian()));

    replyKeyboardService.getDefaultKeyboard(message);

    try {
      bot.execute(message);
    } catch (Exception e) {
      log.error("Failed to send word suggestion to admin: {}", e.getMessage());
    }
  }

  public void handleClearVocabulary(SendMessage message, Long chatId) {

    log.trace("Received clear vocabulary command from chat: {}", chatId);

    wordRepository.deleteByChatId(chatId);
    message.setText("🗑️ Vocabulary Cleared\n\nYour vocabulary has been cleared successfully.");

    replyKeyboardService.getDefaultKeyboard(message);
  }

  public void handleDictionaryInfoCommand(SendMessage message, Long chatId) {

    log.trace("Received dictionary info command from chat: {}", chatId);

    message.setText("🔧 Dictionary Info\n\nWords count: " + translationService.getWordsCount());

    replyKeyboardService.getDefaultKeyboard(message);
  }

  public void handleSomethingWentWrong(SendMessage message) {

    log.error("Something went wrong");
    message.setText("❌ Something went wrong. Please try again later.");
  }

  public void handleVocabulary(SendMessage message, Long chatId) {

    log.trace("Received vocabulary command from chat: {}", chatId);

    List<Word> words = wordRepository.findByChatIdAndLearned(chatId, false);
    String vocabularyHeader = "==🇬🇧== Vocabulary ==🇺🇦== - % -\n\n";

    if (words != null && !words.isEmpty()) {
      message.setText(generateWordsListTable(vocabularyHeader, words, true));
    } else {
      message.setText(vocabularyHeader + "\nNothing yet...");
    }
  }

  public void handleLearned(SendMessage message, Long chatId) {

    log.trace("Received learned command from chat: {}", chatId);

    List<Word> words = wordRepository.findByChatIdAndLearned(chatId, true);
    String vocabularyHeader = "==🇬🇧== Learned Words ==🇬🇧==\n\n";

    if (words != null && !words.isEmpty()) {
      message.setText(generateWordsListTable(vocabularyHeader, words, false));
    } else {
      message.setText(vocabularyHeader + "\nNothing yet...");
    }
  }

  private String generateWordsListTable(String header, List<Word> words, boolean includeScore) {

    StringBuilder table = new StringBuilder();
    table.append(header);

    for (Word word : words) {

      StringBuilder wordLine = table.append(word.getEnglish()).append("  ➜  ").append(word.getUkrainian());

      if (includeScore) {
        wordLine.append("  -  ").append(word.getScore()).append("%");
      }

      wordLine.append("\n");
    }

    return table.toString();
  }

  public void handleStartCommand(boolean isAdmin, Update update, SendMessage message, Long chatId) {

    log.trace("Received start command from chat: {}", chatId);

    if (!userRepository.existsByChatId(chatId)) {
      org.telegram.telegrambots.meta.api.objects.User tgUser = update.getMessage().getFrom();

      userRepository.save(
          User.builder()
              .id(tgUser.getId())
              .chatId(chatId)
              .firstName(tgUser.getFirstName())
              .lastName(tgUser.getLastName())
              .userName(tgUser.getUserName())
              .languageCode(tgUser.getLanguageCode())
              .isBot(tgUser.getIsBot())
              .build());

      log.info("New user registered");
    }

    String filePath = isAdmin ? FilePathConfig.ADMIN_START_MESSAGE_FILE : FilePathConfig.START_MESSAGE_FILE;
    message.setText(FileReader.readFile(filePath));

    replyKeyboardService.getDefaultKeyboard(message);
  }

  public void handleHelpCommand(boolean isAdmin, SendMessage message, Long chatId) {

    log.trace("Received help command from chat: {}", chatId);

    String filePath = isAdmin ? FilePathConfig.ADMIN_HELP_MESSAGE_FILE : FilePathConfig.HELP_MESSAGE_FILE;
    message.setText(FileReader.readFile(filePath));

    replyKeyboardService.getDefaultKeyboard(message);
  }

  public boolean havePendingCommand(TelegramBot bot, SendMessage message, Long chatId, String text) {

    log.trace("Checking for pending command for chat: {}", chatId);
    CallbackQueryTemp callbackQueryTemp = callbackQueryRepository.findById(chatId).orElse(null);

    if (callbackQueryTemp != null) {

      callbackQueryRepository.delete(callbackQueryTemp);
      CallbackQueryDto callbackQueryDto = CallbackQueryDto.extract(callbackQueryTemp.getCallbackQuery());

      switch (callbackQueryDto.getCallbackQuery()) {
        case ADD_OWN_TRANSLATION -> {
          Long wordTempId = Long.valueOf(callbackQueryDto.getData());
          return handleAddWithOwnTranslation(bot, message, chatId, wordTempId, text);
        }
      }
    }

    return false;
  }

  private boolean handleAddWithOwnTranslation(TelegramBot bot, SendMessage message, Long chatId, Long wordTempId, String text) {

    WordTemp wordTemp = wordTempRepository.findById(wordTempId).orElse(null);

    if (wordTemp != null) {

      wordTempRepository.delete(wordTemp);

      if (!wordRepository.existsByEnglishOrUkrainian(wordTemp.getEnglish(), text)) {

        Word word = Word.builder().chatId(chatId).english(wordTemp.getEnglish()).ukrainian(text).score(0).build();
        wordRepository.save(word);

        if (wordTemp.getTranslationMethod() == TranslationMethod.GOOGLE_TRANSLATOR) {
          handleWordSuggestion(bot, word, chatId);
        }

        message.setText(
            "Added word with own translation to vocabulary:\n\n" + LanguageUtil.formatTranslationPair(word.getEnglish(), word.getUkrainian()));

        replyKeyboardService.getDefaultKeyboard(message);
      } else {
        message.setText("This word pair already exists in your vocabulary.");
      }

      return true;
    }

    return false;
  }

  public void handleTranslateCommand(SendMessage message, Long chatId, String text) {

    if (text.startsWith("/")) {
      message.setText("❌ Unknown command. Type /help for assistance.");
      return;
    }

    log.trace("Received translate command from chat: {}", chatId);

    String[] words = text.trim().split("\\s+");

    if (words.length > 3) {
      message.setText("❌ Text too long! Supports only max 3 words.");
      return;
    }

    log.debug("Translating text: '{}'", text);

    String eng, ukr;
    LanguageCode detected;

    WordTemp wordTemp;
    TranslationMethod translationMethod;

    try {
      detected = translationService.detectLanguage(text);

      if (detected.equals(LanguageCode.UNKNOWN)) {

        log.error("Unsupported language detected");
        message.setText(
            """
            ❌ Unsupported language detected

            Currently supports only English 🇬🇧 and Ukrainian 🇺🇦.
            """);
        return;
      }

      TranslationResult translationResultEng = translate(text, detected, LanguageCode.EN);
      TranslationResult translationResultUkr = translate(text, detected, LanguageCode.UK);

      eng = translationResultEng.text;
      ukr = translationResultUkr.text;

      translationMethod = translationResultEng.method() != null ? translationResultEng.method() : translationResultUkr.method();
      wordTemp = WordTemp.builder().chatId(chatId).english(eng).ukrainian(ukr).translationMethod(translationMethod).build();

      wordTempRepository.save(wordTemp);

    } catch (TranslationException e) {
      log.error("Translation failed: {}", e.getMessage());
      message.setText("❌ Translation Failed\n\n" + "Error: " + e.getMessage());
      return;
    }

    log.trace("Translation successful");

    InlineKeyboardButton addToVocabulary = new InlineKeyboardButton("Add to vocabulary");
    addToVocabulary.setCallbackData(CallbackQuery.ADD_TO_VOCABULARY + ":" + wordTemp.getId());

    InlineKeyboardButton addWithOwnTranslation = new InlineKeyboardButton("Add with own translation");
    addWithOwnTranslation.setCallbackData(CallbackQuery.ADD_OWN_TRANSLATION + ":" + wordTemp.getId());

    List<InlineKeyboardButton> firstRow = List.of(addToVocabulary);
    List<List<InlineKeyboardButton>> rows = new ArrayList<>(List.of(firstRow));

    if (detected == LanguageCode.EN) {
      List<InlineKeyboardButton> secondRow = List.of(addWithOwnTranslation);
      rows.add(secondRow);
    }

    InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
    markup.setKeyboard(rows);

    message.setParseMode("HTML");
    message.setText(LanguageUtil.formatTranslationPair(eng, ukr) + "\n\n<i>Note: Translated by " + translationMethod.getName() + "</i>");
    message.setReplyMarkup(markup);
  }

  private TranslationResult translate(String text, LanguageCode detected, LanguageCode target) throws TranslationException {

    if (detected == target) {
      return new TranslationResult(text, null);
    }

    String translated;
    TranslationMethod translationMethod;

    try {
      translated = translationService.translate(text);
      translationMethod = TranslationMethod.DICTIONARY;
      log.debug("Word found in dictionary: '{}'", translated);

    } catch (TranslationException translationException) {

      log.debug("Not found word in dictionary, trying to translate via API: {}", translationException.getMessage());
      translated = googleTranslatorService.translate(text, detected.getCode(), target.getCode());
      translationMethod = TranslationMethod.GOOGLE_TRANSLATOR;
    }

    return new TranslationResult(translated, translationMethod);
  }

  public record TranslationResult(String text, TranslationMethod method) {}
}
