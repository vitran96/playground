CREATE TABLE ticket_counter (
  id          BIGINT PRIMARY KEY,
  next_number INT NOT NULL
);
CREATE TABLE ticket (
  id     BIGSERIAL PRIMARY KEY,
  number INT NOT NULL
);
CREATE TABLE job (
  id         BIGSERIAL PRIMARY KEY,
  payload    TEXT        NOT NULL,
  status     TEXT        NOT NULL DEFAULT 'NEW',
  worker     TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
