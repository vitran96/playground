create table if not exists stored_file (
  id            uuid primary key,
  original_name text        not null,
  size_bytes    bigint      not null,
  status        text        not null check (status in ('UPLOADING', 'READY')),
  chunk_size    integer,
  total_chunks  integer,
  created_at    timestamp with time zone not null default now()
);

create table if not exists upload_chunk (
  file_id     uuid    not null references stored_file(id) on delete cascade,
  chunk_index integer not null,
  primary key (file_id, chunk_index)
);
