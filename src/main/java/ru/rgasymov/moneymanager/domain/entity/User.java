package ru.rgasymov.moneymanager.domain.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import ru.rgasymov.moneymanager.domain.enums.AuthProviders;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@ToString(onlyExplicitlyIncluded = true)
public class User implements Serializable {
  @Serial
  private static final long serialVersionUID = 1234567L;

  @Id
  @ToString.Include
  private String id;

  @ToString.Include
  private String name;

  @Column
  private String picture;

  private String email;

  private String locale;

  @Column(name = "last_visit")
  private LocalDateTime lastVisit;

  @Enumerated(EnumType.STRING)
  private AuthProviders provider;

  @OneToOne(cascade = CascadeType.MERGE)
  @JoinColumn(name = "current_account_id")
  private Account currentAccount;
}
