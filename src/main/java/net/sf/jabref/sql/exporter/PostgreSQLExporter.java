/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General public static License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General public static License for more details.

    You should have received a copy of the GNU General public static License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package net.sf.jabref.sql.exporter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import net.sf.jabref.model.entry.IdGenerator;
import net.sf.jabref.sql.DBStrings;
import net.sf.jabref.sql.SQLUtil;

final public class PostgreSQLExporter extends DBExporter {

    private static PostgreSQLExporter instance;


    private PostgreSQLExporter() {
    }

    /**
     * @return The singleton instance of the PostgreSQLExporter
     */
    public static PostgreSQLExporter getInstance() {
        if (PostgreSQLExporter.instance == null) {
            PostgreSQLExporter.instance = new PostgreSQLExporter();
        }
        return PostgreSQLExporter.instance;
    }

    @Override
    public Connection connectToDB(DBStrings dbstrings) throws Exception {
        this.dbStrings = dbstrings;
        String url = SQLUtil.createJDBCurl(dbstrings, true);
        String drv = "org.postgresql.Driver";

        Class.forName(drv).newInstance();
        try (Connection conn = DriverManager.getConnection(url, dbstrings.getUsername(), dbstrings.getPassword());
                Statement statement = (Statement) SQLUtil.processQueryWithResults(conn,
                        "SELECT count(*) AS alreadyThere FROM pg_database WHERE datname='" + dbStrings.getDatabase()
                                + '\'');
                ResultSet rs = statement.getResultSet()) {

            rs.next();
            if (rs.getInt("alreadyThere") == 0) {
                SQLUtil.processQuery(conn, "CREATE DATABASE " + dbStrings.getDatabase());
            }
            rs.getStatement().close();
            conn.close();
        }

        Connection conn = DriverManager.getConnection(url, dbstrings.getUsername(), dbstrings.getPassword());
        createPLPGSQLFunction(conn);

        return conn;
    }

    private void createPLPGSQLFunction(Connection conn) throws SQLException {
        SQLUtil.processQuery(
                conn,
                "create or replace function create_table_if_not_exists (create_sql text) returns bool as $$"
                        + "BEGIN"
                        + "\tBEGIN"
                        + "\t\tEXECUTE create_sql;"
                        + "\t\tException when duplicate_table THEN"
                        + "\t\tRETURN false;"
                        + "\tEND;"
                        + "\tRETURN true;"
                        + "END;" + "$$" + "Language plpgsql;");
    }

    /**
     * Generates SQL necessary to create all tables in a MySQL database, and
     * writes it to appropriate output.
     *
     * @param out The output (PrintStream or Connection) object to which the DML
     *            should be written.
     */
    @Override
    protected void createTables(Object out) throws SQLException {

        SQLUtil.processQuery(out,
                "SELECT create_table_if_not_exists ('CREATE TABLE jabref_database ( \n"
                        + "database_id SERIAL NOT NULL, \n"
                        + "database_name VARCHAR(64) NOT NULL, \n"
                        + "md5_path VARCHAR(32) NOT NULL, \n"
                        + "PRIMARY KEY (database_id)\n );')");
        SQLUtil.processQuery(
                out,
                "SELECT create_table_if_not_exists ('CREATE TABLE entry_types ( \n"
                        + "entry_types_id    SERIAL, \n"
                        + "label TEXT, \n"
                        + SQLUtil.fieldsAsCols(SQLUtil.getAllFields(),
                        " VARCHAR(3) DEFAULT NULL") + ", \n"
                        + "PRIMARY KEY (entry_types_id) \n" + ");')");
        SQLUtil.processQuery(
                out,
                "SELECT create_table_if_not_exists ('CREATE TABLE entries ( \n"
                        + "entries_id      SERIAL, \n"
                        + "jabref_eid      VARCHAR("
                        + IdGenerator.getMinimumIntegerDigits()
                        + ")   DEFAULT NULL, \n"
                        + "database_id INTEGER, \n"
                        + "entry_types_id  INTEGER DEFAULT NULL, \n"
                        + "cite_key        VARCHAR(100)     DEFAULT NULL, \n"
                        + SQLUtil.fieldsAsCols(SQLUtil.getAllFields(),
                        " TEXT DEFAULT NULL")
                        + ",\n"
                        + "PRIMARY KEY (entries_id), \n"
                        + "FOREIGN KEY (entry_types_id) REFERENCES entry_types (entry_types_id), \n"
                        + "FOREIGN KEY (database_id) REFERENCES jabref_database(database_id) \n"
                        + ");')");
        SQLUtil.processQuery(out,
                "SELECT create_table_if_not_exists ('CREATE TABLE strings ( \n"
                        + "strings_id      SERIAL, \n"
                        + "label      VARCHAR(100)  DEFAULT NULL, \n"
                        + "content    VARCHAR(200)  DEFAULT NULL, \n"
                        + "database_id INTEGER, \n"
                        + "FOREIGN KEY (database_id) REFERENCES jabref_database(database_id), \n"
                        + "PRIMARY KEY (strings_id) \n" + ");')");
        SQLUtil.processQuery(out,
                "SELECT create_table_if_not_exists ('CREATE TABLE group_types ( \n"
                        + "group_types_id  SERIAL, \n"
                        + "label   VARCHAR(100)    DEFAULT NULL, \n"
                        + "PRIMARY KEY (group_types_id) \n" + ");')");
        SQLUtil.processQuery(
                out,
                "SELECT create_table_if_not_exists ('CREATE TABLE groups ( \n"
                        + "groups_id       SERIAL, \n"
                        + "group_types_id  INTEGER         DEFAULT NULL, \n"
                        + "label           VARCHAR(100)    DEFAULT NULL, \n"
                        + "database_id INTEGER, \n"
                        + "parent_id       INTEGER         DEFAULT NULL, \n"
                        + "search_field       VARCHAR(100)          DEFAULT NULL, \n"
                        + "search_expression  VARCHAR(200)          DEFAULT NULL, \n"
                        + "case_sensitive  BOOLEAN       DEFAULT NULL, \n"
                        + "reg_exp BOOLEAN DEFAULT NULL, \n"
                        + "hierarchical_context INTEGER DEFAULT NULL, \n"
                        + "FOREIGN KEY (database_id) REFERENCES jabref_database(database_id), \n"
                        + "PRIMARY KEY (groups_id) \n" + ");')");
        SQLUtil.processQuery(
                out,
                "SELECT create_table_if_not_exists ('CREATE TABLE entry_group ( \n"
                        + "entries_id       SERIAL, \n"
                        + "groups_id        INTEGER        DEFAULT NULL, \n"
                        + "FOREIGN KEY (entries_id) REFERENCES entries (entries_id) ON DELETE CASCADE, \n"
                        + "FOREIGN KEY (groups_id)  REFERENCES groups (groups_id), \n"
                        + "PRIMARY KEY (groups_id, entries_id) \n" + ");')");
    }
}
