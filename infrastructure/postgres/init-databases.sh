#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE DATABASE codemonk_knowledge;
    CREATE DATABASE codemonk_search;
    
    \c codemonk_search;
    CREATE EXTENSION IF NOT EXISTS vector;
EOSQL
