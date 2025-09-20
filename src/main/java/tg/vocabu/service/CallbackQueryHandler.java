package tg.vocabu.service;

import java.util.HashSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import tg.vocabu.bot.TelegramBot;
import tg.vocabu.model.entity.CallbackQueryTemp;
import tg.vocabu.model.entity.Vocabulary;
import tg.vocabu.model.entity.Word;
import tg.vocabu.model.entity.WordTemp;
import tg.vocabu.model.enums.CallbackQuery;
import tg.vocabu.repository.CallbackQueryRepository;
import tg.vocabu.repository.VocabularyRepository;
import tg.vocabu.repository.WordTempRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class CallbackQueryHandler {

  private final WordTempRepository wordTempRepository;
  private final VocabularyRepository vocabularyRepository;
  private final CallbackQueryRepository callbackQueryRepository;

  public void handleAddToVocabulary(TelegramBot bot, long chatId) {

    log.debug("Handling Add to Vocabulary callback query");

    SendMessage message = new SendMessage();
    message.setChatId(chatId);

    WordTemp wordTemp = wordTempRepository.findById(chatId).orElse(null);

    if (wordTemp != null) {

      wordTempRepository.delete(wordTemp);

      Vocabulary vocabulary = vocabularyRepository.findById(chatId).orElse(Vocabulary.builder().chatId(chatId).words(new HashSet<>()).build());
      Word word = Word.builder().english(wordTemp.getEnglish()).ukrainian(wordTemp.getUkrainian()).score(0).build();

      vocabulary.getWords().add(word);
      vocabularyRepository.save(vocabulary);

      message.setText("Added word to vocabulary:\n\n" + "🇬🇧 " + word.getEnglish() + " - 🇺🇦 " + word.getUkrainian());
    } else {
      return;
    }

    try {
      bot.execute(message);
    } catch (TelegramApiException e) {
      e.printStackTrace();
    }
  }

  public void handleAddOwnTranslation(TelegramBot bot, long chatId) {

    if (!wordTempRepository.existsById(chatId)) {
      return;
    }

    callbackQueryRepository.deleteById(chatId);
    callbackQueryRepository.save(new CallbackQueryTemp(chatId, CallbackQuery.ADD_OWN_TRANSLATION));

    log.debug("Handling Add Own Translation callback query");

    SendMessage message = new SendMessage(String.valueOf(chatId), "Send your own translation:");

    try {
      bot.execute(message);
    } catch (TelegramApiException e) {
      e.printStackTrace();
    }
  }
}
