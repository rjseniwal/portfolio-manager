CREATE INDEX ix_accounts_space_name ON accounts (space_id, name, id);
CREATE INDEX ix_categories_space_name ON categories (space_id, name, id);
CREATE INDEX ix_transactions_space_date_created ON cash_transactions (space_id, transaction_date, created_at, id);
CREATE INDEX ix_transactions_space_type_date_created ON cash_transactions (space_id, type, transaction_date, created_at, id);
