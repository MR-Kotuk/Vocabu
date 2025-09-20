package tg.vocabu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tg.vocabu.model.entity.Vocabulary;

public interface VocabularyRepository extends JpaRepository<Vocabulary, Long> {}
