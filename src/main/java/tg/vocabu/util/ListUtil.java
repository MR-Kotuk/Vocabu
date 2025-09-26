package tg.vocabu.util;

import java.util.List;
import java.util.Random;
import org.springframework.stereotype.Component;

@Component
public class ListUtil {

  private static final Random random = new Random();

  public static <T> T popRandomElementFromList(List<T> words) {
    return words.remove(random.nextInt(words.size()));
  }
}
