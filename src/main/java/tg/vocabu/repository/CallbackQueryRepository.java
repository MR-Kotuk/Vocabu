package tg.vocabu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tg.vocabu.model.entity.CallbackQueryTemp;

public interface CallbackQueryRepository extends JpaRepository<CallbackQueryTemp, Long> {}
