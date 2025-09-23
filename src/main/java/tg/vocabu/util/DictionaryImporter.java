package tg.vocabu.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tg.vocabu.config.DictionaryConfig;
import tg.vocabu.model.entity.dictionary.DictionaryWord;
import tg.vocabu.repository.DictionaryRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class DictionaryImporter {

  private final DictionaryConfig dictionaryConfig;

  private final DictionaryRepository dictionaryRepository;

  public void importDictionary() throws Exception {

    if (!dictionaryConfig.isImportOnStartup()) {
      log.info("Dictionary import on startup is disabled. Skipping import.");
      return;
    }

    if (dictionaryRepository.count() > 0 && !dictionaryConfig.isResetBeforeImport()) {
      log.info("Dictionary already contains data. Skipping import.");
      return;
    }

    if (dictionaryConfig.isResetBeforeImport()) {
      log.info("Resetting dictionary before import.");
      dictionaryRepository.deleteAll();
    }

    String filePath = dictionaryConfig.getSourceFilePath();

    List<DictionaryWord> batch = new ArrayList<>();
    int batchSize = 500;

    try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {

      String line;
      br.readLine();

      while ((line = br.readLine()) != null) {
        String[] parts = line.split(",", 2);
        if (parts.length < 2) continue;

        DictionaryWord word = new DictionaryWord();
        word.setEnglish(parts[0].trim());
        word.setUkrainian(parts[1].trim());

        batch.add(word);

        if (batch.size() >= batchSize) {
          dictionaryRepository.saveAll(batch);
          batch.clear();
        }
      }
      if (!batch.isEmpty()) {
        dictionaryRepository.saveAll(batch);
      }
    }

    log.info("Dictionary import completed from file: {}", filePath);
  }
}
