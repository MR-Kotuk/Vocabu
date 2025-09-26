package tg.vocabu.util;

import org.springframework.stereotype.Component;

@Component
public class StringUtil {

  public static String normalizeText(String text) {
    return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
  }
}
