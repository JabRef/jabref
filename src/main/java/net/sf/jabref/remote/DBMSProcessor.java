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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.sf.jabref.MetaData;
import net.sf.jabref.event.source.EntryEventSource;
import net.sf.jabref.logic.cleanup.FieldFormatterCleanup;
import net.sf.jabref.logic.exporter.FieldFormatterCleanups;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Processes all incoming or outgoing bib data to external SQL Database and manages its structure.
 */
public class DBMSProcessor {

    private static final Log LOGGER = LogFactory.getLog(DBMSConnector.class);

    private DBMSType dbType;
    private final DBMSHelper dbHelper;

    public static final String ENTRY = "ENTRY";
    public static final String METADATA = "METADATA";

    public static final List<String> ALL_TABLES = new ArrayList<>(Arrays.asList(ENTRY, METADATA));

    // Elected column names of main the table
    // This entries are needed to ease the changeability, cause some database systems dependent on the context expect low or uppercase characters.
    public static final String ENTRY_REMOTE_ID = "REMOTE_ID";
    public static final String ENTRY_ENTRYTYPE = "ENTRYTYPE";

    public static final String METADATA_SORT_ID = "SORT_ID";
    public static final String METADATA_KEY = "META_KEY";
    public static final String METADATA_FIELD = "FIELD";
    public static final String METADATA_VALUE = "META_VALUE";


    /**
     * @param connection Working SQL connection
     * @param dbType Instance of {@link DBMSType}
     */
    public DBMSProcessor(DBMSHelper dbmsHelper, DBMSType dbType) {
        this.dbType = dbType;
        this.dbHelper = dbmsHelper;
    }

    /**
     * Scans the database for required tables.
     * @return <code>true</code> if the structure matches the requirements, <code>false</code> if not.
     */
    public boolean checkBaseIntegrity() {
        List<String> requiredTables = new ArrayList<>(ALL_TABLES);
        try {
            DatabaseMetaData databaseMetaData = dbHelper.getMetaData();

            // ...getTables(null, ...): no restrictions
            try (ResultSet databaseMetaDataResultSet = databaseMetaData.getTables(null, null, null, null)) {

                while (databaseMetaDataResultSet.next()) {
                    String tableName = databaseMetaDataResultSet.getString("TABLE_NAME").toUpperCase();
                    requiredTables.remove(tableName); // Remove matching tables to check requiredTables for emptiness
                }

                return requiredTables.size() == 0;
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
        if (dbType == DBMSType.MYSQL) {
            dbHelper.executeUpdate("CREATE TABLE IF NOT EXISTS " + ENTRY + " ("
                    + ENTRY_REMOTE_ID + " INT(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,"
                    + ENTRY_ENTRYTYPE + " VARCHAR(255) DEFAULT NULL"
                    + ") ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=latin1;");
            dbHelper.executeUpdate("CREATE TABLE IF NOT EXISTS " + METADATA + " ("
                    + METADATA_SORT_ID + " int(11) NOT NULL,"
                    + METADATA_KEY + " varchar(255) NOT NULL,"
                    + METADATA_FIELD + " varchar(255) DEFAULT NULL,"
                    + METADATA_VALUE + " text NOT NULL,"
                    + "UNIQUE(" + METADATA_SORT_ID + ")"
                    + ") ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=latin1;");
        } else if (dbType == DBMSType.POSTGRESQL) {
            dbHelper.executeUpdate("CREATE TABLE IF NOT EXISTS " + ENTRY + " ("
                    + ENTRY_REMOTE_ID + " SERIAL PRIMARY KEY,"
                    + ENTRY_ENTRYTYPE + " VARCHAR);");
            dbHelper.executeUpdate("CREATE TABLE IF NOT EXISTS " + METADATA + " ("
                    + METADATA_SORT_ID + " INT UNIQUE,"
                    + METADATA_KEY + " VARCHAR,"
                    + METADATA_FIELD + " VARCHAR,"
                    + METADATA_VALUE + " TEXT);");
        } else if (dbType == DBMSType.ORACLE) {
            dbHelper.executeUpdate("CREATE TABLE \"" + ENTRY + "\" (" + "\""
                    + ENTRY_REMOTE_ID + "\"  NUMBER NOT NULL," + "\""
                    + ENTRY_ENTRYTYPE + "\"  VARCHAR2(255) NULL,"
                    + "CONSTRAINT  \"" + ENTRY + "_PK\" PRIMARY KEY (\"" + ENTRY_REMOTE_ID + "\"))");
            dbHelper.executeUpdate("CREATE SEQUENCE \"" + ENTRY + "_SEQ\"");
            dbHelper.executeUpdate("CREATE TRIGGER \"BI_" + ENTRY + "\" BEFORE INSERT ON \"" + ENTRY + "\" "
                    + "FOR EACH ROW BEGIN " + "SELECT \"" + ENTRY + "_SEQ\".NEXTVAL INTO :NEW."
                    + ENTRY_REMOTE_ID.toLowerCase(Locale.ENGLISH) + " FROM DUAL; " + "END;");
            dbHelper.executeUpdate("CREATE TABLE \"" + METADATA + "\" (" + "\""
                    + METADATA_SORT_ID + "\"  NUMBER NOT NULL," + "\""
                    + METADATA_KEY + "\"  VARCHAR2(255) NULL," + "\""
                    + METADATA_FIELD + "\"  VARCHAR2(255) NULL," + "\""
                    + METADATA_VALUE + "\"  CLOB NOT NULL,"
                    + "CONSTRAINT  \"" + METADATA + "_UQ\" UNIQUE (\"" + METADATA_SORT_ID + "\"))");
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
            try (ResultSet resultSet = dbHelper.query(
                    "SELECT * FROM " + escape(ENTRY) + " WHERE " + escape(ENTRY_REMOTE_ID) + " = " + remote_id)) {
                if (resultSet.next()) {
                    return;
                }
            } catch (SQLException e) {
                LOGGER.error("SQL Error: ", e);
            }
        }


        String query = "INSERT INTO " + escape(ENTRY) + "(";
        ArrayList<String> fieldNames = new ArrayList<>(bibEntry.getFieldNames());

        for (int i = 0; i < fieldNames.size(); i++) {
            query = query + escape(fieldNames.get(i).toUpperCase()) + ", ";
        }
        query = query + escape(ENTRY_ENTRYTYPE) + ") VALUES(";
        for (int i = 0; i < fieldNames.size(); i++) {
            query = query + escapeValue(bibEntry.getField(fieldNames.get(i))) + ", ";
        }
        query = query + escapeValue(bibEntry.getType()) + ")";

        try (PreparedStatement preparedStatement = dbHelper.prepareStatement(query,
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

        LOGGER.info("SQL INSERT: " + query);
    }

    /**
     * Updates the whole bibEntry remotely.
     *
     * @param bibEntry {@link BibEntry} affected by changes
     */
    public void updateEntry(BibEntry bibEntry) {
        prepareEntryTableStructure(bibEntry);

        String query = "UPDATE " + escape(ENTRY) + " SET " + escape(ENTRY_ENTRYTYPE) + " = "
                + escapeValue(bibEntry.getType());

        Set<String> fields = bibEntry.getFieldNames();
        Set<String> emptyFields = getRemoteEntry(bibEntry.getRemoteId()).getFieldNames();
        emptyFields.removeAll(fields); // emptyFields now contains only fields which should be null.

        for (String emptyField : emptyFields) {
            query = query + ", " + escape(emptyField.toUpperCase(Locale.ENGLISH)) + " = NULL";
        }

        for (String field : fields) {
            query = query + ", " + escape(field.toUpperCase(Locale.ENGLISH)) + " = "
                    + escapeValue(bibEntry.getField(field));
        }

        query = query + " WHERE " + escape(ENTRY_REMOTE_ID) + " = " + bibEntry.getRemoteId();
        dbHelper.executeUpdate(query);
        LOGGER.info("SQL UPDATE: " + query);
    }

    /**
     * Removes the remote existing bibEntry
     * @param bibEntry {@link BibEntry} to be deleted
     */
    public void removeEntry(BibEntry bibEntry) {
        String query = "DELETE FROM " + escape(ENTRY) + " WHERE " + escape(ENTRY_REMOTE_ID) + " = "
                + bibEntry.getRemoteId();
        dbHelper.executeUpdate(query);
        LOGGER.info("SQL DELETE: " + query);
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
        Set<String> fieldNames = dbHelper.allToUpperCase(bibEntry.getFieldNames());
        fieldNames.removeAll(dbHelper.allToUpperCase(dbHelper.getColumnNames(escape(ENTRY))));

        String columnType = dbType == DBMSType.ORACLE ? " CLOB NULL" : " TEXT NULL DEFAULT NULL";

        for (String fieldName : fieldNames) {
            dbHelper.executeUpdate("ALTER TABLE " + escape(ENTRY) + " ADD " + escape(fieldName) + columnType);
        }
    }

    /**
     *  Deletes all unused columns where every entry has a value NULL.
     */
    public void normalizeEntryTable() {
        ArrayList<String> columnsToRemove = new ArrayList<>();

        columnsToRemove.addAll(dbHelper.allToUpperCase(dbHelper.getColumnNames(escape(ENTRY))));
        columnsToRemove.remove(ENTRY_REMOTE_ID); // essential column
        columnsToRemove.remove(ENTRY_ENTRYTYPE); // essential column

        try (ResultSet resultSet = dbHelper.query("SELECT * FROM " + escape(ENTRY))) {
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
    public BibEntry getRemoteEntry(int remoteId) {
        List<BibEntry> entries = getRemoteEntries(remoteId);
        if (entries.size() > 0) {
            return entries.get(0);
        }
        return null;
    }

    /**
     * @param remoteId Entry ID. If 0, all entries are going to be fetched.
     * @return List of {@link BibEntry} instances
     */
    private List<BibEntry> getRemoteEntries(int remoteId) {
        List<BibEntry> remoteEntries = new ArrayList<>();
        try (ResultSet resultSet = dbHelper.query("SELECT * FROM " + escape(ENTRY)
                + (remoteId != 0 ? " WHERE " + ENTRY_REMOTE_ID + " = " + remoteId : ""))) {
            Set<String> columns = dbHelper.allToUpperCase(dbHelper.getColumnNames(escape(ENTRY)));

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
     * Fetches all remotely present meta data.
     */
    public Map<String, List<String>> getRemoteMetaData() {
        Map<String, List<String>> metaData = new HashMap<>();
        String query = "SELECT * FROM " + escape(METADATA) + " ORDER BY " + escape(METADATA_SORT_ID);

        try (ResultSet resultSet = dbHelper.query(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)) {
            String metaKey = "";
            String field = "";
            List<String> orderedData = new ArrayList<>();

            while (resultSet.next()) {
                if (!metaKey.equals(resultSet.getString(METADATA_KEY))) {
                    if (!orderedData.isEmpty()) {
                        metaData.put(metaKey, new ArrayList<>(orderedData));
                    }
                    orderedData.clear();
                    metaKey = resultSet.getString(METADATA_KEY);
                    field = "";
                }

                if (metaKey.equals(MetaData.SAVE_ACTIONS)) {
                    if (resultSet.getString(METADATA_FIELD) == null) {
                        orderedData.add(resultSet.getString(METADATA_VALUE));
                    } else {
                        if (field.isEmpty()) {
                            orderedData.add(resultSet.getString(METADATA_FIELD) + "["
                                    + resultSet.getString(METADATA_VALUE) + "]");
                        } else if (!field.equals(resultSet.getString(METADATA_FIELD))) {
                            String value = orderedData.remove(orderedData.size() - 1);
                            value = value + "\n" + resultSet.getString(METADATA_FIELD) + "[" + resultSet.getString(METADATA_VALUE) + "]";
                            orderedData.add(value);
                        } else {
                            String value = orderedData.remove(orderedData.size() - 1);
                            value = value.substring(0, value.lastIndexOf(']')) + ",";
                            value = value + resultSet.getString(METADATA_VALUE) + "]";
                            orderedData.add(value);
                        }
                        field = resultSet.getString(METADATA_FIELD);
                    }
                } else if (metaKey.equals(MetaData.SAVE_ORDER_CONFIG)) {
                    if (resultSet.getString(METADATA_FIELD) == null) {
                        orderedData.add(resultSet.getString(METADATA_VALUE));
                    } else {
                        orderedData.add(resultSet.getString(METADATA_FIELD));
                        orderedData.add(resultSet.getString(METADATA_VALUE));
                    }
                } else {
                    orderedData.add(resultSet.getString(METADATA_VALUE));
                }

                if (resultSet.isLast()) {
                    metaData.put(metaKey, new ArrayList<>(orderedData));
                }
            }
        } catch (SQLException e) {
            LOGGER.error("SQL Error", e);
        }
        return metaData;
    }

    /**
     * Clears and sets all meta data remotely.
     * @param metaData JabRef meta data.
     */
    public void setRemoteMetaData(Map<String, List<String>> metaData) {
        dbHelper.clearTables(METADATA);

        for (String metaKey : metaData.keySet()) {
            List<String> values = metaData.get(metaKey);

            if (metaKey.equals(MetaData.SAVE_ACTIONS)) {
                insertMetaData(METADATA, METADATA_KEY, metaKey, METADATA_VALUE, values.get(0));
                for (FieldFormatterCleanup cleanUp : FieldFormatterCleanups.parse(values.get(1))) {
                    insertMetaData(METADATA, METADATA_KEY, metaKey, METADATA_FIELD, cleanUp.getField(),
                            METADATA_VALUE, cleanUp.getFormatter().getKey());
                }
            } else if (metaKey.equals(MetaData.SAVE_ORDER_CONFIG)) {
                insertMetaData(METADATA, METADATA_KEY, metaKey, METADATA_VALUE, values.get(0));

                for (int i = 1; i < values.size(); i+=2) {
                    insertMetaData(METADATA, METADATA_KEY, metaKey, METADATA_FIELD, values.get(i), METADATA_VALUE, values.get(i+1));
                }
            } else {
                insertMetaData(METADATA, METADATA_KEY, metaKey, METADATA_VALUE, values.get(0));
            }
        }
    }

    /**
     * Inserts the given data into database.
     * @param table Relational table the data should be inserted in
     * @param columnValueMapping Mapping between columns and values in form of an usual array
     * Call example: <code>insert("table", "column1", "value1", "column2", "value2");</code>
     */
    private void insertMetaData(String table, Object... columnValueMapping) {
        int sortId = 1;

        // To unify all three systems it was necessary to work around with the following code
        // Only reseting a sequence in Oracle takes 20-30 LOC (!)
        try (ResultSet resultSet = dbHelper.query("SELECT MAX(" + escape(METADATA_SORT_ID) + ") FROM " + escape(METADATA))) {
            if (resultSet.next()) {
                sortId = resultSet.getInt(1) + 1;
            }
        } catch (SQLException e) {
            LOGGER.error("SQL Error", e);
        }

        String query = "INSERT INTO " + escape(table) + "(" + escape(METADATA_SORT_ID) + ", ";
        for (int i = 0; i < columnValueMapping.length; i += 2) { // Prepare columns
            query = query + escape(String.valueOf(columnValueMapping[i]));
            query = i < (columnValueMapping.length - 2) ? query + ", " : query;
        }
        query = query + ") VALUES(" + escapeValue(sortId) + ", ";
        for (int i = 1; i < columnValueMapping.length; i += 2) { // Prepare values
            query = query + escapeValue(columnValueMapping[i]);
            query = i < (columnValueMapping.length - 2) ? query + ", " : query;
        }
        query = query + ")";
        dbHelper.executeUpdate(query);
    }

    /**
     * Drops the given columns.
     * @param columnsToRemove
     */
    private void dropColumns(List<String> columnsToRemove) {
        String columnExpression = "";
        String expressionPrefix = "";
        if ((dbType == dbType.MYSQL) || (dbType == dbType.POSTGRESQL)) {
            expressionPrefix = "DROP ";
        }

        for (int i = 0; i < columnsToRemove.size(); i++) {
            String column = columnsToRemove.get(i);
            columnExpression = columnExpression + expressionPrefix + escape(column);
            columnExpression = i < (columnsToRemove.size() - 1) ? columnExpression + ", " : columnExpression;
        }

        if (dbType == dbType.ORACLE) {
            columnExpression = "DROP (" + columnExpression + ")"; // DROP command in Oracle differs from the other systems.
        }

        if (columnsToRemove.size() > 0) {
            dbHelper.executeUpdate("ALTER TABLE " + escape(ENTRY) + " " + columnExpression);
        }
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
     * of the database system using the current dbType.
     * @param expression Table or field name
     * @return Correctly escape expression
     */
    public String escape(String expression) {
        return escape(expression, dbType);
    }

    /**
     * Escapes the value indication of SQL expressions.
     *
     * @param Value to be escaped
     * @return Correctly escaped expression or "NULL" if <code>value</code> is real <code>null</code> object.
     */
    public static String escapeValue(Object obj) {
        String stringValue;
        if (obj == null) {
            stringValue = "NULL";
        } else {
            if (obj instanceof String) {
                stringValue = "'" + obj + "'";
            } else {
                stringValue = String.valueOf(obj);
            }
        }
        return stringValue;
    }

    public void setDBType(DBMSType dbType) {
        this.dbType = dbType;
    }

    public DBMSType getDBType() {
        return this.dbType;
    }

}
