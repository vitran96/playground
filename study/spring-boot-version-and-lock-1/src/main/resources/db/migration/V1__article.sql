CREATE TABLE article (
  id      BIGSERIAL PRIMARY KEY,
  title   TEXT   NOT NULL,
  body    TEXT   NOT NULL,
  likes   INT    NOT NULL DEFAULT 0,
  version BIGINT NOT NULL DEFAULT 0
);
