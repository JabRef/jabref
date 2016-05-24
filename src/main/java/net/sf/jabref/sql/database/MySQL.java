package net.sf.jabref.sql.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import net.sf.jabref.model.entry.IdGenerator;
import net.sf.jabref.sql.DBStrings;
import net.sf.jabref.sql.Database;
import net.sf.jabref.sql.DatabaseType;
import net.sf.jabref.sql.SQLUtil;

public class MySQL implements Database {

    public static final String DRIVER = "com.mysql.jdbc.Driver";


    private void loadDriver() throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        Class.forName(DRIVER).newInstance();
    }

    @Override
    public Connection connect(String url, String username, String password)
            throws IllegalAccessException, InstantiationException, ClassNotFoundException, SQLException {
        loadDriver();

        return DriverManager.getConnection(url, username, password);
    }

    @Override
    public String getReadColumnNamesQuery() {
        return "SHOW columns FROM entries;";
    }

    @Override
    public String getCreateTableSQL(Table table) {
        switch (table) {

        case JABREF_DATABASE:
            return
                    "CREATE TABLE IF NOT EXISTS jabref_database ( \n"
                            + "database_id INT UNSIGNED NOT NULL AUTO_INCREMENT, \n"
                            + "database_name VARCHAR(64) NOT NULL, \n"
                            + "md5_path VARCHAR(32) NOT NULL, \n"
                            + "PRIMARY KEY (database_id)\n );";
        case ENTRY_TYPES:
            return "CREATE TABLE IF NOT EXISTS entry_types ( \n"
                    + "entry_types_id    INT UNSIGNED  NOT NULL AUTO_INCREMENT, \n"
                    + "label			 TEXT, \n"
                    + SQLUtil.fieldsAsCols(SQLUtil.getAllFields(),
                    " VARCHAR(3) DEFAULT NULL") + ", \n"
                    + "PRIMARY KEY (entry_types_id) \n" + ");";
        case ENTRIES:
            return
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
                            + "FOREIGN KEY (database_id) REFERENCES jabref_database(database_id) \n);";
        case STRINGS:
            return "CREATE TABLE IF NOT EXISTS strings ( \n"
                    + "strings_id      INTEGER         NOT NULL AUTO_INCREMENT, \n"
                    + "label      VARCHAR(100)  DEFAULT NULL, \n"
                    + "content    VARCHAR(200)  DEFAULT NULL, \n"
                    + "database_id INT UNSIGNED, \n"
                    + "FOREIGN KEY (database_id) REFERENCES jabref_database(database_id), \n"
                    + "PRIMARY KEY (strings_id) \n" + ");";
        case GROUP_TYPES:
            return "CREATE TABLE IF NOT EXISTS group_types ( \n"
                    + "group_types_id  INTEGER     NOT NULL AUTO_INCREMENT, \n"
                    + "label   VARCHAR(100)    DEFAULT NULL, \n"
                    + "PRIMARY KEY (group_types_id) \n" + ");";
        case GROUPS:
            return
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
                            + "PRIMARY KEY (groups_id) \n" + ");";
        case ENTRY_GROUP:
            return
                    "CREATE TABLE IF NOT EXISTS entry_group ( \n"
                            + "entries_id       INTEGER        NOT NULL AUTO_INCREMENT, \n"
                            + "groups_id        INTEGER        DEFAULT NULL, \n"
                            + "INDEX(entries_id), \n"
                            + "INDEX(groups_id), \n"
                            + "FOREIGN KEY (entries_id) REFERENCES entries(entries_id) ON DELETE CASCADE, \n"
                            + "FOREIGN KEY (groups_id)  REFERENCES groups(groups_id), \n"
                            + "PRIMARY KEY (groups_id, entries_id) \n" + ");";
        default:
            return "";
        }

    }

    private static final String OPT_ALLOW_MULTI_QUERIES = "?allowMultiQueries=true";

    @Override
    public Connection connectAndEnsureDatabaseExists(DBStrings dbStrings)
            throws SQLException, IllegalAccessException, ClassNotFoundException, InstantiationException {

        dbStrings.setDbParameters(OPT_ALLOW_MULTI_QUERIES);
        String url = SQLUtil.createJDBCurl(dbStrings, false);

        Connection conn = connect(url, dbStrings.getDbPreferences().getUsername(), dbStrings.getPassword());

        String query = "CREATE DATABASE IF NOT EXISTS `" + dbStrings.getDbPreferences().getDatabase() + '`';
        SQLUtil.processQuery(conn, query);
        conn.setCatalog(dbStrings.getDbPreferences().getDatabase());

        return conn;
    }

    @Override
    public DatabaseType getType() {
        return DatabaseType.MYSQL;
    }
}

