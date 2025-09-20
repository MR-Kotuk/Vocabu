package tg.vocabu.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import tg.vocabu.model.entity.CallbackQueryTemp;
import tg.vocabu.model.entity.Word;
import tg.vocabu.model.entity.WordTemp;
import tg.vocabu.model.enums.CallbackQuery;
import tg.vocabu.repository.CallbackQueryRepository;
import tg.vocabu.repository.WordRepository;
import tg.vocabu.repository.WordTempRepository;
import tg.vocabu.util.LanguageUtil;

@Slf4j
@Service
@RequiredArgsConstructor
public class CallbackQueryHandler {

  private final ReplyKeyboardService replyKeyboardService;
  private final CommandHandler commandHandler;

  private final WordTempRepository wordTempRepository;
  private final CallbackQueryRepository callbackQueryRepository;
  private final WordRepository wordRepository;

  public void handleAddToVocabulary(SendMessage message, long chatId) {

    log.trace("Handling Add to Vocabulary callback query");

    WordTemp wordTemp = wordTempRepository.findById(chatId).orElse(null);

    if (wordTemp != null) {

      wordTempRepository.delete(wordTemp);

      if (!wordRepository.existsByEnglishOrUkrainian(wordTemp.getEnglish(), wordTemp.getUkrainian())) {

        Word word = Word.builder().chatId(chatId).english(wordTemp.getEnglish()).ukrainian(wordTemp.getUkrainian()).score(0).build();
        wordRepository.save(word);

        message.setText(
            "Added word with own translation to vocabulary:\n\n" + LanguageUtil.formatTranslationPair(word.getEnglish(), word.getUkrainian()));

        replyKeyboardService.getDefaultKeyboard(message);
      } else {
        message.setText("This word pair already exists in your vocabulary.");
      }
    } else {
      commandHandler.handleSomethingWentWrong(message);
    }
  }

  public void handleAddOwnTranslation(SendMessage message, long chatId) {

    log.trace("Handling Add Own Translation callback query");

    callbackQueryRepository.deleteById(chatId);
    WordTemp wordTemp = wordTempRepository.findById(chatId).orElse(null);

    if (wordTemp == null) {
      commandHandler.handleSomethingWentWrong(message);
      return;
    }

    if (wordRepository.existsByEnglish(wordTemp.getEnglish())) {
      message.setText("This word pair already exists in your vocabulary.");
      return;
    }

    callbackQueryRepository.save(new CallbackQueryTemp(chatId, CallbackQuery.ADD_OWN_TRANSLATION));

    message.setText("Send your own translation:");
  }
}
