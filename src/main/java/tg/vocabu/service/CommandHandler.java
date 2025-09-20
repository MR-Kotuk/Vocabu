package tg.vocabu.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import tg.vocabu.config.FilePathConfig;
import tg.vocabu.exception.TranslationException;
import tg.vocabu.model.entity.CallbackQueryTemp;
import tg.vocabu.model.entity.User;
import tg.vocabu.model.entity.Word;
import tg.vocabu.model.entity.WordTemp;
import tg.vocabu.model.enums.CallbackQuery;
import tg.vocabu.model.enums.LanguageCode;
import tg.vocabu.repository.CallbackQueryRepository;
import tg.vocabu.repository.UserRepository;
import tg.vocabu.repository.WordRepository;
import tg.vocabu.repository.WordTempRepository;
import tg.vocabu.util.FileReader;
import tg.vocabu.util.LanguageUtil;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommandHandler {

  private final GoogleTranslationService translationService;
  private final ReplyKeyboardService replyKeyboardService;

  private final UserRepository userRepository;
  private final WordRepository wordRepository;
  private final WordTempRepository wordTempRepository;
  private final CallbackQueryRepository callbackQueryRepository;

  public void handleSomethingWentWrong(SendMessage message) {

    log.error("Something went wrong");
    message.setText("❌ Something went wrong. Please try again later.");
  }

  public void handleUsersStatus(SendMessage message) {

    log.trace("Received users status command from admin");

    long usersCount = userRepository.count();
    message.setText("Users count: " + usersCount);
  }

  public void handleVocabulary(SendMessage message, Long chatId) {

    log.trace("Received vocabulary command from chat: {}", chatId);

    List<Word> words = wordRepository.findByChatIdAndLearned(chatId, false);
    String vocabularyHeader = "==🇬🇧== Vocabulary ==🇺🇦== - % -\n\n";

    if (words != null && !words.isEmpty()) {

      StringBuilder table = new StringBuilder();
      table.append(vocabularyHeader);

      for (Word word : words) {
        table.append(word.getEnglish()).append("  ➜  ").append(word.getUkrainian()).append("  -  ").append(word.getScore()).append("%\n");
      }

      message.setText(table.toString());
    } else {
      message.setText(vocabularyHeader + "\nNothing yet...");
    }
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

  public void handleStatusCheckCommand(SendMessage message, Long chatId) {

    log.trace("Received status check command from chat: {}", chatId);

    try {
      String status = translationService.getStatus();
      message.setText("🔧 Service Status\n\n" + status + "\n\n💡 If offline, try again in a few minutes. Google may temporarily limit requests.");

      replyKeyboardService.getDefaultKeyboard(message);
    } catch (Exception e) {
      log.error("Status check failed: {}", e.getMessage());
      message.setText("⚠️ Status Check Failed\n\n" + "Could not check Google Translate status.\n" + "Error: " + e.getMessage());
    }
  }

  public void handleClearCacheCommand(SendMessage message, Long chatId) {

    log.trace("Received clear cache command from chat: {}", chatId);

    try {
      translationService.clearCache();
      message.setText(
          "🗑️ Cache Cleared\n\n"
              + "Translation cache has been cleared successfully.\n"
              + "Next translations will be fetched fresh from Google Translate.");

      replyKeyboardService.getDefaultKeyboard(message);
    } catch (Exception e) {
      log.error("Cache clearing failed: {}", e.getMessage());
      message.setText("❌ Could not clear cache: " + e.getMessage());
    }
  }

  public void handleStatsCheckCommand(SendMessage message, Long chatId) {

    log.trace("Received stats check command from chat: {}", chatId);

    try {
      Map<String, Object> stats = translationService.getCacheStats();

      message.setText(
          "📊 Translation Statistics\n\n"
              + "Provider: "
              + stats.get("provider")
              + "\n"
              + "Cache Size: "
              + stats.get("cacheSize")
              + " translations\n"
              + "Caching: "
              + (Boolean.TRUE.equals(stats.get("cachingEnabled")) ? "Enabled ✅" : "Disabled ❌")
              + "\n\n"
              + "🚀 Cache Benefits:\n"
              + "• Instant responses for repeated translations\n"
              + "• Reduces API calls and rate limits\n"
              + "• Improves overall performance");

      replyKeyboardService.getDefaultKeyboard(message);
    } catch (Exception e) {
      log.error("Stats retrieval failed: {}", e.getMessage());
      message.setText("❌ Could not retrieve statistics: " + e.getMessage());
    }
  }

  public boolean havePendingCommand(SendMessage message, Long chatId, String text) {

    log.trace("Checking for pending command for chat: {}", chatId);
    CallbackQueryTemp callbackQueryTemp = callbackQueryRepository.findById(chatId).orElse(null);

    if (callbackQueryTemp != null && callbackQueryTemp.getCallbackQuery() == CallbackQuery.ADD_OWN_TRANSLATION) {

      callbackQueryRepository.delete(callbackQueryTemp);
      return handleAddWithOwnTranslation(message, chatId, text);
    }

    return false;
  }

  private boolean handleAddWithOwnTranslation(SendMessage message, Long chatId, String text) {

    WordTemp wordTemp = wordTempRepository.findById(chatId).orElse(null);

    if (wordTemp != null) {

      wordTempRepository.delete(wordTemp);

      if (!wordRepository.existsByEnglishOrUkrainian(wordTemp.getEnglish(), text)) {

        Word word = Word.builder().chatId(chatId).english(wordTemp.getEnglish()).ukrainian(text).score(0).build();
        wordRepository.save(word);

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

    try {
      String autoDetectedLang = translationService.detectLanguage(text);
      detected = LanguageCode.fromString(autoDetectedLang);

      if (detected.equals(LanguageCode.UNKNOWN)) {

        log.error("Unsupported language detected: {}", autoDetectedLang);
        message.setText(
            """
            ❌ Unsupported language detected: %s %s

            Currently supports only English 🇬🇧 and Ukrainian 🇺🇦.
            """
                .formatted(autoDetectedLang, LanguageUtil.getLanguageFlag(autoDetectedLang)));
        return;
      }

      eng = translateIfNeeded(text, detected, LanguageCode.EN);
      ukr = translateIfNeeded(text, detected, LanguageCode.UK);

      WordTemp wordTemp = WordTemp.builder().chatId(chatId).english(eng).ukrainian(ukr).build();
      wordTempRepository.save(wordTemp);

    } catch (TranslationException e) {
      log.error("Language detection failed: {}", e.getMessage());
      message.setText("❌ Language detection failed\n\n" + "Error: " + e.getMessage());
      return;
    }

    log.trace("Translation successful");

    InlineKeyboardButton addToVocabulary = new InlineKeyboardButton("Add to vocabulary");
    addToVocabulary.setCallbackData(CallbackQuery.ADD_TO_VOCABULARY.toString());

    InlineKeyboardButton addWithOwnTranslation = new InlineKeyboardButton("Add with own translation");
    addWithOwnTranslation.setCallbackData(CallbackQuery.ADD_OWN_TRANSLATION.toString());

    List<InlineKeyboardButton> firstRow = List.of(addToVocabulary);
    List<List<InlineKeyboardButton>> rows = new ArrayList<>(List.of(firstRow));

    if (detected == LanguageCode.EN) {
      List<InlineKeyboardButton> secondRow = List.of(addWithOwnTranslation);
      rows.add(secondRow);
    }

    InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
    markup.setKeyboard(rows);

    message.setText(LanguageUtil.formatTranslationPair(eng, ukr));
    message.setReplyMarkup(markup);
  }

  private String translateIfNeeded(String text, LanguageCode detected, LanguageCode target) throws TranslationException {
    return detected == target ? text : translationService.translate(text, detected.getCode(), target.getCode());
  }
}
