package tg.vocabu.model.entity;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@Embeddable
@SuperBuilder
@NoArgsConstructor
public class Word {

  private String english;
  private String ukrainian;

  private int score;
}
