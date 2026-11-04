CREATE TABLE product (
  id      BIGSERIAL PRIMARY KEY,
  name    TEXT   NOT NULL,
  stock   INT    NOT NULL,
  version BIGINT NOT NULL DEFAULT 0
);
