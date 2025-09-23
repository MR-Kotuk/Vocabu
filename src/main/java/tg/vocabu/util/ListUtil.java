package tg.vocabu.util;

import java.util.List;
import java.util.Random;
import org.springframework.stereotype.Component;

@Component
public class ListUtil {

  public static <T> T popRandomElementFromList(List<T> words) {

    int index = new Random().nextInt(words.size());

    return words.remove(index);
  }
}
