# Use MariaDB Connector

## Context and Problem Statement

JabRef needs to connect to a MySQL database.
See [Shared SQL Database](https://help.jabref.org/en/SQLDatabase) for more information.

## Considered Options

* Use MariaDB Connector
* Use MySQL Connector

Other alternatives are listed at <https://stackoverflow.com/a/31312280/873282>.

## Decision Outcome

Chosen option: "Use MariaDB Connector", because comes out best (see below).

## Pros and Cons of the Options

### Use MariaDB Connector

The [MariaDB Connector](https://mariadb.com/kb/en/library/about-mariadb-connector-j/) is a LGPL-licensed JDBC driver to connect to MySQL and MariaDB.

* Good, because can be used as drop-in replacement for MySQL connectopr

### Use MySQL Connector

The [MySQL Connector](https://www.mysql.com/de/products/connector/) is distributed by Oracle and licensed under GPL-2. Source: <https://downloads.mysql.com/docs/licenses/connector-j-8.0-gpl-en.pdf>.
Oracle added the [Universal FOSS Exception, Version 1.0](https://oss.oracle.com/licenses/universal-foss-exception/) to it, which seems to limit the effects of GPL.
More information on the FOSS Exception are available at <https://www.mysql.com/de/about/legal/licensing/foss-exception/>.

* Good, because it stems from the same development team than MySQL
* Bad, because the "Universal FOSS Exception" makes licensing more complicated.
