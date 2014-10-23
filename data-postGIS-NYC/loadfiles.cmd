set SRID=4269
set DATABASE=nyc
set PORT=5432
set USERNAME=aus
set PGPASSWORD=aus123
REM for %%f in (*.shp) do shp2pgsql -I -s %SRID% %%f %%~nf > %%~nf.sql
for %%f in (*.sql) do psql -U %USERNAME% -d %DATABASE% -p %PORT% -f %%f
psql -U %USERNAME% -d %DATABASE% -p %PORT% -c "\d"