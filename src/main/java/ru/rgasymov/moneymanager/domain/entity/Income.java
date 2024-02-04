package ru.rgasymov.moneymanager.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

@Entity
@Table(name = "income")
@Getter
@Setter
@ToString(callSuper = true)
@NoArgsConstructor
@SuperBuilder
public class Income extends BaseOperation implements Cloneable {

  @ManyToOne(fetch = FetchType.LAZY)
  @Fetch(FetchMode.JOIN)
  @JoinColumn(name = "category_id")
  private IncomeCategory category;

  @Override
  public Income clone() {
    try {
      return (Income) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }
}
