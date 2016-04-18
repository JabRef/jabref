package net.sf.jabref.sql.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import net.sf.jabref.model.entry.IdGenerator;
import net.sf.jabref.sql.DBStrings;
import net.sf.jabref.sql.Database;
import net.sf.jabref.sql.DatabaseType;
import net.sf.jabref.sql.SQLUtil;

public class PostgreSQL implements Database {

    public static final String DRIVER = "org.postgresql.Driver";

    private void loadDriver() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
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
        return "SELECT column_name FROM information_schema.columns WHERE table_name ='entries';";
    }

    @Override
    public String getCreateTableSQL(Table table) {
        switch (table) {

        case JABREF_DATABASE:
            return
                    "SELECT create_table_if_not_exists ('CREATE TABLE jabref_database ( \n"
                            + "database_id SERIAL NOT NULL, \n"
                            + "database_name VARCHAR(64) NOT NULL, \n"
                            + "md5_path VARCHAR(32) NOT NULL, \n"
                            + "PRIMARY KEY (database_id)\n );')";
        case ENTRY_TYPES:
            return
                    "SELECT create_table_if_not_exists ('CREATE TABLE entry_types ( \n"
                            + "entry_types_id    SERIAL, \n"
                            + "label TEXT, \n"
                            + SQLUtil.fieldsAsCols(SQLUtil.getAllFields(),
                            " VARCHAR(3) DEFAULT NULL") + ", \n"
                            + "PRIMARY KEY (entry_types_id) \n" + ");')";
        case ENTRIES:
            return
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
                            + ");')";
        case STRINGS:
            return
                    "SELECT create_table_if_not_exists ('CREATE TABLE strings ( \n"
                            + "strings_id      SERIAL, \n"
                            + "label      VARCHAR(100)  DEFAULT NULL, \n"
                            + "content    VARCHAR(200)  DEFAULT NULL, \n"
                            + "database_id INTEGER, \n"
                            + "FOREIGN KEY (database_id) REFERENCES jabref_database(database_id), \n"
                            + "PRIMARY KEY (strings_id) \n" + ");')";
        case GROUP_TYPES:
            return
                    "SELECT create_table_if_not_exists ('CREATE TABLE group_types ( \n"
                            + "group_types_id  SERIAL, \n"
                            + "label   VARCHAR(100)    DEFAULT NULL, \n"
                            + "PRIMARY KEY (group_types_id) \n" + ");')";
        case GROUPS:
            return
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
                            + "PRIMARY KEY (groups_id) \n" + ");')";
        case ENTRY_GROUP:
            return
                    "SELECT create_table_if_not_exists ('CREATE TABLE entry_group ( \n"
                            + "entries_id       SERIAL, \n"
                            + "groups_id        INTEGER        DEFAULT NULL, \n"
                            + "FOREIGN KEY (entries_id) REFERENCES entries (entries_id) ON DELETE CASCADE, \n"
                            + "FOREIGN KEY (groups_id)  REFERENCES groups (groups_id), \n"
                            + "PRIMARY KEY (groups_id, entries_id) \n" + ");')";
        default:
            return "";
        }
    }

    @Override
    public Connection connectAndEnsureDatabaseExists(DBStrings dbStrings)
            throws SQLException, IllegalAccessException, ClassNotFoundException, InstantiationException {

        // requires that the database is already there
        String url = SQLUtil.createJDBCurl(dbStrings, true);

        Connection conn = connect(url,
                dbStrings.getDbPreferences().getUsername(), dbStrings.getPassword());

        createPLPGSQLFunction(conn);

        return conn;
    }

    @Override
    public DatabaseType getType() {
        return DatabaseType.POSTGRESQL;
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

}
