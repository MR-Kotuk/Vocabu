package tg.vocabu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tg.vocabu.model.entity.temp.CallbackQueryTemp;

@Repository
public interface CallbackQueryRepository extends JpaRepository<CallbackQueryTemp, Long> {}
