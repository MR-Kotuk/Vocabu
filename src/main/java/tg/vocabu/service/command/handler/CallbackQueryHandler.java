package tg.vocabu.service.command.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import tg.vocabu.bot.TelegramBot;
import tg.vocabu.model.entity.dictionary.DictionaryWord;
import tg.vocabu.model.entity.temp.CallbackQueryTemp;
import tg.vocabu.model.entity.temp.Exercise;
import tg.vocabu.model.entity.temp.WordTemp;
import tg.vocabu.model.entity.user.Word;
import tg.vocabu.model.enums.CallbackQuery;
import tg.vocabu.model.enums.TranslationMethod;
import tg.vocabu.repository.CallbackQueryRepository;
import tg.vocabu.repository.DictionaryRepository;
import tg.vocabu.repository.ExerciseRepository;
import tg.vocabu.repository.UserRepository;
import tg.vocabu.repository.WordRepository;
import tg.vocabu.repository.WordTempRepository;
import tg.vocabu.service.ReplyKeyboardService;
import tg.vocabu.util.LanguageUtil;

@Slf4j
@Service
@RequiredArgsConstructor
public class CallbackQueryHandler {

  private final ReplyKeyboardService replyKeyboardService;
  private final CommandHandler commandHandler;

  private final WordTempRepository wordTempRepository;
  private final CallbackQueryRepository callbackQueryRepository;
  private final ExerciseRepository exerciseRepository;
  private final UserRepository userRepository;
  private final WordRepository wordRepository;
  private final DictionaryRepository dictionaryRepository;

  public void handleSkipExercise(SendMessage message, long chatId) {

    log.trace("Handling Skip Exercise callback query");

    Exercise exercise = exerciseRepository.findById(chatId).orElse(null);

    if (exercise == null) {
      commandHandler.handleSomethingWentWrong(message);
      return;
    }

    Optional<Word> wordOptional = wordRepository.findById(exercise.getWordId());

    if (wordOptional.isPresent()) {

      Word word = wordOptional.get();
      word.decrementScore(5);

      wordRepository.save(word);
    } else {

      Word word = Word.builder().chatId(chatId).english(exercise.getEnglish()).ukrainian(exercise.getUkrainian()).learned(false).build();
      wordRepository.save(word);
    }

    message.setText(
        "⏭ Skipped\nThe correct answer is:\n\n" + LanguageUtil.formatTranslationPair(exercise.getEnglish(), exercise.getUkrainian()) + "     - 5%");

    exerciseRepository.delete(exercise);

    InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
    markup.setKeyboard(
        List.of(List.of(InlineKeyboardButton.builder().text("Continue ->").callbackData(CallbackQuery.START_EXERCISE.toString()).build())));

    message.setReplyMarkup(markup);

    replyKeyboardService.getDefaultKeyboard(message);
  }

  public void handleExerciseAnswer(SendMessage message, long chatId, String selectedOption) {

    log.trace("Handling Exercise Answer callback query");

    Exercise exercise = exerciseRepository.findById(chatId).orElse(null);

    if (exercise == null) {
      commandHandler.handleSomethingWentWrong(message);
      return;
    }

    Optional<Word> wordOptional = wordRepository.findById(exercise.getWordId());

    if (CallbackQuery.CORRECT_EXERCISE_ANSWER.toString().equals(selectedOption)) {

      boolean isLearned = false;

      if (wordOptional.isPresent()) {

        Word word = wordOptional.get();
        isLearned = word.incrementScore(10);

        wordRepository.save(word);
      } else {

        Word word = Word.builder().chatId(chatId).english(exercise.getEnglish()).ukrainian(exercise.getUkrainian()).score(5).learned(false).build();
        wordRepository.save(word);
      }

      message.setText(
          "✅ Correct!\n\n"
              + LanguageUtil.formatTranslationPair(exercise.getEnglish(), exercise.getUkrainian())
              + "     + 10%"
              + (isLearned ? "\n\nMarked as /learned! 🎉" : ""));
    } else if (CallbackQuery.WRONG_EXERCISE_ANSWER.toString().equals(selectedOption)) {

      if (wordOptional.isPresent()) {

        Word word = wordOptional.get();
        word.decrementScore(10);

        wordRepository.save(word);
      } else {

        Word word = Word.builder().chatId(chatId).english(exercise.getEnglish()).ukrainian(exercise.getUkrainian()).learned(false).build();
        wordRepository.save(word);
      }

      message.setText(
          "❌ Incorrect. The correct answer is:\n\n"
              + LanguageUtil.formatTranslationPair(exercise.getEnglish(), exercise.getUkrainian())
              + "     - 10%");
    } else {
      commandHandler.handleSomethingWentWrong(message);
      return;
    }

    exerciseRepository.delete(exercise);

    InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
    markup.setKeyboard(
        List.of(List.of(InlineKeyboardButton.builder().text("Continue ->").callbackData(CallbackQuery.START_EXERCISE.toString()).build())));

    message.setReplyMarkup(markup);

    replyKeyboardService.getDefaultKeyboard(message);
  }

  public void handleAddToVocabulary(TelegramBot bot, SendMessage message, long chatId, Long wordTempId) {

    log.trace("Handling Add to Vocabulary callback query");

    WordTemp wordTemp = wordTempRepository.findById(wordTempId).orElse(null);

    if (wordTemp != null) {

      wordTempRepository.delete(wordTemp);

      if (!wordRepository.existsByEnglishOrUkrainian(wordTemp.getEnglish(), wordTemp.getUkrainian())) {

        Word word = Word.builder().chatId(chatId).english(wordTemp.getEnglish()).ukrainian(wordTemp.getUkrainian()).score(0).build();
        wordRepository.save(word);

        if (wordTemp.getTranslationMethod() == TranslationMethod.GOOGLE_TRANSLATOR) {
          commandHandler.handleWordSuggestion(bot, word, chatId);
        }

        message.setText("Added new word to vocabulary:\n\n" + LanguageUtil.formatTranslationPair(word.getEnglish(), word.getUkrainian()));

        replyKeyboardService.getDefaultKeyboard(message);
      } else {
        message.setText("This word pair already exists in your vocabulary.");
      }
    }
  }

  public void handleAddOwnTranslation(SendMessage message, long chatId, Long wordTempId) {

    log.trace("Handling Add Own Translation callback query");

    callbackQueryRepository.deleteById(chatId);
    WordTemp wordTemp = wordTempRepository.findById(wordTempId).orElse(null);

    if (wordTemp == null) {
      commandHandler.handleSomethingWentWrong(message);
      return;
    }

    if (wordRepository.existsByEnglish(wordTemp.getEnglish())) {
      message.setText("This word pair already exists in your vocabulary.");
      return;
    }

    callbackQueryRepository.save(new CallbackQueryTemp(chatId, CallbackQuery.ADD_OWN_TRANSLATION + ":" + wordTempId));

    message.setText("Send your own translation:");
  }

  public void handleAddSuggestedWordToDictionary(SendMessage message, Long wordId) {

    log.trace("Handling Add Suggested Word to Dictionary callback query");

    Word word = wordRepository.findById(wordId).orElse(null);

    if (word == null) {
      commandHandler.handleSomethingWentWrong(message);
      return;
    }

    DictionaryWord dictionaryWord = DictionaryWord.builder().english(word.getEnglish()).ukrainian(word.getUkrainian()).build();

    dictionaryRepository.save(dictionaryWord);
    log.info("Added new word to dictionary: {}", dictionaryWord.getEnglish());

    message.setText("The word has been added to the dictionary:\n\n" + LanguageUtil.formatTranslationPair(word.getEnglish(), word.getUkrainian()));

    replyKeyboardService.getDefaultKeyboard(message);
  }

  public void handleBanUser(SendMessage message, Long userId) {

    log.trace("Handling Ban User callback query for user ID: {}", userId);

    if (!userRepository.existsById(userId)) {
      message.setText("User with ID: " + userId + ", does not exist.");
      log.warn("Attempted to ban non-existent user with ID: {}", userId);
    }

    userRepository.banUserById(userId);
    log.info("Banned user with ID: {}", userId);

    InlineKeyboardButton unbanUser = new InlineKeyboardButton("Unban");
    unbanUser.setCallbackData(CallbackQuery.UNBAN_USER + ":" + userId);

    List<InlineKeyboardButton> row = List.of(unbanUser);

    List<List<InlineKeyboardButton>> rows = new ArrayList<>(List.of(row));

    InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
    markup.setKeyboard(rows);

    message.setReplyMarkup(markup);
    message.setText("User:\n@" + userRepository.findUserNameById(userId) + "\nID: " + userId + "\n\nhas been banned");
  }

  public void handleUnbanUser(SendMessage message, Long userId) {

    log.trace("Handling Unban User callback query for user ID: {}", userId);

    if (!userRepository.existsById(userId)) {
      message.setText("User with ID: " + userId + ", does not exist.");
      log.warn("Attempted to unban non-existent user with ID: {}", userId);
    }

    userRepository.unbanUserById(userId);
    log.info("Unbanned user with ID: {}", userId);

    message.setText("User:\n@" + userRepository.findUserNameById(userId) + "\nID: " + userId + "\n\nhas been unbanned");
  }
}
