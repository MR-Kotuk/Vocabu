package tg.vocabu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tg.vocabu.model.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  boolean existsByChatId(Long chatId);
}
