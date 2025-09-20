package tg.vocabu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tg.vocabu.model.entity.WordTemp;

@Repository
public interface WordTempRepository extends JpaRepository<WordTemp, Long> {}
