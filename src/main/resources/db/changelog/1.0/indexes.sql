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

-- changeset "Gasymov Ruslan":000000-create-saving-indexes-optimized
-- Composite index for the most common query pattern: account_id + date range + sort by date
-- Covers: WHERE account_id = ? AND date BETWEEN ? AND ? ORDER BY date
CREATE INDEX IF NOT EXISTS idx_saving_account_date ON saving(account_id, date);
-- Composite index for sorting by value while filtering by account
-- Covers: WHERE account_id = ? ORDER BY value_
CREATE INDEX IF NOT EXISTS idx_saving_account_value ON saving(account_id, value_);
-- Covering index for date range queries with value sorting
-- Covers: WHERE account_id = ? AND date BETWEEN ? AND ? ORDER BY value_
CREATE INDEX IF NOT EXISTS idx_saving_account_date_value ON saving(account_id, date, value_);
-- Remove old indexes
DROP INDEX IF EXISTS idx_saving_date;
DROP INDEX IF EXISTS idx_saving_account_id;

-- changeset "Gasymov Ruslan":000000-create-operation-indexes-optimized
-- Composite index for filtering incomes by account and saving
-- Covers: WHERE account_id = ? AND saving_id IN (...)
CREATE INDEX IF NOT EXISTS idx_income_account_saving ON income(account_id, saving_id);
CREATE INDEX IF NOT EXISTS idx_expense_account_saving ON expense(account_id, saving_id);
-- Remove old indexes
DROP INDEX IF EXISTS idx_expense_account_id;
DROP INDEX IF EXISTS idx_income_account_id;
-- Composite index for category-based filtering with saving lookup
-- Covers: WHERE account_id = ? AND saving_id IN (...) AND category_id IN (...)
CREATE INDEX IF NOT EXISTS idx_income_account_saving_category ON income(account_id, saving_id, category_id);
CREATE INDEX IF NOT EXISTS idx_expense_account_saving_category ON expense(account_id, saving_id, category_id);
-- Remove old indexes
DROP INDEX IF EXISTS idx_income_category_id;
DROP INDEX IF EXISTS idx_expense_category_id;
