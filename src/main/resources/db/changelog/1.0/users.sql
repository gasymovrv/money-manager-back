-- liquibase formatted sql

-- changeset "Gasymov Ruslan":000000-create-table-users
CREATE TABLE users (
  id varchar(255),
  name varchar(255),
  picture text,
  email varchar(255),
  locale varchar(255),
  last_visit timestamp,
  current_account_id bigint
);

-- changeset "Gasymov Ruslan":000000-create-constraint-users
ALTER TABLE users ALTER COLUMN id SET NOT NULL;
ALTER TABLE users ADD PRIMARY KEY (id);
ALTER TABLE users ALTER COLUMN current_account_id SET NOT NULL;

-- changeset "Gasymov Ruslan":000000-add-column-provider-users
ALTER TABLE users ADD COLUMN provider varchar(50) DEFAULT 'GOOGLE';
ALTER TABLE users ALTER COLUMN provider SET NOT NULL;
