package org.jabref.logic.shared;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.logic.shared.exception.OfflineLockException;
import org.jabref.model.database.shared.DBMSType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.testutils.category.DatabaseTest;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@DatabaseTest
class DBMSProcessorTest {

    @ParameterizedTest
    @MethodSource("org.jabref.logic.shared.TestManager#getTestingDatabaseSystems")
    void testCheckBaseIntegrity(DBMSType dbmsType, DBMSConnection dbmsConnection, DBMSProcessor dbmsProcessor) throws SQLException {
        dbmsProcessor.setupSharedDatabase();
        assertTrue(dbmsProcessor.checkBaseIntegrity());
        clear(dbmsConnection);
        assertFalse(dbmsProcessor.checkBaseIntegrity());

        clear(dbmsConnection);
    }

    @ParameterizedTest
    @MethodSource("org.jabref.logic.shared.TestManager#getTestingDatabaseSystems")
    void testSetUpSharedDatabase(DBMSType dbmsType, DBMSConnection dbmsConnection, DBMSProcessor dbmsProcessor) throws SQLException {
        dbmsProcessor.setupSharedDatabase();
        clear(dbmsConnection);
        dbmsProcessor.setupSharedDatabase();
        assertTrue(dbmsProcessor.checkBaseIntegrity());

        clear(dbmsConnection);
    }

    @ParameterizedTest
    @MethodSource("org.jabref.logic.shared.TestManager#getTestingDatabaseSystems")
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

        Map<String, String> expectedFieldMap = expectedEntry.getFieldMap().entrySet().stream().collect(Collectors.toMap((entry) -> entry.getKey().getName(), Map.Entry::getValue));

        assertEquals(expectedFieldMap, actualFieldMap);
        clear(dbmsConnection);
    }

    @ParameterizedTest
    @MethodSource("org.jabref.logic.shared.TestManager#getTestingDatabaseSystems")
    void testUpdateEntry(DBMSType dbmsType, DBMSConnection dbmsConnection, DBMSProcessor dbmsProcessor) throws Exception {
        dbmsProcessor.setupSharedDatabase();
        BibEntry expectedEntry = getBibEntryExample();
        dbmsProcessor.insertEntry(expectedEntry);

        expectedEntry.setType(StandardEntryType.Book);
        expectedEntry.setField(StandardField.AUTHOR, "Michael J and Hutchings");
        expectedEntry.setField(new UnknownField("customField"), "custom value");
        expectedEntry.clearField(StandardField.BOOKTITLE);
        dbmsProcessor.updateEntry(expectedEntry);

        Optional<BibEntry> actualEntry = dbmsProcessor.getSharedEntry(expectedEntry.getSharedBibEntryData().getSharedID());
        assertEquals(expectedEntry, actualEntry.get());

        clear(dbmsConnection);
    }

    @ParameterizedTest
    @MethodSource("org.jabref.logic.shared.TestManager#getTestingDatabaseSystems")
    void testGetEntriesByIdList(DBMSType dbmsType, DBMSConnection dbmsConnection, DBMSProcessor dbmsProcessor) throws Exception {
        dbmsProcessor.setupSharedDatabase();
        BibEntry firstEntry = getBibEntryExample();
        firstEntry.setField(InternalField.INTERNAL_ID_FIELD, "00001");
        BibEntry secondEntry = getBibEntryExample();
        secondEntry.setField(InternalField.INTERNAL_ID_FIELD, "00002");

        dbmsProcessor.insertEntry(firstEntry);
        dbmsProcessor.insertEntry(secondEntry);

        List<BibEntry> sharedEntriesByIdList = dbmsProcessor.getSharedEntries(Arrays.asList(1, 2));

        clear(dbmsConnection);

        assertEquals(firstEntry, sharedEntriesByIdList.get(0));
        assertEquals(secondEntry, sharedEntriesByIdList.get(1));

    }

    @ParameterizedTest
    @MethodSource("org.jabref.logic.shared.TestManager#getTestingDatabaseSystems")
    void testUpdateNewerEntry(DBMSType dbmsType, DBMSConnection dbmsConnection, DBMSProcessor dbmsProcessor) throws OfflineLockException, SQLException {
        dbmsProcessor.setupSharedDatabase();
        BibEntry bibEntry = getBibEntryExample();

        dbmsProcessor.insertEntry(bibEntry);

        //simulate older version
        bibEntry.getSharedBibEntryData().setVersion(0);
        bibEntry.setField(StandardField.YEAR, "1993");

        assertThrows(OfflineLockException.class, () -> dbmsProcessor.updateEntry(bibEntry));

        clear(dbmsConnection);
    }

    @ParameterizedTest
    @MethodSource("org.jabref.logic.shared.TestManager#getTestingDatabaseSystems")
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

        clear(dbmsConnection);
    }

    @ParameterizedTest
    @MethodSource("org.jabref.logic.shared.TestManager#getTestingDatabaseSystems")
    void testRemoveAllEntries(DBMSType dbmsType, DBMSConnection dbmsConnection, DBMSProcessor dbmsProcessor) throws SQLException {
        dbmsProcessor.setupSharedDatabase();
        BibEntry firstEntry = getBibEntryExample();
        BibEntry secondEntry = getBibEntryExample();
        List<BibEntry> entriesToRemove = Arrays.asList(firstEntry, secondEntry);
        dbmsProcessor.insertEntry(firstEntry);
        dbmsProcessor.insertEntry(secondEntry);
        dbmsProcessor.removeEntries(entriesToRemove);

        try (ResultSet resultSet = selectFrom("ENTRY", dbmsConnection, dbmsProcessor)) {
            assertFalse(resultSet.next());
        }
        clear(dbmsConnection);
    }

    @ParameterizedTest
    @MethodSource("org.jabref.logic.shared.TestManager#getTestingDatabaseSystems")
    void testRemoveSomeEntries(DBMSType dbmsType, DBMSConnection dbmsConnection, DBMSProcessor dbmsProcessor) throws SQLException {
        dbmsProcessor.setupSharedDatabase();
        BibEntry firstEntry = getBibEntryExample();
        BibEntry secondEntry = getBibEntryExample();
        BibEntry thirdEntry = getBibEntryExample();

        // Remove the first and third entries - the second should remain (SHARED_ID will be 2)

        List<BibEntry> entriesToRemove = Arrays.asList(firstEntry, thirdEntry);
        dbmsProcessor.insertEntry(firstEntry);
        dbmsProcessor.insertEntry(secondEntry);
        dbmsProcessor.insertEntry(thirdEntry);
        dbmsProcessor.removeEntries(entriesToRemove);

        try (ResultSet entryResultSet = selectFrom("ENTRY", dbmsConnection, dbmsProcessor)) {
            assertTrue(entryResultSet.next());
            assertEquals(2, entryResultSet.getInt("SHARED_ID"));
            assertFalse(entryResultSet.next());
        }

        clear(dbmsConnection);
    }

    @ParameterizedTest
    @MethodSource("org.jabref.logic.shared.TestManager#getTestingDatabaseSystems")
    void testRemoveSingleEntry(DBMSType dbmsType, DBMSConnection dbmsConnection, DBMSProcessor dbmsProcessor) throws SQLException {
        dbmsProcessor.setupSharedDatabase();
        BibEntry entryToRemove = getBibEntryExample();
        dbmsProcessor.insertEntry(entryToRemove);
        dbmsProcessor.removeEntries(Collections.singletonList(entryToRemove));

        try (ResultSet entryResultSet = selectFrom("ENTRY", dbmsConnection, dbmsProcessor)) {
            assertFalse(entryResultSet.next());
        }

        clear(dbmsConnection);
    }

    @ParameterizedTest
    @MethodSource("org.jabref.logic.shared.TestManager#getTestingDatabaseSystems")
    void testRemoveEntriesOnNullThrows(DBMSType dbmsType, DBMSConnection dbmsConnection, DBMSProcessor dbmsProcessor) throws SQLException {
        dbmsProcessor.setupSharedDatabase();
        assertThrows(NullPointerException.class, () -> dbmsProcessor.removeEntries(null));
        clear(dbmsConnection);
    }

    @ParameterizedTest
    @MethodSource("org.jabref.logic.shared.TestManager#getTestingDatabaseSystems")
    void testRemoveEmptyEntryList(DBMSType dbmsType, DBMSConnection dbmsConnection, DBMSProcessor dbmsProcessor) throws SQLException {
        dbmsProcessor.setupSharedDatabase();
        dbmsProcessor.removeEntries(Collections.emptyList());

        try (ResultSet entryResultSet = selectFrom("ENTRY", dbmsConnection, dbmsProcessor)) {
            assertFalse(entryResultSet.next());
        }

        clear(dbmsConnection);
    }

    @ParameterizedTest
    @MethodSource("org.jabref.logic.shared.TestManager#getTestingDatabaseSystems")
    void testGetSharedEntries(DBMSType dbmsType, DBMSConnection dbmsConnection, DBMSProcessor dbmsProcessor) throws SQLException {
        dbmsProcessor.setupSharedDatabase();
        BibEntry bibEntry = getBibEntryExampleWithEmptyFields();

        dbmsProcessor.insertEntry(bibEntry);

        List<BibEntry> expectedEntries = Arrays.asList(bibEntry);
        List<BibEntry> actualEntries = dbmsProcessor.getSharedEntries();

        assertEquals(expectedEntries, actualEntries);
        clear(dbmsConnection);
    }

    @ParameterizedTest
    @MethodSource("org.jabref.logic.shared.TestManager#getTestingDatabaseSystems")
    void testGetSharedEntry(DBMSType dbmsType, DBMSConnection dbmsConnection, DBMSProcessor dbmsProcessor) throws SQLException {
        dbmsProcessor.setupSharedDatabase();
        BibEntry expectedBibEntry = getBibEntryExampleWithEmptyFields();

        dbmsProcessor.insertEntry(expectedBibEntry);

        Optional<BibEntry> actualBibEntryOptional = dbmsProcessor.getSharedEntry(expectedBibEntry.getSharedBibEntryData().getSharedID());

        assertEquals(expectedBibEntry, actualBibEntryOptional.get());
        clear(dbmsConnection);
    }

    @ParameterizedTest
    @MethodSource("org.jabref.logic.shared.TestManager#getTestingDatabaseSystems")
    void testGetNotExistingSharedEntry(DBMSType dbmsType, DBMSConnection dbmsConnection, DBMSProcessor dbmsProcessor) throws SQLException {
        dbmsProcessor.setupSharedDatabase();
        Optional<BibEntry> actualBibEntryOptional = dbmsProcessor.getSharedEntry(1);
        assertFalse(actualBibEntryOptional.isPresent());

        clear(dbmsConnection);
    }

    @ParameterizedTest
    @MethodSource("org.jabref.logic.shared.TestManager#getTestingDatabaseSystems")
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

        clear(dbmsConnection);
    }

    @ParameterizedTest
    @MethodSource("org.jabref.logic.shared.TestManager#getTestingDatabaseSystems")
    void testGetSharedMetaData(DBMSType dbmsType, DBMSConnection dbmsConnection, DBMSProcessor dbmsProcessor) throws SQLException {
        dbmsProcessor.setupSharedDatabase();
        insertMetaData("databaseType", "bibtex;", dbmsConnection, dbmsProcessor);
        insertMetaData("protectedFlag", "true;", dbmsConnection, dbmsProcessor);
        insertMetaData("saveActions", "enabled;\nauthor[capitalize,html_to_latex]\ntitle[title_case]\n;", dbmsConnection, dbmsProcessor);
        insertMetaData("saveOrderConfig", "specified;author;false;title;false;year;true;", dbmsConnection, dbmsProcessor);

        Map<String, String> expectedMetaData = getMetaDataExample();
        Map<String, String> actualMetaData = dbmsProcessor.getSharedMetaData();

        assertEquals(expectedMetaData, actualMetaData);
        clear(dbmsConnection);
    }

    @ParameterizedTest
    @MethodSource("getTestingDatabaseSystems")
    void testSetSharedMetaData(DBMSType dbmsType, DBMSConnection dbmsConnection, DBMSProcessor dbmsProcessor) throws SQLException {
        dbmsProcessor.setupSharedDatabase();
        Map<String, String> expectedMetaData = getMetaDataExample();
        dbmsProcessor.setSharedMetaData(expectedMetaData);

        Map<String, String> actualMetaData = dbmsProcessor.getSharedMetaData();

        assertEquals(expectedMetaData, actualMetaData);
        clear(dbmsConnection);
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

    void clear(DBMSConnection dbmsConnection) throws SQLException {
        TestManager.clearTables(dbmsConnection);
    }
}
