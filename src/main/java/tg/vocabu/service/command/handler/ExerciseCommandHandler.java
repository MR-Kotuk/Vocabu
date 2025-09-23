package tg.vocabu.service.command.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import tg.vocabu.model.entity.user.Word;
import tg.vocabu.repository.WordRepository;
import tg.vocabu.service.TranslationService;
import tg.vocabu.util.ListUtil;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExerciseCommandHandler {

  private final WordRepository wordRepository;
  private final TranslationService translationService;

  public void handleExercise(SendMessage message, Long chatId) {

    log.trace("Received exercise command from chat: {}", chatId);
    handleWordTranslationExercise(message, chatId);
  }

  private void handleWordTranslationExercise(SendMessage message, Long chatId) {

    log.trace("Handling Word Translation Exercise for chat: {}", chatId);

    List<Word> words = wordRepository.findByChatIdAndLearned(chatId, false);

    if (words == null || words.isEmpty()) {
      message.setText("Your vocabulary is empty. Please add some words first.");
      return;
    }

    Word wordForExercise = ListUtil.popRandomElementFromList(words);

    log.trace("Selected word for exercise: {}", wordForExercise);

    String[] wordsSelection = new String[4];
    String[] wordSelectionStatus = new String[4];
    int correctIndex = new Random().nextInt(3);

    for (int i = 0; i < wordsSelection.length; i++) {

      if (i == correctIndex) {
        wordsSelection[i] = wordForExercise.getUkrainian();
        wordSelectionStatus[i] = "CORRECT";
        continue;
      }

      if (words.size() > i) {
        wordsSelection[i] = ListUtil.popRandomElementFromList(words).getUkrainian();
      } else {
        wordsSelection[i] = translationService.getRandomWord().getUkrainian();
      }

      wordSelectionStatus[i] = "WRONG";
    }

    List<InlineKeyboardButton> firstRow =
        List.of(
            InlineKeyboardButton.builder().text(wordsSelection[0]).callbackData(wordSelectionStatus[0]).build(),
            InlineKeyboardButton.builder().text(wordsSelection[1]).callbackData(wordSelectionStatus[1]).build());

    List<InlineKeyboardButton> secondRow =
        List.of(
            InlineKeyboardButton.builder().text(wordsSelection[2]).callbackData(wordSelectionStatus[2]).build(),
            InlineKeyboardButton.builder().text(wordsSelection[3]).callbackData(wordSelectionStatus[3]).build());

    List<InlineKeyboardButton> thirdRow = List.of(InlineKeyboardButton.builder().text("Skip ->").callbackData("SKIP").build());

    List<List<InlineKeyboardButton>> rows = new ArrayList<>(List.of(firstRow, secondRow, thirdRow));

    InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
    markup.setKeyboard(rows);

    message.setText("Choose the correct translation for: \n\n                  🇬🇧 " + wordForExercise.getEnglish());
    message.setReplyMarkup(markup);
  }
}
