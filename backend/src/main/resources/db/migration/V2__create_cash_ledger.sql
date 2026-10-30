CREATE TABLE accounts (
    id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    space_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(16) NOT NULL,
    starting_balance DECIMAL(19,2) NOT NULL,
    tracking_start_date DATE NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_accounts_space_id (space_id, id),
    CONSTRAINT fk_account_space FOREIGN KEY (space_id) REFERENCES spaces(id),
    CONSTRAINT ck_account_type CHECK (type IN ('BANK', 'CASH'))
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4;

CREATE TABLE categories (
    id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    space_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(16) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_categories_space_id (space_id, id),
    CONSTRAINT fk_category_space FOREIGN KEY (space_id) REFERENCES spaces(id),
    CONSTRAINT ck_category_type CHECK (type IN ('INCOME', 'EXPENSE'))
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4;

CREATE TABLE cash_transactions (
    id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    space_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    transaction_date DATE NOT NULL,
    type VARCHAR(16) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    source_account_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
    destination_account_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
    category_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
    description VARCHAR(1000) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY ix_transactions_date (space_id, transaction_date, id),
    KEY ix_transactions_source (space_id, source_account_id),
    KEY ix_transactions_destination (space_id, destination_account_id),
    KEY ix_transactions_category (space_id, category_id),
    CONSTRAINT fk_transaction_space FOREIGN KEY (space_id) REFERENCES spaces(id),
    CONSTRAINT fk_transaction_source FOREIGN KEY (space_id, source_account_id) REFERENCES accounts(space_id, id),
    CONSTRAINT fk_transaction_destination FOREIGN KEY (space_id, destination_account_id) REFERENCES accounts(space_id, id),
    CONSTRAINT fk_transaction_category FOREIGN KEY (space_id, category_id) REFERENCES categories(space_id, id),
    CONSTRAINT ck_transaction_amount CHECK (amount > 0),
    CONSTRAINT ck_transaction_shape CHECK (
        (type = 'INCOME' AND source_account_id IS NULL AND destination_account_id IS NOT NULL AND category_id IS NOT NULL) OR
        (type = 'EXPENSE' AND source_account_id IS NOT NULL AND destination_account_id IS NULL AND category_id IS NOT NULL) OR
        (type = 'TRANSFER' AND source_account_id IS NOT NULL AND destination_account_id IS NOT NULL AND source_account_id <> destination_account_id AND category_id IS NULL)
    )
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4;
