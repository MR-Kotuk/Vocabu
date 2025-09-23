package tg.vocabu.service.command.handler;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import tg.vocabu.service.GoogleTranslationService;
import tg.vocabu.service.ReplyKeyboardService;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleTranslatorHandler {

  private final GoogleTranslationService googleTranslationService;
  private final ReplyKeyboardService replyKeyboardService;

  public void handleStatusCheckCommand(SendMessage message, Long chatId) {

    log.trace("Received status check command from chat: {}", chatId);

    try {
      String status = googleTranslationService.getStatus();
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
      googleTranslationService.clearCache();
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
      Map<String, Object> stats = googleTranslationService.getCacheStats();

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
}
