package tg.vocabu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tg.vocabu.model.entity.temp.Exercise;

public interface ExerciseRepository extends JpaRepository<Exercise, Long> {}
