package tg.vocabu.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tg.vocabu.model.entity.dictionary.DictionaryWord;

public interface DictionaryRepository extends JpaRepository<DictionaryWord, Long> {

  @Query(
      "SELECT CASE "
          + "WHEN LOWER(:word) LIKE LOWER(d.english) THEN d.ukrainian "
          + "WHEN LOWER(:word) LIKE LOWER(d.ukrainian) THEN d.english "
          + "END "
          + "FROM DictionaryWord d "
          + "WHERE LOWER(:word) LIKE LOWER(d.english) "
          + "   OR LOWER(:word) LIKE LOWER(d.ukrainian)")
  Optional<String> findTranslation(@Param("word") String word);

  @Query(value = "SELECT * FROM dictionary ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
  Optional<DictionaryWord> findRandomWord();
}
