CREATE TABLE IF NOT EXISTS last_run_time (
  id BIGSERIAL NOT NULL PRIMARY KEY,
  last_run timestamp NOT NULL
);
