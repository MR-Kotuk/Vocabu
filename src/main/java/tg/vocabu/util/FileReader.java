package tg.vocabu.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import tg.vocabu.exception.ErrorReadingFileException;
import tg.vocabu.exception.ResourceNotFoundException;

@Component
public class FileReader {

  public static String readFile(String filePath) {

    try {
      InputStream inputStream = FileReader.class.getResourceAsStream(filePath);

      if (inputStream == null) {
        throw new ResourceNotFoundException("File not found in classpath: " + filePath);
      }

      try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
        return reader.lines().collect(Collectors.joining(System.lineSeparator()));
      }
    } catch (IOException e) {
      throw new ErrorReadingFileException("Error reading file from classpath: " + filePath);
    }
  }
}
