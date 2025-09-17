package tg.vocabu.util;

import org.springframework.stereotype.Component;

@Component
public class LanguageUtil {

  public static String getLanguageFlag(String langCode) {
    return switch (langCode.toLowerCase()) {
      case "uk" -> "🇺🇦";
      case "en" -> "🇬🇧";
      case "ru" -> "\uD83D\uDC37";
      case "de" -> "🇩🇪";
      case "fr" -> "🇫🇷";
      case "es" -> "🇪🇸";
      case "it" -> "🇮🇹";
      case "pl" -> "🇵🇱";
      case "pt" -> "🇵🇹";
      case "ja" -> "🇯🇵";
      case "ko" -> "🇰🇷";
      case "zh" -> "🇨🇳";
      case "ar" -> "🇸🇦";
      case "hi" -> "🇮🇳";
      case "tr" -> "🇹🇷";
      case "nl" -> "🇳🇱";
      case "sv" -> "🇸🇪";
      case "da" -> "🇩🇰";
      case "no" -> "🇳🇴";
      case "fi" -> "🇫🇮";
      case "cs" -> "🇨🇿";
      case "sk" -> "🇸🇰";
      case "hu" -> "🇭🇺";
      case "ro" -> "🇷🇴";
      case "bg" -> "🇧🇬";
      case "hr" -> "🇭🇷";
      case "sl" -> "🇸🇮";
      case "et" -> "🇪🇪";
      case "lv" -> "🇱🇻";
      case "lt" -> "🇱🇹";
      case "mt" -> "🇲🇹";
      case "ga" -> "🇮🇪";
      case "cy" -> "🏴󠁧󠁢󠁷󠁬󠁳󠁿";
      default -> "🌐";
    };
  }
}
