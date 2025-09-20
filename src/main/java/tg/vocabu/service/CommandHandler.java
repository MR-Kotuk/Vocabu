package tg.vocabu.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import tg.vocabu.config.FilePathConfig;
import tg.vocabu.exception.TranslationException;
import tg.vocabu.model.entity.CallbackQueryTemp;
import tg.vocabu.model.entity.Vocabulary;
import tg.vocabu.model.entity.Word;
import tg.vocabu.model.entity.WordTemp;
import tg.vocabu.model.enums.CallbackQuery;
import tg.vocabu.model.enums.LanguageCode;
import tg.vocabu.repository.CallbackQueryRepository;
import tg.vocabu.repository.VocabularyRepository;
import tg.vocabu.repository.WordTempRepository;
import tg.vocabu.util.FileReader;
import tg.vocabu.util.LanguageUtil;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommandHandler {

  private final GoogleTranslationService translationService;

  private final WordTempRepository wordTempRepository;
  private final VocabularyRepository vocabularyRepository;
  private final CallbackQueryRepository callbackQueryRepository;

  public void handleUsersStatus(SendMessage message) {

    log.debug("Received users status command from admin");

    long usersCount = vocabularyRepository.count();
    message.setText("Users count: " + usersCount);
  }

  public void handleVocabulary(SendMessage message, Long chatId) {

    log.debug("Received vocabulary command from chat: {}", chatId);

    Vocabulary vocabulary = vocabularyRepository.findById(chatId).orElse(null);
    String vocabularyHeader = "==🇬🇧== Vocabulary ==🇺🇦== - % -\n\n";

    if (vocabulary != null && !vocabulary.getWords().isEmpty()) {

      StringBuilder table = new StringBuilder();
      table.append(vocabularyHeader);

      for (Word word : vocabulary.getWords()) {
        table.append(word.getEnglish()).append("  ➜  ").append(word.getUkrainian()).append("  -  ").append(word.getScore()).append("%\n");
      }

      message.setText(table.toString());
    } else {
      message.setText(vocabularyHeader + "\nNothing yet...");
    }
  }

  public void handleStartCommand(boolean isAdmin, SendMessage message, Long chatId) {

    log.debug("Received start command from chat: {}", chatId);

    if (!vocabularyRepository.existsById(chatId)) {
      Vocabulary vocabulary = Vocabulary.builder().chatId(chatId).words(new HashSet<>()).build();
      vocabularyRepository.save(vocabulary);
    }

    String filePath = isAdmin ? FilePathConfig.ADMIN_START_MESSAGE_FILE : FilePathConfig.START_MESSAGE_FILE;
    message.setText(FileReader.readFile(filePath));
  }

  public void handleHelpCommand(boolean isAdmin, SendMessage message, Long chatId) {

    log.debug("Received help command from chat: {}", chatId);

    String filePath = isAdmin ? FilePathConfig.ADMIN_HELP_MESSAGE_FILE : FilePathConfig.HELP_MESSAGE_FILE;
    message.setText(FileReader.readFile(filePath));
  }

  public void handleStatusCheckCommand(SendMessage message, Long chatId) {

    log.debug("Received status check command from chat: {}", chatId);

    try {
      String status = translationService.getStatus();
      message.setText("🔧 Service Status\n\n" + status + "\n\n💡 If offline, try again in a few minutes. Google may temporarily limit requests.");
    } catch (Exception e) {
      log.error("Status check failed: {}", e.getMessage());
      message.setText("⚠️ Status Check Failed\n\n" + "Could not check Google Translate status.\n" + "Error: " + e.getMessage());
    }
  }

  public void handleClearCacheCommand(SendMessage message, Long chatId) {

    log.debug("Received clear cache command from chat: {}", chatId);

    try {
      translationService.clearCache();
      message.setText(
          "🗑️ Cache Cleared\n\n"
              + "Translation cache has been cleared successfully.\n"
              + "Next translations will be fetched fresh from Google Translate.");
    } catch (Exception e) {
      log.error("Cache clearing failed: {}", e.getMessage());
      message.setText("❌ Could not clear cache: " + e.getMessage());
    }
  }

  public void handleStatsCheckCommand(SendMessage message, Long chatId) {

    log.debug("Received stats check command from chat: {}", chatId);

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
    } catch (Exception e) {
      log.error("Stats retrieval failed: {}", e.getMessage());
      message.setText("❌ Could not retrieve statistics: " + e.getMessage());
    }
  }

  public boolean havePendingCommand(SendMessage message, Long chatId, String text) {

    log.debug("Checking for pending command for chat: {}", chatId);
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

      Vocabulary vocabulary = vocabularyRepository.findById(chatId).orElse(Vocabulary.builder().chatId(chatId).words(new HashSet<>()).build());
      Word word = Word.builder().english(wordTemp.getEnglish()).ukrainian(text).score(0).build();

      vocabulary.getWords().add(word);
      vocabularyRepository.save(vocabulary);

      message.setText(
          "Added word with own translation to vocabulary:\n\n" + LanguageUtil.formatTranslationPair(word.getEnglish(), word.getUkrainian()));

      return true;
    }

    return false;
  }

  public void handleTranslateCommand(SendMessage message, Long chatId, String text) {

    log.debug("Received translate command from chat: {}", chatId);

    String[] words = text.trim().split("\\s+");

    if (words.length > 3) {
      message.setText("❌ Text too long! Supports only max 3 words.");
      return;
    }

    log.debug("Translating text: '{}'", text);

    String eng, ukr;
    LanguageCode from = LanguageCode.EN;

    try {
      String autoDetectedLang = translationService.detectLanguage(text);
      LanguageCode detected = LanguageCode.valueOf(autoDetectedLang.toUpperCase());

      switch (detected) {
        case EN -> {
          eng = text;
          ukr = translationService.translate(text, LanguageCode.EN.getCode(), LanguageCode.UK.getCode());
        }
        case UK, UNKNOWN -> {
          from = LanguageCode.UK;

          eng = translationService.translate(text, LanguageCode.UK.getCode(), LanguageCode.EN.getCode());
          ukr = text;
        }
        default -> {
          log.error("Unsupported language detected: {}", autoDetectedLang);
          message.setText(
              """
              ❌ Unsupported language detected: %s %s

              Currently supports only English 🇬🇧 and Ukrainian 🇺🇦.
              """
                  .formatted(autoDetectedLang, LanguageUtil.getLanguageFlag(autoDetectedLang)));
          return;
        }
      }

      wordTempRepository.deleteById(chatId);

      WordTemp wordTemp = WordTemp.builder().chatId(chatId).english(eng).ukrainian(ukr).build();
      wordTempRepository.save(wordTemp);

    } catch (TranslationException e) {
      log.error("Language detection failed: {}", e.getMessage());
      message.setText("❌ Language detection failed\n\n" + "Error: " + e.getMessage());
      return;
    }

    log.debug("Translation successful");

    InlineKeyboardButton addToVocabulary = new InlineKeyboardButton("Add to vocabulary");
    addToVocabulary.setCallbackData(CallbackQuery.ADD_TO_VOCABULARY.toString());

    InlineKeyboardButton addWithOwnTranslation = new InlineKeyboardButton("Add with own translation");
    addWithOwnTranslation.setCallbackData(CallbackQuery.ADD_OWN_TRANSLATION.toString());

    List<InlineKeyboardButton> firstRow = List.of(addToVocabulary);
    List<List<InlineKeyboardButton>> rows = new ArrayList<>(List.of(firstRow));

    if (from == LanguageCode.EN) {
      List<InlineKeyboardButton> secondRow = List.of(addWithOwnTranslation);
      rows.add(secondRow);
    }

    InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
    markup.setKeyboard(rows);

    message.setText(LanguageUtil.formatTranslationPair(eng, ukr));
    message.setReplyMarkup(markup);
  }
}
