package tg.vocabu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tg.vocabu.model.entity.WordTemp;

public interface WordTempRepository extends JpaRepository<WordTemp, Long> {}
