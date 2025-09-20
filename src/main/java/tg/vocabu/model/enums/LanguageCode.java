package tg.vocabu.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LanguageCode {
  EN("en"),
  UK("uk"),
  UNKNOWN("unknown");

  private final String code;
}
