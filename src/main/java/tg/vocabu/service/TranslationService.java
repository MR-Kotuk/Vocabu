package tg.vocabu.service;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tg.vocabu.exception.TranslationException;
import tg.vocabu.model.entity.dictionary.DictionaryWord;
import tg.vocabu.model.enums.LanguageCode;
import tg.vocabu.repository.DictionaryRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class TranslationService {

  private final DictionaryRepository dictionaryRepository;

  public String translate(String word) throws TranslationException {

    if (word == null || word.isBlank()) {
      throw new TranslationException("Input word is null or blank");
    }

    Optional<String> translation = dictionaryRepository.findTranslation(word);

    if (translation.isEmpty()) {
      throw new TranslationException(
          "Translation not found for word: " + word + "\n In our dictionary of " + dictionaryRepository.count() + " words.");
    }

    log.debug("Found translation for word '{}': {}", word, translation.get());
    return translation.get();
  }

  public LanguageCode detectLanguage(String word) {

    if (word == null || word.isBlank()) {
      return LanguageCode.UNKNOWN;
    }

    if (word.chars().anyMatch(Character::isAlphabetic)) {

      if (word.matches(".*[a-zA-Z].*")) {
        return LanguageCode.EN;
      } else if (word.matches(".*[а-яА-ЯєЄіІїЇґҐ].*")) {
        return LanguageCode.UK;
      }
    }

    return LanguageCode.UNKNOWN;
  }

  public DictionaryWord getRandomWord() {
    return dictionaryRepository.findRandomWord().orElse(null);
  }

  public long getWordsCount() {
    return dictionaryRepository.count();
  }
}
