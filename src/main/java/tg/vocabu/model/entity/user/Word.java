package tg.vocabu.model.entity.user;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "words")
public class Word {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private long chatId;

  private String english;
  private String ukrainian;

  private int score;

  @Builder.Default private boolean learned = false;

  public boolean incrementScore(int increment) {

    this.score += increment;

    if (this.score >= 100) {
      this.score = 100;
      this.learned = true;
    }

    return this.learned;
  }

  public void decrementScore(int decrement) {

    this.score -= decrement;

    if (this.score < 0) {
      this.score = 0;
    }
  }
}
