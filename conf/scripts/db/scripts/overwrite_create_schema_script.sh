#!/bin/bash
pg_dump -U postgres -d ls_dev --schema-only --schema "public" -O | cat <(printf  '#!/bin/bash \n  psql -U $USERNAME -d $DBNAME -p $PORT -c \n " \n ') <(cat -) <(printf ' " ') > create_schema.sh
