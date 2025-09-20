package tg.vocabu.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "users")
public class User {

  @Id private Long id;
  private long chatId;

  private String firstName;
  private String lastName;
  private String userName;

  private String languageCode;

  private boolean isBot;
}
