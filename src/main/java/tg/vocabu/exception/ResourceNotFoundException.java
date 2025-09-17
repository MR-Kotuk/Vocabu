package tg.vocabu.exception;

import java.io.IOException;

public class ResourceNotFoundException extends IOException {

  public ResourceNotFoundException(String message) {
    super(message);
  }
}
