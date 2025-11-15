-- liquibase formatted sql

-- changeset "Gasymov Ruslan":000000-create-saving-indexes
CREATE INDEX IF NOT EXISTS idx_saving_date ON saving(date);
CREATE INDEX IF NOT EXISTS idx_saving_account_id ON saving(account_id);

-- changeset "Gasymov Ruslan":000000-create-income-indexes
CREATE INDEX IF NOT EXISTS idx_income_saving_id ON income(saving_id);
CREATE INDEX IF NOT EXISTS idx_income_account_id ON income(account_id);
CREATE INDEX IF NOT EXISTS idx_income_category_id ON income(category_id);

-- changeset "Gasymov Ruslan":000000-create-expense-indexes
CREATE INDEX IF NOT EXISTS idx_expense_saving_id ON expense(saving_id);
CREATE INDEX IF NOT EXISTS idx_expense_account_id ON expense(account_id);
CREATE INDEX IF NOT EXISTS idx_expense_category_id ON expense(category_id);

-- changeset "Gasymov Ruslan":000000-create-income-category-indexes
CREATE INDEX IF NOT EXISTS idx_income_category_account_id ON income_category(account_id);

-- changeset "Gasymov Ruslan":000000-create-expense-category-indexes
CREATE INDEX IF NOT EXISTS idx_expense_category_account_id ON expense_category(account_id);
