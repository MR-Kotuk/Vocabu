package tg.vocabu.service.command.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import tg.vocabu.model.entity.temp.Exercise;
import tg.vocabu.model.entity.user.Word;
import tg.vocabu.model.enums.CallbackQuery;
import tg.vocabu.repository.ExerciseRepository;
import tg.vocabu.repository.WordRepository;
import tg.vocabu.service.TranslationService;
import tg.vocabu.util.ListUtil;
import tg.vocabu.util.StringUtil;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExerciseCommandHandler {

  private final TranslationService translationService;

  private final WordRepository wordRepository;
  private final ExerciseRepository exerciseRepository;

  private final Random random = new Random();

  public void handleExercise(SendMessage message, Long chatId) {

    log.trace("Received exercise command from chat: {}", chatId);

    Optional<Exercise> exercise = exerciseRepository.findById(chatId);

    if (exercise.isPresent()) {

      log.trace("Found existing exercise for chat: {}", chatId);

      displayExercise(message, exercise.get());
      return;
    }

    handleWordTranslationExercise(message, chatId, random.nextBoolean());
  }

  private void handleWordTranslationExercise(SendMessage message, Long chatId, boolean isEngToUkr) {

    log.trace("Handling Word Translation Exercise for chat: {}", chatId);

    List<Word> words = wordRepository.findByChatIdAndLearned(chatId, false);

    if (words == null || words.isEmpty()) {
      message.setText("Your vocabulary is empty. Please add some words first.");
      return;
    }

    Word word = ListUtil.popRandomElementFromList(words);

    log.trace("Selected word for exercise: {}", word);

    String[] options = new String[4];
    String[] statuses = new String[4];
    int correctOptionIndex = random.nextInt(3);

    for (int i = 0; i < options.length; i++) {

      boolean isCorrectOptionIndex = i == correctOptionIndex;

      Word optionWord = !words.isEmpty() ? ListUtil.popRandomElementFromList(words) : translationService.getRandomWord().toWord();
      optionWord = isCorrectOptionIndex ? word : optionWord;

      options[i] = isEngToUkr ? optionWord.getUkrainian() : optionWord.getEnglish();
      options[i] = StringUtil.normalizeText(options[i]);

      statuses[i] = isCorrectOptionIndex ? CallbackQuery.CORRECT_EXERCISE_ANSWER.toString() : CallbackQuery.WRONG_EXERCISE_ANSWER.toString();
    }

    Exercise exercise = new Exercise(chatId, word.getId(), isEngToUkr, word.getEnglish(), word.getUkrainian(), options, statuses);
    exerciseRepository.save(exercise);

    displayExercise(message, exercise);
  }

  private void displayExercise(SendMessage message, Exercise exercise) {

    List<List<InlineKeyboardButton>> rows = getExerciseKeyboard(exercise.getOptions(), exercise.getStatuses(), 2);

    InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
    markup.setKeyboard(rows);

    message.setText(
        "Choose the correct translation for:\n\n     "
            + (exercise.isEngToUkr() ? "🇬🇧 " + exercise.getEnglish() : "🇺🇦 " + exercise.getUkrainian()));
    message.setReplyMarkup(markup);
  }

  private List<List<InlineKeyboardButton>> getExerciseKeyboard(String[] options, String[] statuses, int buttonsInRow) {

    List<List<InlineKeyboardButton>> rows = new ArrayList<>();
    List<InlineKeyboardButton> currentRow = new ArrayList<>();

    for (int i = 0; i < options.length; i++) {

      InlineKeyboardButton button = InlineKeyboardButton.builder().text(options[i]).callbackData(statuses[i]).build();

      currentRow.add(button);

      if (currentRow.size() == buttonsInRow) {
        rows.add(new ArrayList<>(currentRow));
        currentRow.clear();
      }
    }

    List<InlineKeyboardButton> skip =
        List.of(InlineKeyboardButton.builder().text("Skip ->").callbackData(CallbackQuery.SKIP_EXERCISE.toString()).build());
    rows.add(skip);

    return rows;
  }
}
