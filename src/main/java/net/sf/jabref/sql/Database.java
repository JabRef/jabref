package net.sf.jabref.sql;

import java.sql.Connection;
import java.sql.SQLException;

public interface Database {

    Connection connect(String url, String username, String password)
            throws IllegalAccessException, InstantiationException, ClassNotFoundException, SQLException;

    String getReadColumnNamesQuery();

    enum Table {
        JABREF_DATABASE, ENTRY_TYPES, ENTRIES, STRINGS, GROUP_TYPES, GROUPS, ENTRY_GROUP
    }

    String getCreateTableSQL(Table table);

    Connection connectAndEnsureDatabaseExists(DBStrings dbStrings) throws SQLException, IllegalAccessException, ClassNotFoundException, InstantiationException;

    DatabaseType getType();

}
