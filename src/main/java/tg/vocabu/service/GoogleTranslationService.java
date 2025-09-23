package tg.vocabu.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tg.vocabu.exception.TranslationException;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleTranslationService {

  private static final String TRANSLATE_URL = "https://translate.googleapis.com/translate_a/single";
  private static final String USER_AGENT =
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36";

  private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final Map<String, String> translationCache = new ConcurrentHashMap<>();

  public String translate(String text, String sourceLang, String targetLang) throws TranslationException {

    if (text == null || text.trim().isEmpty()) {
      throw new TranslationException("Text cannot be null or empty");
    }

    if (text.length() > 5000) {
      throw new TranslationException("Text too long. Supports up to 5000 characters.");
    }

    String cacheKey = generateCacheKey(text, sourceLang, targetLang);
    String cachedResult = translationCache.get(cacheKey);

    if (cachedResult != null) {
      log.trace("Returning cached translation for: {}", text.substring(0, Math.min(50, text.length())));
      return cachedResult;
    }

    try {
      String translatedText = performTranslation(text, sourceLang, targetLang);

      translationCache.put(cacheKey, translatedText);

      log.debug(
          "Translation successful: '{}' -> '{}'",
          text.substring(0, Math.min(50, text.length())),
          translatedText.substring(0, Math.min(50, translatedText.length())));

      return translatedText;

    } catch (Exception e) {
      throw new TranslationException("Translation failed: " + e.getMessage(), e);
    }
  }

  public boolean isAvailable() {

    try {
      String testResult = translate("Hello", "en", "es");
      return testResult != null && !testResult.trim().isEmpty();

    } catch (Exception e) {
      log.warn("Google Translate availability check failed: {}", e.getMessage());
      return false;
    }
  }

  public String getStatus() {

    try {
      long startTime = System.currentTimeMillis();
      boolean available = isAvailable();
      long responseTime = System.currentTimeMillis() - startTime;

      return String.format("✅ Google Translate: ONLINE\n" + "Response time: %dms\n" + "Is Available: %s", responseTime, available);

    } catch (Exception e) {
      return "❌ Google Translate: OFFLINE\nError: " + e.getMessage();
    }
  }

  public void clearCache() {

    translationCache.clear();

    log.info("Google Translate cache cleared");
  }

  public Map<String, Object> getCacheStats() {
    return Map.of("cacheSize", translationCache.size(), "cachingEnabled", true, "provider", "Google Translate");
  }

  private String performTranslation(String text, String sourceLang, String targetLang) throws Exception {

    String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);
    String url =
        String.format("%s?client=gtx&sl=%s&tl=%s&dt=t&q=%s", TRANSLATE_URL, sourceLang.equals("auto") ? "auto" : sourceLang, targetLang, encodedText);

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(15))
            .header("User-Agent", USER_AGENT)
            .header("Referer", "https://translate.google.com/")
            .GET()
            .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    if (response.statusCode() == 200) {
      return parseTranslationResponse(response.body());
    } else if (response.statusCode() == 429) {
      throw new Exception("Rate limit exceeded. Please try again later.");
    } else {
      throw new Exception("HTTP " + response.statusCode() + ": " + response.body());
    }
  }

  private String parseTranslationResponse(String responseBody) throws TranslationException {

    try {
      if (!responseBody.startsWith("[[")) {
        throw new Exception("Invalid response format");
      }

      Pattern pattern = Pattern.compile("\\[\\[\\[\"([^\"]+)\"");
      Matcher matcher = pattern.matcher(responseBody);

      if (matcher.find()) {

        String translatedText = matcher.group(1);
        translatedText =
            translatedText.replace("\\u0026", "&").replace("\\u003c", "<").replace("\\u003e", ">").replace("\\\"", "\"").replace("\\/", "/");

        return translatedText;
      }

      try {

        List<List<Object>> outerArray = objectMapper.readValue(responseBody, List.class);

        if (!outerArray.isEmpty() && outerArray.get(0) != null) {

          List<Object> innerArray = outerArray.get(0);

          StringBuilder result = new StringBuilder();
          for (Object item : innerArray) {

            if (item instanceof List) {

              List<Object> translationPart = (List<Object>) item;

              if (!translationPart.isEmpty() && translationPart.get(0) instanceof String) {
                result.append(translationPart.get(0));
              }
            }
          }

          if (!result.isEmpty()) {
            return result.toString();
          }
        }
      } catch (Exception jsonException) {
        log.warn("JSON parsing fallback failed: {}", jsonException.getMessage());
      }

      throw new TranslationException("Could not extract translation from response");

    } catch (Exception e) {
      log.error("Failed to parse translation response: {}", responseBody);
      throw new TranslationException("Translation parsing failed: " + e.getMessage());
    }
  }

  private String generateCacheKey(String text, String sourceLang, String targetLang) {
    return String.format("%s|%s|%s", text.hashCode(), sourceLang, targetLang);
  }
}
