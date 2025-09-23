package tg.vocabu.service.command.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import tg.vocabu.repository.UserRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCommandHandler {

  private final UserRepository userRepository;

  public void handleUsersStatus(SendMessage message) {

    log.trace("Received users status command from admin");

    long usersCount = userRepository.count();
    message.setText("Users count: " + usersCount);
  }

  public void handleUserList(SendMessage message) {

    log.trace("Received user list command from admin");

    StringBuilder userList = new StringBuilder("User List:\n\n");

    userRepository
        .findAll()
        .forEach(
            user ->
                userList
                    .append("ID: ")
                    .append(user.getChatId())
                    .append("\nUsername: @")
                    .append(user.getUserName())
                    .append("\nRegion: ")
                    .append(user.getLanguageCode())
                    .append("\nBot: ")
                    .append(user.isBot())
                    .append("\n--------------------\n"));

    message.setText(userList.toString());
  }
}
