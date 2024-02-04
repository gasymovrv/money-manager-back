package ru.rgasymov.moneymanager.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "expense_category")
@Getter
@ToString(callSuper = true)
@NoArgsConstructor
@SuperBuilder
public class ExpenseCategory extends BaseOperationCategory {
}
