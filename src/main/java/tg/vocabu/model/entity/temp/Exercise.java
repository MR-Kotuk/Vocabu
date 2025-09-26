package tg.vocabu.model.entity.temp;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "exercises")
public class Exercise {

  @Id private Long chatId;

  private Long wordId;

  private boolean engToUkr;

  private String english;
  private String ukrainian;

  private String[] options;
  private String[] statuses;
}
