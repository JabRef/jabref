/*  Copyright (C) 2003-2016 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.remote;

import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import net.sf.jabref.event.source.EntryEventSource;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Processes all incoming or outgoing bib data to external SQL Database and manages its structure.
 */
public class DBMSProcessor {

    private static final Log LOGGER = LogFactory.getLog(DBMSConnector.class);

    private DBMSType dbmsType;
    private final DBMSHelper dbmsHelper;

    public static final String ENTRY = "ENTRY";
    public static final String METADATA = "METADATA";

    public static final List<String> ALL_TABLES = new ArrayList<>(Arrays.asList(ENTRY, METADATA));

    // Elected column names of main the table
    // This entries are needed to ease the changeability, cause some database systems dependent on the context expect low or uppercase characters.
    public static final String ENTRY_REMOTE_ID = "REMOTE_ID";
    public static final String ENTRY_ENTRYTYPE = "ENTRYTYPE";

    public static final String METADATA_KEY = "META_KEY";
    public static final String METADATA_VALUE = "META_VALUE";


    /**
     * @param connection Working SQL connection
     * @param dbmsType Instance of {@link DBMSType}
     */
    public DBMSProcessor(DBMSHelper dbmsHelper, DBMSType dbmsType) {
        this.dbmsType = dbmsType;
        this.dbmsHelper = dbmsHelper;
    }

    /**
     * Scans the database for required tables.
     * @return <code>true</code> if the structure matches the requirements, <code>false</code> if not.
     */
    public boolean checkBaseIntegrity() {
        List<String> requiredTables = new ArrayList<>(ALL_TABLES);
        try {
            DatabaseMetaData databaseMetaData = dbmsHelper.getMetaData();

            // ...getTables(null, ...): no restrictions
            try (ResultSet databaseMetaDataResultSet = databaseMetaData.getTables(null, null, null, null)) {

                while (databaseMetaDataResultSet.next()) {
                    String tableName = databaseMetaDataResultSet.getString("TABLE_NAME").toUpperCase();
                    requiredTables.remove(tableName); // Remove matching tables to check requiredTables for emptiness
                }

                return requiredTables.isEmpty();
            }
        } catch (SQLException e) {
            LOGGER.error("SQL Error: ", e);
        }
        return false;
    }

    /**
     * Creates and sets up the needed tables and columns according to the database type.
     */
    public void setUpRemoteDatabase() {
        if (dbmsType == DBMSType.MYSQL) {
            dbmsHelper.executeUpdate("CREATE TABLE IF NOT EXISTS " + ENTRY + " ("
                    + ENTRY_REMOTE_ID + " INT(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,"
                    + ENTRY_ENTRYTYPE + " VARCHAR(255) DEFAULT NULL)");
            dbmsHelper.executeUpdate("CREATE TABLE IF NOT EXISTS " + METADATA + " ("
                    + METADATA_KEY + " varchar(255) NOT NULL,"
                    + METADATA_VALUE + " text NOT NULL)");
        } else if (dbmsType == DBMSType.POSTGRESQL) {
            dbmsHelper.executeUpdate("CREATE TABLE IF NOT EXISTS " + ENTRY + " ("
                    + ENTRY_REMOTE_ID + " SERIAL PRIMARY KEY,"
                    + ENTRY_ENTRYTYPE + " VARCHAR);");
            dbmsHelper.executeUpdate("CREATE TABLE IF NOT EXISTS " + METADATA + " ("
                    + METADATA_KEY + " VARCHAR,"
                    + METADATA_VALUE + " TEXT);");
        } else if (dbmsType == DBMSType.ORACLE) {
            dbmsHelper.executeUpdate("CREATE TABLE \"" + ENTRY + "\" (" + "\""
                    + ENTRY_REMOTE_ID + "\"  NUMBER NOT NULL," + "\""
                    + ENTRY_ENTRYTYPE + "\"  VARCHAR2(255) NULL,"
                    + "CONSTRAINT  \"" + ENTRY + "_PK\" PRIMARY KEY (\"" + ENTRY_REMOTE_ID + "\"))");
            dbmsHelper.executeUpdate("CREATE SEQUENCE \"" + ENTRY + "_SEQ\"");
            dbmsHelper.executeUpdate("CREATE TRIGGER \"BI_" + ENTRY + "\" BEFORE INSERT ON \"" + ENTRY + "\" "
                    + "FOR EACH ROW BEGIN " + "SELECT \"" + ENTRY + "_SEQ\".NEXTVAL INTO :NEW."
                    + ENTRY_REMOTE_ID.toLowerCase(Locale.ENGLISH) + " FROM DUAL; " + "END;");
            dbmsHelper.executeUpdate("CREATE TABLE \"" + METADATA + "\" (" + "\""
                    + METADATA_KEY + "\"  VARCHAR2(255) NULL," + "\""
                    + METADATA_VALUE + "\"  CLOB NOT NULL)");
        }
        if (!checkBaseIntegrity()) {
            // can only happen with users direct intervention in remote database
            LOGGER.error(Localization.lang("Corrupt_remote_database_structure."));
        }
    }

    /**
     * Inserts the given bibEntry into remote database.
     * @param bibEntry {@link BibEntry} to be inserted
     */
    public void insertEntry(BibEntry bibEntry) {
        prepareEntryTableStructure(bibEntry);

        // Check if already exists
        int remote_id = bibEntry.getRemoteId();
        if (remote_id != -1) {
            try (ResultSet resultSet = selectFromEntryTable(remote_id)) {
                if (resultSet.next()) {
                    return;
                }
            } catch (SQLException e) {
                LOGGER.error("SQL Error: ", e);
            }
        }

        StringBuilder query = new StringBuilder();
        query.append("INSERT INTO ");
        query.append(escape(ENTRY));
        query.append("(");

        List<String> fieldNames = new ArrayList<>(bibEntry.getFieldNames());

        for (String fieldName : fieldNames) {
            query.append(escape(fieldName.toUpperCase(Locale.ENGLISH)));
            query.append(", ");
        }

        query.append(escape(ENTRY_ENTRYTYPE));
        query.append(") VALUES(");

        for (String fieldName : fieldNames) {
            query.append(escapeValue(bibEntry.getFieldOptional(fieldName)));
            query.append(", ");
        }

        query.append(escapeValue(bibEntry.getType()));
        query.append(")");

        try (PreparedStatement preparedStatement = dbmsHelper.prepareStatement(query.toString(),
                ENTRY_REMOTE_ID.toLowerCase(Locale.ENGLISH))) { // This is the only method to get generated keys which is accepted by MySQL, PostgreSQL and Oracle.
            preparedStatement.executeUpdate();
            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    bibEntry.setRemoteId(generatedKeys.getInt(1)); // set generated ID locally
                }
                preparedStatement.close();
                generatedKeys.close();
            }
        } catch (SQLException e) {
            LOGGER.error("SQL Error: ", e);
        }
    }

    /**
     * Updates the whole bibEntry remotely.
     *
     * @param bibEntry {@link BibEntry} affected by changes
     */
    public void updateEntry(BibEntry bibEntry) {
        prepareEntryTableStructure(bibEntry);

        StringBuilder query = new StringBuilder();

        query.append("UPDATE ");
        query.append(escape(ENTRY));
        query.append(" SET ");
        query.append(escape(ENTRY_ENTRYTYPE));
        query.append(" = ");
        query.append(escapeValue(bibEntry.getType()));

        Optional<BibEntry> remoteBibEntry = getRemoteEntry(bibEntry.getRemoteId());
        Set<String> fields = bibEntry.getFieldNames();
        Set<String> emptyFields = remoteBibEntry.isPresent() ? remoteBibEntry.get().getFieldNames() : new HashSet<>();
        emptyFields.removeAll(fields); // emptyFields now contains only fields which should be null.

        for (String emptyField : emptyFields) {
            query.append(", ");
            query.append(escape(emptyField.toUpperCase(Locale.ENGLISH)));
            query.append(" = NULL");
        }

        for (String field : fields) {
            query.append(", ");
            query.append(escape(field.toUpperCase(Locale.ENGLISH)));
            query.append(" = ");
            query.append(escapeValue(bibEntry.getFieldOptional(field)));
        }

        query.append(" WHERE ");
        query.append(escape(ENTRY_REMOTE_ID));
        query.append(" = ");
        query.append(bibEntry.getRemoteId());
        dbmsHelper.executeUpdate(query.toString());
    }

    /**
     * Removes the remote existing bibEntry
     * @param bibEntry {@link BibEntry} to be deleted
     */
    public void removeEntry(BibEntry bibEntry) {
        String query = "DELETE FROM " + escape(ENTRY) + " WHERE " + escape(ENTRY_REMOTE_ID) + " = "
                + bibEntry.getRemoteId();
        dbmsHelper.executeUpdate(query);
        normalizeEntryTable();
    }

    /**
     *  Prepares the database table for a new {@link BibEntry}.
     *  Learning table structure: Columns which are not available are going to be created.
     *
     *  @param bibEntry Entry which pretends missing columns which should be created.
     *
     */
    public void prepareEntryTableStructure(BibEntry bibEntry) {
        Set<String> fieldNames = dbmsHelper.allToUpperCase(bibEntry.getFieldNames());
        fieldNames.removeAll(dbmsHelper.allToUpperCase(dbmsHelper.getColumnNames(escape(ENTRY))));

        String columnType = dbmsType == DBMSType.ORACLE ? " CLOB NULL" : " TEXT NULL DEFAULT NULL";

        for (String fieldName : fieldNames) {
            dbmsHelper.executeUpdate("ALTER TABLE " + escape(ENTRY) + " ADD " + escape(fieldName) + columnType);
        }
    }

    /**
     *  Deletes all unused columns where every entry has a value NULL.
     */
    public void normalizeEntryTable() {
        ArrayList<String> columnsToRemove = new ArrayList<>();

        columnsToRemove.addAll(dbmsHelper.allToUpperCase(dbmsHelper.getColumnNames(escape(ENTRY))));
        columnsToRemove.remove(ENTRY_REMOTE_ID); // essential column
        columnsToRemove.remove(ENTRY_ENTRYTYPE); // essential column

        try (ResultSet resultSet = selectFromEntryTable()) {
            while (resultSet.next()) {
                for (int i = 0; i < columnsToRemove.size(); i++) {
                    if (resultSet.getObject(columnsToRemove.get(i)) != null) {
                        columnsToRemove.remove(i);
                        i--; // due to index shift
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.error("SQL Error: ", e);
        }

        dropColumns(columnsToRemove);
    }

    /**
     * Converts all remotely present bib entries to the List of real {@link BibEntry} objects and retrieves them.
     */
    public List<BibEntry> getRemoteEntries() {
        return getRemoteEntries(0);
    }

    /**
     * @param remoteId Entry ID
     * @return instance of {@link BibEntry}
     */
    public Optional<BibEntry> getRemoteEntry(int remoteId) {
        List<BibEntry> entries = getRemoteEntries(remoteId);
        return entries.isEmpty() ? Optional.empty() : Optional.of(entries.get(0));
    }

    /**
     * @param remoteId Entry ID. If 0, all entries are going to be fetched.
     * @return List of {@link BibEntry} instances
     */
    private List<BibEntry> getRemoteEntries(int remoteId) {
        List<BibEntry> remoteEntries = new ArrayList<>();
        try (ResultSet resultSet = remoteId == 0 ? selectFromEntryTable() : selectFromEntryTable(remoteId)) {
            Set<String> columns = dbmsHelper.allToUpperCase(dbmsHelper.getColumnNames(escape(ENTRY)));

            while (resultSet.next()) {
                BibEntry bibEntry = new BibEntry();
                for (String column : columns) {
                    if (column.equals(ENTRY_REMOTE_ID)) { // distinguish, because special methods in BibEntry has to be used in this case
                        bibEntry.setRemoteId(resultSet.getInt(column));
                    } else if (column.equals(ENTRY_ENTRYTYPE)) {
                        bibEntry.setType(resultSet.getString(column));
                    } else {
                        String value = resultSet.getString(column);
                        if (value != null) {
                            bibEntry.setField(column.toLowerCase(Locale.ENGLISH), value, EntryEventSource.REMOTE);
                        }
                    }
                }
                remoteEntries.add(bibEntry);
            }
        } catch (SQLException e) {
            LOGGER.error("SQL Error", e);
        }
        return remoteEntries;
    }

    /**
     * Fetches and returns all remotely present meta data.
     */
    public Map<String, String> getRemoteMetaData() {
        Map<String, String> data = new HashMap<>();

        try (ResultSet resultSet = selectFromMetaDataTable()) {
            while(resultSet.next()) {
                data.put(resultSet.getString(METADATA_KEY), resultSet.getString(METADATA_VALUE));
            }
        } catch (SQLException e) {
            LOGGER.error("SQL Error", e);
        }

        return data;
    }

    /**
     * Clears and sets all meta data remotely.
     * @param metaData JabRef meta data.
     */
    public void setRemoteMetaData(Map<String, String> data) {
        dbmsHelper.clearTables(METADATA);

        for (Map.Entry<String, String> metaEntry : data.entrySet()) {
            dbmsHelper.executeUpdate("INSERT INTO " + escape(METADATA) + "(" +
                    escape(METADATA_KEY) + ", " + escape(METADATA_VALUE) + ") VALUES(" +
                    escapeValue(metaEntry.getKey()) + ", " + escapeValue(metaEntry.getValue()) + ")");
        }
    }

    /**
     * Drops the given columns.
     * @param columnsToRemove
     */
    private void dropColumns(List<String> columnsToRemove) {
        String columnExpression = "";
        String expressionPrefix = "";
        if ((dbmsType == dbmsType.MYSQL) || (dbmsType == dbmsType.POSTGRESQL)) {
            expressionPrefix = "DROP ";
        }

        for (int i = 0; i < columnsToRemove.size(); i++) {
            String column = columnsToRemove.get(i);
            columnExpression = columnExpression + expressionPrefix + escape(column);
            columnExpression = i < (columnsToRemove.size() - 1) ? columnExpression + ", " : columnExpression;
        }

        if (dbmsType == dbmsType.ORACLE) {
            columnExpression = "DROP (" + columnExpression + ")"; // DROP command in Oracle differs from the other systems.
        }

        if (columnsToRemove.size() > 0) {
            dbmsHelper.executeUpdate("ALTER TABLE " + escape(ENTRY) + " " + columnExpression);
        }
    }

    /**
     * Helping method for SQL selection retrieving a {@link ResultSet}
     */
    private ResultSet selectFromEntryTable() throws SQLException {
        return dbmsHelper.query("SELECT * FROM " + escape(ENTRY));
    }

    /**
     * Helping method for SQL selection retrieving a {@link ResultSet}
     * @param id remoteId of {@link BibEntry}
     */
    private ResultSet selectFromEntryTable(int id) throws SQLException {
        return dbmsHelper.query("SELECT * FROM " + escape(ENTRY) + " WHERE " + escape(ENTRY_REMOTE_ID) + " = " + id);
    }

    /**
     * Helping method for SQL selection retrieving a {@link ResultSet}
     */
    private ResultSet selectFromMetaDataTable() throws SQLException {
        return dbmsHelper.query("SELECT * FROM " + escape(METADATA));
    }

    /**
     * Escapes parts of SQL expressions like table or field name to match the conventions
     * of the database system.
     * @param expression Table or field name
     * @param type Type of database system
     * @return Correctly escape expression
     */
    public static String escape(String expression, DBMSType type) {
        if (type == DBMSType.ORACLE) {
            return "\"" + expression + "\"";
        } else if (type == DBMSType.MYSQL) {
            return "`" + expression + "`";
        }
        return expression;
    }

    /**
     * Escapes parts of SQL expressions like table or field name to match the conventions
     * of the database system using the current dbmsType.
     * @param expression Table or field name
     * @return Correctly escape expression
     */
    public String escape(String expression) {
        return escape(expression, dbmsType);
    }

    /**
     * Escapes the value indication of SQL expressions.
     *
     * @param Value to be escaped
     * @return Correctly escaped expression or "NULL" if no value is present.
     */
    public static String escapeValue(String value) {
        return escapeValue(Optional.ofNullable(value));
    }

    /**
     * Escapes the value indication of SQL expressions.
     *
     * @param Value to be escaped
     * @return Correctly escaped expression or "NULL" if no value is present.
     */
    public static String escapeValue(Optional<String> value) {
        if (value.isPresent()) {
            return "'" + value.get() + "'";
        }
        return "NULL";
    }

    public void setDBType(DBMSType dbmsType) {
        this.dbmsType = dbmsType;
    }

    public DBMSType getDBType() {
        return this.dbmsType;
    }

}
