package tg.vocabu.config;

public class FilePathConfig {

  private static final String BASE_PATH = "/txt/";
  private static final String USER_PATH = BASE_PATH + "user/";
  private static final String ADMIN_PATH = BASE_PATH + "admin/";

  public static final String START_MESSAGE_FILE = USER_PATH + "startMessage.txt";
  public static final String HELP_MESSAGE_FILE = USER_PATH + "helpMessage.txt";

  public static final String ADMIN_START_MESSAGE_FILE = ADMIN_PATH + "startMessageAdmin.txt";
  public static final String ADMIN_HELP_MESSAGE_FILE = ADMIN_PATH + "helpMessageAdmin.txt";
}
