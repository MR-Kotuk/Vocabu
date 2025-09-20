package tg.vocabu.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tg.vocabu.model.entity.Word;

@Repository
public interface WordRepository extends JpaRepository<Word, Long> {

  @Query("SELECT CASE WHEN COUNT(w) > 0 THEN true ELSE false END " + "FROM Word w " + "WHERE w.english = :english OR w.ukrainian = :ukrainian")
  boolean existsByEnglishOrUkrainian(@Param("english") String english, @Param("ukrainian") String ukrainian);

  @Query("SELECT CASE WHEN COUNT(w) > 0 THEN true ELSE false END " + "FROM Word w " + "WHERE w.english = :english")
  boolean existsByEnglish(@Param("english") String english);

  List<Word> findByChatIdAndLearned(long chatId, boolean learned);
}
