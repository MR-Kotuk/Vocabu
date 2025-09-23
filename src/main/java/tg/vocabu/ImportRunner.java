package tg.vocabu;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import tg.vocabu.util.DictionaryImporter;

@Component
@RequiredArgsConstructor
public class ImportRunner implements CommandLineRunner {

  private final DictionaryImporter dictionaryImporter;

  @Override
  public void run(String... args) throws Exception {
    dictionaryImporter.importDictionary();
  }
}
