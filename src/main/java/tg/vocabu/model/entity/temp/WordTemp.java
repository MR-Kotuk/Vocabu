package tg.vocabu.model.entity.temp;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import tg.vocabu.model.enums.TranslationMethod;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Table(name = "words_temp")
public class WordTemp {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long chatId;

  private String english;
  private String ukrainian;

  private TranslationMethod translationMethod;
}
