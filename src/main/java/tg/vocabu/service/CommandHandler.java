package tg.vocabu.service;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import tg.vocabu.config.FilePathConfig;
import tg.vocabu.exception.TranslationException;
import tg.vocabu.model.LanguageCode;
import tg.vocabu.util.FileReader;
import tg.vocabu.util.LanguageUtil;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommandHandler {

  private final GoogleTranslationService translationService;

  public void handleStartCommand(boolean isAdmin, SendMessage message, Long chatId) {

    log.debug("Received start command from chat: {}", chatId);

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

  public void handleTranslateCommand(SendMessage message, Long chatId, String text) {

    log.debug("Received translate command from chat: {}", chatId);

    if (text.length() > 5000) {
      message.setText("❌ Text too long! Supports up to 5000 characters per message.");
      return;
    }

    log.debug("Translating text: '{}'", text.substring(0, Math.min(50, text.length())));

    String translatedText;
    String autoDetectedLang;
    LanguageCode to;

    try {
      autoDetectedLang = translationService.detectLanguage(text);

      if (LanguageCode.UK.getCode().equals(autoDetectedLang)) {
        to = LanguageCode.EN;
        translatedText = translateText(text, LanguageCode.UK, LanguageCode.EN);
      } else if (LanguageCode.EN.getCode().equals(autoDetectedLang)) {
        to = LanguageCode.UK;
        translatedText = translateText(text, LanguageCode.EN, LanguageCode.UK);
      } else {
        message.setText(
            "❌ Unsupported language detected: "
                + autoDetectedLang
                + " "
                + LanguageUtil.getLanguageFlag(autoDetectedLang)
                + "\n\nCurrently, only Ukrainian 🇺🇦 and English 🇬🇧 are supported for translation.");
        return;
      }
    } catch (TranslationException e) {
      log.error("Language detection failed: {}", e.getMessage());
      message.setText("❌ Language detection failed\n\n" + "Error: " + e.getMessage());
      return;
    }

    message.setText(LanguageUtil.getLanguageFlag(autoDetectedLang) + " -> " + LanguageUtil.getLanguageFlag(to.getCode()) + "\n\n" + translatedText);
    log.debug("Translation successful");
  }

  private String translateText(String text, LanguageCode from, LanguageCode to) {
    try {
      return translationService.translate(text, from.getCode(), to.getCode());
    } catch (Exception e) {
      return null;
    }
  }
}
