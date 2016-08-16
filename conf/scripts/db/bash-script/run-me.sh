#!/bin/bash
export USERNAME=USERNAME
export PGPASSWORD=PASSWORD
export DBNAME=DBNAME
export PORT=5432

./scripts/create_extensions.sh
./scripts/create_schema.sh