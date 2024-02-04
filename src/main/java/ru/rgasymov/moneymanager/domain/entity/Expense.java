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
@Table(name = "expense")
@Getter
@Setter
@ToString(callSuper = true)
@NoArgsConstructor
@SuperBuilder
public class Expense extends BaseOperation implements Cloneable {

  @ManyToOne(fetch = FetchType.LAZY)
  @Fetch(FetchMode.JOIN)
  @JoinColumn(name = "category_id")
  private ExpenseCategory category;

  @Override
  public Expense clone() {
    try {
      return (Expense) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }
}
