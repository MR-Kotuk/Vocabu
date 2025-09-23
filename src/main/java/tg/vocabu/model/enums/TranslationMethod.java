package tg.vocabu.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TranslationMethod {
  GOOGLE_TRANSLATOR("Google Translator"),
  DICTIONARY("Dictionary");

  private final String name;
}
