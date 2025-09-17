package tg.vocabu.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LanguageCode {
  EN("en"),
  UK("uk");

  private final String code;
}
