# How to test

## Database tests

### PostgreSQL

To quickly host a local PostgreSQL database, execute following statement:

\`\`\`shell command docker run -d -e POSTGRES\_USER=postgres -e POSTGRES\_PASSWORD=postgres -e POSTGRES\_DB=postgres -p 5432:5432 --name db postgres:10 postgres -c log\_statement=all

```text
Set the environment variable `DBMS` to `postgres` (or leave it unset)

Then, all DBMS Tests (annotated with `@org.jabref.testutils.category.DatabaseTest`) run properly.

### MySQL

A MySQL DBMS can be started using following command:

``´shell command
docker run -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=jabref -p 3800:3307 mysql:8.0 --port=3307
```

Set the environment variable `DBMS` to `mysql`.

