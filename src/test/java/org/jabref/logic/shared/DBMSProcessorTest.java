package org.jabref.logic.shared;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.logic.shared.exception.InvalidDBMSConnectionPropertiesException;
import org.jabref.logic.shared.exception.OfflineLockException;
import org.jabref.model.database.shared.DBMSType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.testutils.category.DatabaseTest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@DatabaseTest
class DBMSProcessorTest {

    private static Stream<Object[]> getTestingDatabaseSystems() throws InvalidDBMSConnectionPropertiesException, SQLException {
        Collection<Object[]> result = new ArrayList<>();
        for (DBMSType dbmsType : TestManager.getDBMSTypeTestParameter()) {
            result.add(new Object[]{
                    dbmsType,
                    TestConnector.getTestDBMSConnection(dbmsType),
                    DBMSProcessor.getProcessorInstance(TestConnector.getTestDBMSConnection(dbmsType))});
        }
        return result.stream();
    }

    @ParameterizedTest
    @MethodSource("getTestingDatabaseSystems")
    void testCheckBaseIntegrity(DBMSType dbmsType, DBMSConnection dbmsConnection, DBMSProcessor dbmsProcessor) throws SQLException {
        dbmsProcessor.setupSharedDatabase();
        assertTrue(dbmsProcessor.checkBaseIntegrity());
        clear(dbmsConnection);
        assertFalse(dbmsProcessor.checkBaseIntegrity());
    }

    @ParameterizedTest
    @MethodSource("getTestingDatabaseSystems")
    void testSetUpSharedDatabase(DBMSType dbmsType, DBMSConnection dbmsConnection, DBMSProcessor dbmsProcessor) throws SQLException {
        dbmsProcessor.setupSharedDatabase();
        clear(dbmsConnection);
        dbmsProcessor.setupSharedDatabase();
        assertTrue(dbmsProcessor.checkBaseIntegrity());
    }

    @ParameterizedTest
    @MethodSource("getTestingDatabaseSystems")
    void testInsertEntry(DBMSType dbmsType, DBMSConnection dbmsConnection, DBMSProcessor dbmsProcessor) throws SQLException {
        dbmsProcessor.setupSharedDatabase();
        BibEntry expectedEntry = getBibEntryExample();

        dbmsProcessor.insertEntry(expectedEntry);

        BibEntry emptyEntry = new BibEntry();
        emptyEntry.getSharedBibEntryData().setSharedID(1);
        dbmsProcessor.insertEntry(emptyEntry); // does not insert, due to same sharedID.

        Map<String, String> actualFieldMap = new HashMap<>();

        try (ResultSet entryResultSet = selectFrom("ENTRY", dbmsConnection, dbmsProcessor)) {
            assertTrue(entryResultSet.next());
            assertEquals(1, entryResultSet.getInt("SHARED_ID"));
            assertEquals("inproceedings", entryResultSet.getString("TYPE"));
            assertEquals(1, entryResultSet.getInt("VERSION"));
            assertFalse(entryResultSet.next());

            try (ResultSet fieldResultSet = selectFrom("FIELD", dbmsConnection, dbmsProcessor)) {
                while (fieldResultSet.next()) {
                    actualFieldMap.put(fieldResultSet.getString("NAME"), fieldResultSet.getString("VALUE"));
                }
            }
        }

        Map<Field, String> expectedFieldMap = expectedEntry.getFieldMap();

        assertEquals(expectedFieldMap, actualFieldMap);
    }

    @ParameterizedTest
    @MethodSource("getTestingDatabaseSystems")
    void testUpdateEntry(DBMSType dbmsType, DBMSConnection dbmsConnection, DBMSProcessor dbmsProcessor) throws OfflineLockException, SQLException {
        dbmsProcessor.setupSharedDatabase();
        BibEntry expectedEntry = getBibEntryExample();

        dbmsProcessor.insertEntry(expectedEntry);

        expectedEntry.setType(StandardEntryType.Book);
        expectedEntry.setField(StandardField.AUTHOR, "Michael J and Hutchings");
        expectedEntry.setField(new UnknownField("customField"), "custom value");
        expectedEntry.clearField(StandardField.BOOKTITLE);

        dbmsProcessor.updateEntry(expectedEntry);

        Optional<BibEntry> actualEntryOptional = dbmsProcessor
                .getSharedEntry(expectedEntry.getSharedBibEntryData().getSharedID());

        assertEquals(expectedEntry, actualEntryOptional.get());
    }

    @ParameterizedTest
    @MethodSource("getTestingDatabaseSystems")
    void testGetEntriesByIdList(DBMSType dbmsType, DBMSConnection dbmsConnection, DBMSProcessor dbmsProcessor) throws OfflineLockException, SQLException {
        dbmsProcessor.setupSharedDatabase();
        BibEntry firstEntry = getBibEntryExample();
        firstEntry.setId("1");
        BibEntry secondEntry = getBibEntryExample();
        secondEntry.setId("2");

        dbmsProcessor.insertEntry(firstEntry);
        dbmsProcessor.insertEntry(secondEntry);

        List<BibEntry> sharedEntriesByIdList = dbmsProcessor.getSharedEntries(Arrays.asList(1, 2));

        assertEquals(firstEntry.getId(), sharedEntriesByIdList.get(0).getId());
        assertEquals(secondEntry.getId(), sharedEntriesByIdList.get(1).getId());
    }

    @ParameterizedTest
    @MethodSource("getTestingDatabaseSystems")
    void testUpdateNewerEntry(DBMSType dbmsType, DBMSConnection dbmsConnection, DBMSProcessor dbmsProcessor) throws OfflineLockException, SQLException {
        dbmsProcessor.setupSharedDatabase();
        BibEntry bibEntry = getBibEntryExample();

        dbmsProcessor.insertEntry(bibEntry);

        //simulate older version
        bibEntry.getSharedBibEntryData().setVersion(0);
        bibEntry.setField(StandardField.YEAR, "1993");

        assertThrows(OfflineLockException.class, () -> dbmsProcessor.updateEntry(bibEntry));
    }

    @ParameterizedTest
    @MethodSource("getTestingDatabaseSystems")
    void testUpdateEqualEntry(DBMSType dbmsType, DBMSConnection dbmsConnection, DBMSProcessor dbmsProcessor) throws OfflineLockException, SQLException {
        dbmsProcessor.setupSharedDatabase();
        BibEntry expectedBibEntry = getBibEntryExample();

        dbmsProcessor.insertEntry(expectedBibEntry);
        //simulate older version
        expectedBibEntry.getSharedBibEntryData().setVersion(0);
        dbmsProcessor.updateEntry(expectedBibEntry);

        Optional<BibEntry> actualBibEntryOptional = dbmsProcessor
                .getSharedEntry(expectedBibEntry.getSharedBibEntryData().getSharedID());

        assertEquals(expectedBibEntry, actualBibEntryOptional.get());
    }

    @ParameterizedTest
    @MethodSource("getTestingDatabaseSystems")
    void testRemoveEntry(DBMSType dbmsType, DBMSConnection dbmsConnection, DBMSProcessor dbmsProcessor) throws SQLException {
        dbmsProcessor.setupSharedDatabase();
        BibEntry bibEntry = getBibEntryExample();
        dbmsProcessor.insertEntry(bibEntry);
        dbmsProcessor.removeEntry(bibEntry);

        try (ResultSet resultSet = selectFrom("ENTRY", dbmsConnection, dbmsProcessor)) {
            assertFalse(resultSet.next());
        }
    }

    @ParameterizedTest
    @MethodSource("getTestingDatabaseSystems")
    void testGetSharedEntries(DBMSType dbmsType, DBMSConnection dbmsConnection, DBMSProcessor dbmsProcessor) throws SQLException {
        dbmsProcessor.setupSharedDatabase();
        BibEntry bibEntry = getBibEntryExampleWithEmptyFields();

        dbmsProcessor.insertEntry(bibEntry);

        List<BibEntry> expectedEntries = Arrays.asList(bibEntry);
        List<BibEntry> actualEntries = dbmsProcessor.getSharedEntries();

        assertEquals(expectedEntries, actualEntries);
    }

    @ParameterizedTest
    @MethodSource("getTestingDatabaseSystems")
    void testGetSharedEntry(DBMSType dbmsType, DBMSConnection dbmsConnection, DBMSProcessor dbmsProcessor) throws SQLException {
        dbmsProcessor.setupSharedDatabase();
        BibEntry expectedBibEntry = getBibEntryExampleWithEmptyFields();

        dbmsProcessor.insertEntry(expectedBibEntry);

        Optional<BibEntry> actualBibEntryOptional = dbmsProcessor
                .getSharedEntry(expectedBibEntry.getSharedBibEntryData().getSharedID());

        assertEquals(expectedBibEntry, actualBibEntryOptional.get());
    }

    @ParameterizedTest
    @MethodSource("getTestingDatabaseSystems")
    void testGetNotExistingSharedEntry(DBMSType dbmsType, DBMSConnection dbmsConnection, DBMSProcessor dbmsProcessor) throws SQLException {
        dbmsProcessor.setupSharedDatabase();
        Optional<BibEntry> actualBibEntryOptional = dbmsProcessor.getSharedEntry(1);
        assertFalse(actualBibEntryOptional.isPresent());
    }

    @ParameterizedTest
    @MethodSource("getTestingDatabaseSystems")
    void testGetSharedIDVersionMapping(DBMSType dbmsType, DBMSConnection dbmsConnection, DBMSProcessor dbmsProcessor) throws OfflineLockException, SQLException {
        dbmsProcessor.setupSharedDatabase();
        BibEntry firstEntry = getBibEntryExample();
        BibEntry secondEntry = getBibEntryExample();

        dbmsProcessor.insertEntry(firstEntry);
        dbmsProcessor.insertEntry(secondEntry);
        dbmsProcessor.updateEntry(secondEntry);

        Map<Integer, Integer> expectedIDVersionMap = new HashMap<>();
        expectedIDVersionMap.put(firstEntry.getSharedBibEntryData().getSharedID(), 1);
        expectedIDVersionMap.put(secondEntry.getSharedBibEntryData().getSharedID(), 2);

        Map<Integer, Integer> actualIDVersionMap = dbmsProcessor.getSharedIDVersionMapping();

        assertEquals(expectedIDVersionMap, actualIDVersionMap);
    }

    @ParameterizedTest
    @MethodSource("getTestingDatabaseSystems")
    void testGetSharedMetaData(DBMSType dbmsType, DBMSConnection dbmsConnection, DBMSProcessor dbmsProcessor) throws SQLException {
        dbmsProcessor.setupSharedDatabase();
        insertMetaData("databaseType", "bibtex;", dbmsConnection, dbmsProcessor);
        insertMetaData("protectedFlag", "true;", dbmsConnection, dbmsProcessor);
        insertMetaData("saveActions", "enabled;\nauthor[capitalize,html_to_latex]\ntitle[title_case]\n;", dbmsConnection, dbmsProcessor);
        insertMetaData("saveOrderConfig", "specified;author;false;title;false;year;true;", dbmsConnection, dbmsProcessor);

        Map<String, String> expectedMetaData = getMetaDataExample();
        Map<String, String> actualMetaData = dbmsProcessor.getSharedMetaData();

        assertEquals(expectedMetaData, actualMetaData);
    }

    @ParameterizedTest
    @MethodSource("getTestingDatabaseSystems")
    void testSetSharedMetaData(DBMSType dbmsType, DBMSConnection dbmsConnection, DBMSProcessor dbmsProcessor) throws SQLException {
        dbmsProcessor.setupSharedDatabase();
        Map<String, String> expectedMetaData = getMetaDataExample();
        dbmsProcessor.setSharedMetaData(expectedMetaData);

        Map<String, String> actualMetaData = dbmsProcessor.getSharedMetaData();

        assertEquals(expectedMetaData, actualMetaData);
    }

    private Map<String, String> getMetaDataExample() {
        Map<String, String> expectedMetaData = new HashMap<>();

        expectedMetaData.put("databaseType", "bibtex;");
        expectedMetaData.put("protectedFlag", "true;");
        expectedMetaData.put("saveActions", "enabled;\nauthor[capitalize,html_to_latex]\ntitle[title_case]\n;");
        expectedMetaData.put("saveOrderConfig", "specified;author;false;title;false;year;true;");

        return expectedMetaData;
    }

    private BibEntry getBibEntryExampleWithEmptyFields() {
        BibEntry bibEntry = new BibEntry();
        bibEntry.setField(StandardField.AUTHOR, "Author");
        bibEntry.setField(StandardField.TITLE, "");
        bibEntry.setField(StandardField.YEAR, "");
        bibEntry.getSharedBibEntryData().setSharedID(1);
        return bibEntry;
    }

    private BibEntry getBibEntryExample() {
        BibEntry bibEntry = new BibEntry(StandardEntryType.InProceedings);
        bibEntry.setField(StandardField.AUTHOR, "Wirthlin, Michael J and Hutchings, Brad L and Gilson, Kent L");
        bibEntry.setField(StandardField.TITLE, "The nano processor: a low resource reconfigurable processor");
        bibEntry.setField(StandardField.BOOKTITLE, "FPGAs for Custom Computing Machines, 1994. Proceedings. IEEE Workshop on");
        bibEntry.setField(StandardField.YEAR, "1994");
        bibEntry.setCiteKey("nanoproc1994");
        return bibEntry;
    }

    private ResultSet selectFrom(String table, DBMSConnection dbmsConnection, DBMSProcessor dbmsProcessor) {
        try {
            return dbmsConnection.getConnection().createStatement().executeQuery("SELECT * FROM " + escape(table, dbmsProcessor));
        } catch (SQLException e) {
            fail(e.getMessage());
            return null;
        }
    }

    // Oracle does not support multiple tuple insertion in one INSERT INTO command.
    // Therefore this function was defined to improve the readability and to keep the code short.
    private void insertMetaData(String key, String value, DBMSConnection dbmsConnection, DBMSProcessor dbmsProcessor) {
        try {
            dbmsConnection.getConnection().createStatement().executeUpdate("INSERT INTO " + escape("METADATA", dbmsProcessor) + "("
                    + escape("KEY", dbmsProcessor) + ", " + escape("VALUE", dbmsProcessor) + ") VALUES("
                    + escapeValue(key) + ", " + escapeValue(value) + ")");
        } catch (SQLException e) {
            fail(e.getMessage());
        }
    }

    private String escape(String expression, DBMSProcessor dbmsProcessor) {
        return dbmsProcessor.escape(expression);
    }

    private String escapeValue(String value) {
        return "'" + value + "'";
    }

    @AfterEach
    void clear(DBMSConnection dbmsConnection) throws SQLException {
        TestManager.clearTables(dbmsConnection);
    }
}
