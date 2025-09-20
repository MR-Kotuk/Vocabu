package tg.vocabu.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tg.vocabu.model.enums.CallbackQuery;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "callback_queries_temp")
public class CallbackQueryTemp {

  @Id private Long chatId;

  private CallbackQuery callbackQuery;
}
