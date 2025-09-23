package tg.vocabu.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tg.vocabu.model.entity.user.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  boolean existsByChatId(Long chatId);

  @Query("SELECT u.banned FROM User u WHERE u.chatId = :chatId")
  Boolean isBanned(@Param("chatId") Long chatId);

  @Query("SELECT u.userName FROM User u WHERE u.id = :id")
  String findUserNameById(@Param("id") Long id);

  @Transactional
  @Modifying(clearAutomatically = true)
  @Query("UPDATE User u SET u.banned = true WHERE u.id = :id")
  void banUserById(@Param("id") Long id);

  @Transactional
  @Modifying(clearAutomatically = true)
  @Query("UPDATE User u SET u.banned = false WHERE u.id = :id")
  void unbanUserById(@Param("id") Long id);
}
