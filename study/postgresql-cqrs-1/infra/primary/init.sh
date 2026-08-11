#!/bin/bash
set -e

# Create a replication user
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE ROLE replicator WITH REPLICATION LOGIN PASSWORD 'replicator_pass';
EOSQL

# Allow replication connections from the replica
echo "host replication replicator all md5" >> "$PGDATA/pg_hba.conf"
