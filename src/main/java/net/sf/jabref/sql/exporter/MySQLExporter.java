/*  Copyright (C) 2003-2011 JabRef contributors.
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
import java.sql.SQLException;

import net.sf.jabref.model.entry.IdGenerator;
import net.sf.jabref.sql.DBStrings;
import net.sf.jabref.sql.SQLUtil;

/**
 *
 * @author ifsteinm.
 *
 *         Jan 20th Extends DBExporter to provide features specific for MySQL
 *         Created after a refactory on SQLUtil
 *
 */

final public class MySQLExporter extends DBExporter {

    private static MySQLExporter instance;
    private static final String OPT_ALLOW_MULTI_QUERIES = "?allowMultiQueries=true";


    private MySQLExporter() {
    }

    /**
     *
     * @return The singleton instance of the MySQLExporter
     */
    public static MySQLExporter getInstance() {
        if (MySQLExporter.instance == null) {
            MySQLExporter.instance = new MySQLExporter();
        }
        return MySQLExporter.instance;
    }

    @Override
    public Connection connectToDB(DBStrings dbstrings) throws Exception {
        this.dbStrings = dbstrings;

        dbStrings.setDbParameters(OPT_ALLOW_MULTI_QUERIES);
        String url = SQLUtil.createJDBCurl(dbstrings, false);
        String drv = "com.mysql.jdbc.Driver";


        Class.forName(drv).newInstance();
        Connection conn = DriverManager.getConnection(url,
                dbstrings.getUsername(), dbstrings.getPassword());
        SQLUtil.processQuery(conn, "CREATE DATABASE IF NOT EXISTS `"
                + dbStrings.getDatabase() + '`');

        conn.setCatalog(dbStrings.getDatabase());
        return conn;
    }

    /**
     * Generates SQLnecessary to create all tables in a MySQL database, and
     * writes it to appropriate output.
     *
     * @param out
     *            The output (PrintStream or Connection) object to which the DML
     *            should be written.
     */
    @Override
    protected void createTables(Object out) throws SQLException {

        SQLUtil.processQuery(
                out,
                "CREATE TABLE IF NOT EXISTS jabref_database ( \n"
                        + "database_id INT UNSIGNED NOT NULL AUTO_INCREMENT, \n"
                        + "database_name VARCHAR(64) NOT NULL, \n"
                        + "md5_path VARCHAR(32) NOT NULL, \n"
                        + "PRIMARY KEY (database_id)\n );");
        SQLUtil.processQuery(
                out,
                "CREATE TABLE IF NOT EXISTS entry_types ( \n"
                        + "entry_types_id    INT UNSIGNED  NOT NULL AUTO_INCREMENT, \n"
                        + "label			 TEXT, \n"
                        + SQLUtil.fieldsAsCols(SQLUtil.getAllFields(),
                                " VARCHAR(3) DEFAULT NULL") + ", \n"
                        + "PRIMARY KEY (entry_types_id) \n" + ");");
        SQLUtil.processQuery(
                out,
                "CREATE TABLE IF NOT EXISTS entries ( \n"
                        + "entries_id      INTEGER         NOT NULL AUTO_INCREMENT, \n"
                        + "jabref_eid      VARCHAR("
                        + IdGenerator.getMinimumIntegerDigits()
                        + ")   DEFAULT NULL, \n"
                        + "database_id INT UNSIGNED, \n"
                        + "entry_types_id  INT UNSIGNED         DEFAULT NULL, \n"
                        + "cite_key        VARCHAR(100)     DEFAULT NULL, \n"
                        + SQLUtil.fieldsAsCols(SQLUtil.getAllFields(),
                                " TEXT DEFAULT NULL")
                        + ",\n"
                        + "PRIMARY KEY (entries_id), \n"
                        + "INDEX(entry_types_id), \n"
                        + "FOREIGN KEY (entry_types_id) REFERENCES entry_types(entry_types_id), \n"
                        + "FOREIGN KEY (database_id) REFERENCES jabref_database(database_id) \n);");
        SQLUtil.processQuery(
                out,
                "CREATE TABLE IF NOT EXISTS strings ( \n"
                        + "strings_id      INTEGER         NOT NULL AUTO_INCREMENT, \n"
                        + "label      VARCHAR(100)  DEFAULT NULL, \n"
                        + "content    VARCHAR(200)  DEFAULT NULL, \n"
                        + "database_id INT UNSIGNED, \n"
                        + "FOREIGN KEY (database_id) REFERENCES jabref_database(database_id), \n"
                        + "PRIMARY KEY (strings_id) \n" + ");");
        SQLUtil.processQuery(out, "CREATE TABLE IF NOT EXISTS group_types ( \n"
                + "group_types_id  INTEGER     NOT NULL AUTO_INCREMENT, \n"
                + "label   VARCHAR(100)    DEFAULT NULL, \n"
                + "PRIMARY KEY (group_types_id) \n" + ");");
        SQLUtil.processQuery(
                out,
                "CREATE TABLE IF NOT EXISTS groups ( \n"
                        + "groups_id       INTEGER         NOT NULL AUTO_INCREMENT, \n"
                        + "group_types_id  INTEGER         DEFAULT NULL, \n"
                        + "label           VARCHAR(100)    DEFAULT NULL, \n"
                        + "database_id INT UNSIGNED, \n"
                        + "parent_id       INTEGER         DEFAULT NULL, \n"
                        + "search_field       VARCHAR(100)          DEFAULT NULL, \n"
                        + "search_expression  VARCHAR(200)          DEFAULT NULL, \n"
                        + "case_sensitive  BOOL          DEFAULT NULL, \n"
                        + "reg_exp BOOL DEFAULT NULL, \n"
                        + "hierarchical_context INTEGER DEFAULT NULL, \n"
                        + "FOREIGN KEY (database_id) REFERENCES jabref_database(database_id), \n"
                        + "PRIMARY KEY (groups_id) \n" + ");");
        SQLUtil.processQuery(
                out,
                "CREATE TABLE IF NOT EXISTS entry_group ( \n"
                        + "entries_id       INTEGER        NOT NULL AUTO_INCREMENT, \n"
                        + "groups_id        INTEGER        DEFAULT NULL, \n"
                        + "INDEX(entries_id), \n"
                        + "INDEX(groups_id), \n"
                        + "FOREIGN KEY (entries_id) REFERENCES entries(entries_id) ON DELETE CASCADE, \n"
                        + "FOREIGN KEY (groups_id)  REFERENCES groups(groups_id), \n"
                        + "PRIMARY KEY (groups_id, entries_id) \n" + ");");
    }
}