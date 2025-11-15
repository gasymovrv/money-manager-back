-- liquibase formatted sql

-- changeset "Gasymov Ruslan":000000-create-table-telegram-users
CREATE TABLE telegram_users (
  telegram_id bigint,
  user_id varchar(255),
  first_name varchar(255),
  last_name varchar(255),
  username varchar(255),
  photo_url text,
  auth_date timestamp,
  created_at timestamp,
  updated_at timestamp
);

-- changeset "Gasymov Ruslan":000000-create-constraint-telegram-users
ALTER TABLE telegram_users ALTER COLUMN telegram_id SET NOT NULL;
ALTER TABLE telegram_users ADD PRIMARY KEY (telegram_id);
ALTER TABLE telegram_users ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE telegram_users
  ADD CONSTRAINT fk_telegram_users_user_id
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- changeset "Gasymov Ruslan":000000-create-table-telegram-messages
CREATE TABLE telegram_messages (
  message_id bigint,
  telegram_id bigint,
  chat_id bigint,
  message_text text,
  processed_at timestamp
);

-- changeset "Gasymov Ruslan":000000-create-constraint-telegram-messages
ALTER TABLE telegram_messages ALTER COLUMN message_id SET NOT NULL;
ALTER TABLE telegram_messages ADD PRIMARY KEY (message_id);
ALTER TABLE telegram_messages ALTER COLUMN telegram_id SET NOT NULL;
ALTER TABLE telegram_messages ALTER COLUMN chat_id SET NOT NULL;
ALTER TABLE telegram_messages ALTER COLUMN processed_at SET NOT NULL;

-- changeset "Gasymov Ruslan":000000-create-index-telegram-messages
CREATE INDEX idx_telegram_messages_telegram_id ON telegram_messages(telegram_id);
