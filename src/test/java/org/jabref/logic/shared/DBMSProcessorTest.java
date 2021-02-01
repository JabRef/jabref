package org.jabref.logic.shared;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.logic.shared.exception.OfflineLockException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.testutils.category.DatabaseTest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@DatabaseTest
@Execution(ExecutionMode.SAME_THREAD)
class DBMSProcessorTest {

    private DBMSConnection dbmsConnection;
    private DBMSProcessor dbmsProcessor;
    private DBMSType dbmsType;

    @BeforeEach
    public void setup() throws Exception {
        this.dbmsType = TestManager.getDBMSTypeTestParameter();
        this.dbmsConnection = TestConnector.getTestDBMSConnection(dbmsType);
        this.dbmsProcessor = DBMSProcessor.getProcessorInstance(TestConnector.getTestDBMSConnection(dbmsType));
        TestManager.clearTables(this.dbmsConnection);
        dbmsProcessor.setupSharedDatabase();
    }

    @AfterEach
    public void closeDbmsConnection() throws SQLException {
        this.dbmsConnection.getConnection().close();
    }

    @Test
    void databaseIntegrityFullFiledAfterSetup() throws SQLException {
        assertTrue(dbmsProcessor.checkBaseIntegrity());
    }

    @Test
    void databaseIntegrityBrokenAfterClearedTables() throws SQLException {
        TestManager.clearTables(this.dbmsConnection);
        assertFalse(dbmsProcessor.checkBaseIntegrity());
    }

    @Test
    void testInsertEntry() throws SQLException {
        BibEntry expectedEntry = getBibEntryExample();

        dbmsProcessor.insertEntry(expectedEntry);

        BibEntry emptyEntry = getBibEntryExample();
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
    }

    @Test
    void testInsertEntryWithEmptyFields() throws SQLException {
        BibEntry expectedEntry = new BibEntry(StandardEntryType.Article);

        dbmsProcessor.insertEntry(expectedEntry);

        try (ResultSet entryResultSet = selectFrom("ENTRY", dbmsConnection, dbmsProcessor)) {
            assertTrue(entryResultSet.next());
            assertEquals(1, entryResultSet.getInt("SHARED_ID"));
            assertEquals("article", entryResultSet.getString("TYPE"));
            assertEquals(1, entryResultSet.getInt("VERSION"));
            assertFalse(entryResultSet.next());

            // Adding an empty entry should not create an entry in field table, only in entry table
            try (ResultSet fieldResultSet = selectFrom("FIELD", dbmsConnection, dbmsProcessor)) {
                assertFalse(fieldResultSet.next());
            }
        }
    }

    private static BibEntry getBibEntryExample() {
        return new BibEntry(StandardEntryType.InProceedings)
                .withField(StandardField.AUTHOR, "Wirthlin, Michael J and Hutchings, Brad L and Gilson, Kent L")
                .withField(StandardField.TITLE, "The nano processor: a low resource reconfigurable processor")
                .withField(StandardField.BOOKTITLE, "FPGAs for Custom Computing Machines, 1994. Proceedings. IEEE Workshop on")
                .withField(StandardField.YEAR, "1994")
                .withCitationKey("nanoproc1994");
    }

    @Test
    void testUpdateEntry() throws Exception {
        BibEntry expectedEntry = getBibEntryExample();
        dbmsProcessor.insertEntry(expectedEntry);

        expectedEntry.setType(StandardEntryType.Book);
        expectedEntry.setField(StandardField.AUTHOR, "Michael J and Hutchings");
        expectedEntry.setField(new UnknownField("customField"), "custom value");
        expectedEntry.clearField(StandardField.BOOKTITLE);
        dbmsProcessor.updateEntry(expectedEntry);

        Optional<BibEntry> actualEntry = dbmsProcessor.getSharedEntry(expectedEntry.getSharedBibEntryData().getSharedID());
        assertEquals(Optional.of(expectedEntry), actualEntry);
    }

    @Test
    void testUpdateEmptyEntry() throws Exception {
        BibEntry expectedEntry = new BibEntry(StandardEntryType.Article);
        dbmsProcessor.insertEntry(expectedEntry);

        expectedEntry.setField(StandardField.AUTHOR, "Michael J and Hutchings");
        expectedEntry.setField(new UnknownField("customField"), "custom value");
        // Update field should now find the entry
        dbmsProcessor.updateEntry(expectedEntry);

        Optional<BibEntry> actualEntry = dbmsProcessor.getSharedEntry(expectedEntry.getSharedBibEntryData().getSharedID());
        assertEquals(Optional.of(expectedEntry), actualEntry);
    }

    @Test
    void testGetEntriesByIdList() throws Exception {
        BibEntry firstEntry = getBibEntryExample();
        firstEntry.setField(InternalField.INTERNAL_ID_FIELD, "00001");
        BibEntry secondEntry = getBibEntryExample();
        secondEntry.setField(InternalField.INTERNAL_ID_FIELD, "00002");

        dbmsProcessor.insertEntry(firstEntry);
        dbmsProcessor.insertEntry(secondEntry);

        List<BibEntry> sharedEntriesByIdList = dbmsProcessor.getSharedEntries(Arrays.asList(1, 2));

        assertEquals(List.of(firstEntry, secondEntry), sharedEntriesByIdList);
    }

    @Test
    void testUpdateNewerEntry() {
        BibEntry bibEntry = getBibEntryExample();

        dbmsProcessor.insertEntry(bibEntry);

        // simulate older version
        bibEntry.getSharedBibEntryData().setVersion(0);
        bibEntry.setField(StandardField.YEAR, "1993");

        assertThrows(OfflineLockException.class, () -> dbmsProcessor.updateEntry(bibEntry));
    }

    @Test
    void testUpdateEqualEntry() throws OfflineLockException, SQLException {
        BibEntry expectedBibEntry = getBibEntryExample();

        dbmsProcessor.insertEntry(expectedBibEntry);
        // simulate older version
        expectedBibEntry.getSharedBibEntryData().setVersion(0);
        dbmsProcessor.updateEntry(expectedBibEntry);

        Optional<BibEntry> actualBibEntryOptional = dbmsProcessor
                .getSharedEntry(expectedBibEntry.getSharedBibEntryData().getSharedID());

        assertEquals(Optional.of(expectedBibEntry), actualBibEntryOptional);
    }

    @Test
    void testRemoveAllEntries() throws SQLException {
        BibEntry firstEntry = getBibEntryExample();
        BibEntry secondEntry = getBibEntryExample2();
        List<BibEntry> entriesToRemove = Arrays.asList(firstEntry, secondEntry);
        dbmsProcessor.insertEntry(firstEntry);
        dbmsProcessor.insertEntry(secondEntry);
        dbmsProcessor.removeEntries(entriesToRemove);

        try (ResultSet resultSet = selectFrom("ENTRY", dbmsConnection, dbmsProcessor)) {
            assertFalse(resultSet.next());
        }
    }

    @Test
    void testRemoveSomeEntries() throws SQLException {
        BibEntry firstEntry = getBibEntryExample();
        BibEntry secondEntry = getBibEntryExample2();
        BibEntry thirdEntry = getBibEntryExample3();

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
    }

    @Test
    void testRemoveSingleEntry() throws SQLException {
        BibEntry entryToRemove = getBibEntryExample();
        dbmsProcessor.insertEntry(entryToRemove);
        dbmsProcessor.removeEntries(Collections.singletonList(entryToRemove));

        try (ResultSet entryResultSet = selectFrom("ENTRY", dbmsConnection, dbmsProcessor)) {
            assertFalse(entryResultSet.next());
        }
    }

    @Test
    void testRemoveEntriesOnNullThrows() {
        assertThrows(NullPointerException.class, () -> dbmsProcessor.removeEntries(null));
    }

    @Test
    void testRemoveEmptyEntryList() throws SQLException {
        dbmsProcessor.removeEntries(Collections.emptyList());

        try (ResultSet entryResultSet = selectFrom("ENTRY", dbmsConnection, dbmsProcessor)) {
            assertFalse(entryResultSet.next());
        }
    }

    @Test
    void testGetSharedEntries() {
        BibEntry bibEntry = getBibEntryExampleWithEmptyFields();

        dbmsProcessor.insertEntry(bibEntry);

        List<BibEntry> actualEntries = dbmsProcessor.getSharedEntries();

        assertEquals(List.of(bibEntry), actualEntries);
    }

    @Test
    void testGetSharedEntry() {
        BibEntry expectedBibEntry = getBibEntryExampleWithEmptyFields();

        dbmsProcessor.insertEntry(expectedBibEntry);

        Optional<BibEntry> actualBibEntryOptional = dbmsProcessor.getSharedEntry(expectedBibEntry.getSharedBibEntryData().getSharedID());

        assertEquals(Optional.of(expectedBibEntry), actualBibEntryOptional);
    }

    @Test
    void testGetNotExistingSharedEntry() {
        Optional<BibEntry> actualBibEntryOptional = dbmsProcessor.getSharedEntry(1);
        assertFalse(actualBibEntryOptional.isPresent());
    }

    @Test
    void testGetSharedIDVersionMapping() throws OfflineLockException, SQLException {
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

    @Test
    void testGetSharedMetaData() {
        insertMetaData("databaseType", "bibtex;", dbmsConnection, dbmsProcessor);
        insertMetaData("protectedFlag", "true;", dbmsConnection, dbmsProcessor);
        insertMetaData("saveActions", "enabled;\nauthor[capitalize,html_to_latex]\ntitle[title_case]\n;", dbmsConnection, dbmsProcessor);
        insertMetaData("saveOrderConfig", "specified;author;false;title;false;year;true;", dbmsConnection, dbmsProcessor);

        Map<String, String> expectedMetaData = getMetaDataExample();
        Map<String, String> actualMetaData = dbmsProcessor.getSharedMetaData();

        assertEquals(expectedMetaData, actualMetaData);
    }

    @Test
    void testSetSharedMetaData() throws SQLException {
        Map<String, String> expectedMetaData = getMetaDataExample();
        dbmsProcessor.setSharedMetaData(expectedMetaData);

        Map<String, String> actualMetaData = dbmsProcessor.getSharedMetaData();

        assertEquals(expectedMetaData, actualMetaData);
    }

    private static Map<String, String> getMetaDataExample() {
        Map<String, String> expectedMetaData = new HashMap<>();

        expectedMetaData.put("databaseType", "bibtex;");
        expectedMetaData.put("protectedFlag", "true;");
        expectedMetaData.put("saveActions", "enabled;\nauthor[capitalize,html_to_latex]\ntitle[title_case]\n;");
        expectedMetaData.put("saveOrderConfig", "specified;author;false;title;false;year;true;");

        return expectedMetaData;
    }

    private static BibEntry getBibEntryExampleWithEmptyFields() {
        BibEntry bibEntry = new BibEntry()
                .withField(StandardField.AUTHOR, "Author")
                .withField(StandardField.TITLE, "")
                .withField(StandardField.YEAR, "");
        bibEntry.getSharedBibEntryData().setSharedID(1);
        return bibEntry;
    }

    private static BibEntry getBibEntryExample2() {
        return new BibEntry(StandardEntryType.InProceedings)
                .withField(StandardField.AUTHOR, "Shelah, Saharon and Ziegler, Martin")
                .withField(StandardField.TITLE, "Algebraically closed groups of large cardinality")
                .withField(StandardField.JOURNAL, "The Journal of Symbolic Logic")
                .withField(StandardField.YEAR, "1979")
                .withCitationKey("algegrou1979");
    }

    private static BibEntry getBibEntryExample3() {
        return new BibEntry(StandardEntryType.InProceedings)
                .withField(StandardField.AUTHOR, "Hodges, Wilfrid and Shelah, Saharon")
                .withField(StandardField.TITLE, "Infinite games and reduced products")
                .withField(StandardField.JOURNAL, "Annals of Mathematical Logic")
                .withField(StandardField.YEAR, "1981")
                .withCitationKey("infigame1981");
    }

    @Test
    void testInsertMultipleEntries() throws SQLException {
        List<BibEntry> entries = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            entries.add(new BibEntry(StandardEntryType.Article).withField(StandardField.JOURNAL, "journal " + i)
                                                               .withField(StandardField.ISSUE, Integer.toString(i)));
        }
        entries.get(3).setType(StandardEntryType.Thesis);
        dbmsProcessor.insertEntries(entries);

        Map<Integer, Map<String, String>> actualFieldMap = new HashMap<>();

        try (ResultSet entryResultSet = selectFrom("ENTRY", dbmsConnection, dbmsProcessor)) {
            assertTrue(entryResultSet.next());
            assertEquals(1, entryResultSet.getInt("SHARED_ID"));
            assertEquals("article", entryResultSet.getString("TYPE"));
            assertEquals(1, entryResultSet.getInt("VERSION"));
            assertTrue(entryResultSet.next());
            assertEquals(2, entryResultSet.getInt("SHARED_ID"));
            assertEquals("article", entryResultSet.getString("TYPE"));
            assertEquals(1, entryResultSet.getInt("VERSION"));
            assertTrue(entryResultSet.next());
            assertEquals(3, entryResultSet.getInt("SHARED_ID"));
            assertEquals("article", entryResultSet.getString("TYPE"));
            assertEquals(1, entryResultSet.getInt("VERSION"));
            assertTrue(entryResultSet.next());
            assertEquals(4, entryResultSet.getInt("SHARED_ID"));
            assertEquals("thesis", entryResultSet.getString("TYPE"));
            assertEquals(1, entryResultSet.getInt("VERSION"));
            assertTrue(entryResultSet.next());
            assertEquals(5, entryResultSet.getInt("SHARED_ID"));
            assertEquals("article", entryResultSet.getString("TYPE"));
            assertEquals(1, entryResultSet.getInt("VERSION"));
            assertFalse(entryResultSet.next());

            try (ResultSet fieldResultSet = selectFrom("FIELD", dbmsConnection, dbmsProcessor)) {
                while (fieldResultSet.next()) {
                    if (actualFieldMap.containsKey(fieldResultSet.getInt("ENTRY_SHARED_ID"))) {
                        actualFieldMap.get(fieldResultSet.getInt("ENTRY_SHARED_ID")).put(
                                fieldResultSet.getString("NAME"), fieldResultSet.getString("VALUE"));
                    } else {
                        int sharedId = fieldResultSet.getInt("ENTRY_SHARED_ID");
                        actualFieldMap.put(sharedId,
                                new HashMap<>());
                        actualFieldMap.get(sharedId).put(fieldResultSet.getString("NAME"),
                                fieldResultSet.getString("VALUE"));
                    }
                }
            }
        }
        Map<Integer, Map<String, String>> expectedFieldMap = entries.stream()
                                                                    .collect(Collectors.toMap(bibEntry -> bibEntry.getSharedBibEntryData().getSharedID(),
                                                                            (bibEntry) -> bibEntry.getFieldMap().entrySet().stream()
                                                                                                  .collect(Collectors.toMap((entry) -> entry.getKey().getName(), Map.Entry::getValue))));

        assertEquals(expectedFieldMap, actualFieldMap);
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

    private static String escape(String expression, DBMSProcessor dbmsProcessor) {
        return dbmsProcessor.escape(expression);
    }

    private static String escapeValue(String value) {
        return "'" + value + "'";
    }
}
