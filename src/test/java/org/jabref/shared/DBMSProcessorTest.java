package org.jabref.shared;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.shared.exception.InvalidDBMSConnectionPropertiesException;
import org.jabref.shared.exception.OfflineLockException;
import org.jabref.testutils.category.DatabaseTests;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
@Category(DatabaseTests.class)
public class DBMSProcessorTest {

    private DBMSConnection dbmsConnection;
    private DBMSProcessor dbmsProcessor;

    @Parameter
    public DBMSType dbmsType;


    @Before
    public void setUp() throws SQLException, InvalidDBMSConnectionPropertiesException {
        dbmsConnection = TestConnector.getTestDBMSConnection(dbmsType);
        dbmsProcessor = DBMSProcessor.getProcessorInstance(dbmsConnection);
        dbmsProcessor.setupSharedDatabase();
    }

    @Parameters(name = "Test with {0} database system")
    public static Collection<DBMSType> getTestingDatabaseSystems() {
        return TestManager.getDBMSTypeTestParameter();
    }

    @Test
    public void testCheckBaseIntegrity() throws SQLException {
        Assert.assertTrue(dbmsProcessor.checkBaseIntegrity());
        clear();
        Assert.assertFalse(dbmsProcessor.checkBaseIntegrity());
    }

    @Test
    public void testSetUpSharedDatabase() throws SQLException {
        clear();
        dbmsProcessor.setupSharedDatabase();
        Assert.assertTrue(dbmsProcessor.checkBaseIntegrity());
    }

    @Test
    public void testInsertEntry() throws SQLException {
        BibEntry expectedEntry = getBibEntryExample();

        dbmsProcessor.insertEntry(expectedEntry);

        BibEntry emptyEntry = new BibEntry();
        emptyEntry.getSharedBibEntryData().setSharedID(1);
        dbmsProcessor.insertEntry(emptyEntry); // does not insert, due to same sharedID.

        Map<String, String> actualFieldMap = new HashMap<>();

        try (ResultSet entryResultSet = selectFrom("ENTRY")) {
            Assert.assertTrue(entryResultSet.next());
            Assert.assertEquals(1, entryResultSet.getInt("SHARED_ID"));
            Assert.assertEquals("inproceedings", entryResultSet.getString("TYPE"));
            Assert.assertEquals(1, entryResultSet.getInt("VERSION"));
            Assert.assertFalse(entryResultSet.next());

            try (ResultSet fieldResultSet = selectFrom("FIELD")) {
                while (fieldResultSet.next()) {
                    actualFieldMap.put(fieldResultSet.getString("NAME"), fieldResultSet.getString("VALUE"));
                }
            }
        }

        Map<String, String> expectedFieldMap = expectedEntry.getFieldMap();

        Assert.assertEquals(expectedFieldMap, actualFieldMap);
    }

    @Test
    public void testUpdateEntry() throws OfflineLockException, SQLException {
        BibEntry expectedEntry = getBibEntryExample();

        dbmsProcessor.insertEntry(expectedEntry);

        expectedEntry.setType("book");
        expectedEntry.setField("author", "Michael J and Hutchings");
        expectedEntry.setField("customField", "custom value");
        expectedEntry.clearField("booktitle");

        dbmsProcessor.updateEntry(expectedEntry);

        Optional<BibEntry> actualEntryOptional = dbmsProcessor
                .getSharedEntry(expectedEntry.getSharedBibEntryData().getSharedID());

        if (actualEntryOptional.isPresent()) {
            Assert.assertEquals(expectedEntry, actualEntryOptional.get());
        } else {
            Assert.fail();
        }
    }

    @Test(expected = OfflineLockException.class)
    public void testUpdateNewerEntry() throws OfflineLockException, SQLException {
        BibEntry bibEntry = getBibEntryExample();

        dbmsProcessor.insertEntry(bibEntry);

        //simulate older version
        bibEntry.getSharedBibEntryData().setVersion(0);
        bibEntry.setField("year", "1993");

        dbmsProcessor.updateEntry(bibEntry);
    }

    @Test
    public void testUpdateEqualEntry() throws OfflineLockException, SQLException {
        BibEntry expectedBibEntry = getBibEntryExample();

        dbmsProcessor.insertEntry(expectedBibEntry);
        //simulate older version
        expectedBibEntry.getSharedBibEntryData().setVersion(0);
        dbmsProcessor.updateEntry(expectedBibEntry);

        Optional<BibEntry> actualBibEntryOptional = dbmsProcessor
                .getSharedEntry(expectedBibEntry.getSharedBibEntryData().getSharedID());

        if (actualBibEntryOptional.isPresent()) {
            Assert.assertEquals(expectedBibEntry, actualBibEntryOptional.get());
        } else {
            Assert.fail();
        }
    }

    @Test
    public void testRemoveEntry() throws SQLException {
        BibEntry bibEntry = getBibEntryExample();
        dbmsProcessor.insertEntry(bibEntry);
        dbmsProcessor.removeEntry(bibEntry);

        try (ResultSet resultSet = selectFrom("ENTRY")) {
            Assert.assertFalse(resultSet.next());
        }
    }

    @Test
    public void testGetSharedEntries() {
        BibEntry bibEntry = getBibEntryExampleWithEmptyFields();

        dbmsProcessor.insertEntry(bibEntry);

        List<BibEntry> expectedEntries = Arrays.asList(bibEntry);
        List<BibEntry> actualEntries = dbmsProcessor.getSharedEntries();

        Assert.assertEquals(expectedEntries, actualEntries);
    }

    @Test
    public void testGetSharedEntry() {
        BibEntry expectedBibEntry = getBibEntryExampleWithEmptyFields();

        dbmsProcessor.insertEntry(expectedBibEntry);

        Optional<BibEntry> actualBibEntryOptional = dbmsProcessor
                .getSharedEntry(expectedBibEntry.getSharedBibEntryData().getSharedID());

        if (actualBibEntryOptional.isPresent()) {
            Assert.assertEquals(expectedBibEntry, actualBibEntryOptional.get());
        } else {
            Assert.fail();
        }
    }

    @Test
    public void testGetNotExistingSharedEntry() {
        Optional<BibEntry> actualBibEntryOptional = dbmsProcessor.getSharedEntry(1);
        Assert.assertFalse(actualBibEntryOptional.isPresent());
    }

    @Test
    public void testGetSharedIDVersionMapping() throws OfflineLockException, SQLException {
        BibEntry firstEntry = getBibEntryExample();
        BibEntry secondEntry = getBibEntryExample();

        dbmsProcessor.insertEntry(firstEntry);
        dbmsProcessor.insertEntry(secondEntry);
        dbmsProcessor.updateEntry(secondEntry);

        Map<Integer, Integer> expectedIDVersionMap = new HashMap<>();
        expectedIDVersionMap.put(firstEntry.getSharedBibEntryData().getSharedID(), 1);
        expectedIDVersionMap.put(secondEntry.getSharedBibEntryData().getSharedID(), 2);

        Map<Integer, Integer> actualIDVersionMap = dbmsProcessor.getSharedIDVersionMapping();

        Assert.assertEquals(expectedIDVersionMap, actualIDVersionMap);

    }

    @Test
    public void testGetSharedMetaData() {
        insertMetaData("databaseType", "bibtex;");
        insertMetaData("protectedFlag", "true;");
        insertMetaData("saveActions", "enabled;\nauthor[capitalize,html_to_latex]\ntitle[title_case]\n;");
        insertMetaData("saveOrderConfig", "specified;author;false;title;false;year;true;");

        Map<String, String> expectedMetaData = getMetaDataExample();
        Map<String, String> actualMetaData = dbmsProcessor.getSharedMetaData();

        Assert.assertEquals(expectedMetaData, actualMetaData);

    }

    @Test
    public void testSetSharedMetaData() throws SQLException {
        Map<String, String> expectedMetaData = getMetaDataExample();
        dbmsProcessor.setSharedMetaData(expectedMetaData);

        Map<String, String> actualMetaData = dbmsProcessor.getSharedMetaData();

        Assert.assertEquals(expectedMetaData, actualMetaData);
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
        bibEntry.setField("author", "Author");
        bibEntry.setField("title", "");
        bibEntry.setField("year", "");
        bibEntry.getSharedBibEntryData().setSharedID(1);
        return bibEntry;
    }

    private BibEntry getBibEntryExample() {
        BibEntry bibEntry = new BibEntry();
        bibEntry.setType("inproceedings");
        bibEntry.setField("author", "Wirthlin, Michael J and Hutchings, Brad L and Gilson, Kent L");
        bibEntry.setField("title", "The nano processor: a low resource reconfigurable processor");
        bibEntry.setField("booktitle", "FPGAs for Custom Computing Machines, 1994. Proceedings. IEEE Workshop on");
        bibEntry.setField("year", "1994");
        bibEntry.setCiteKey("nanoproc1994");
        return bibEntry;
    }

    private ResultSet selectFrom(String table) {
        try {
            return dbmsConnection.getConnection().createStatement().executeQuery("SELECT * FROM " + escape(table));
        } catch (SQLException e) {
            Assert.fail(e.getMessage());
            return null;
        }
    }

    // Oracle does not support multiple tuple insertion in one INSERT INTO command.
    // Therefore this function was defined to improve the readability and to keep the code short.
    private void insertMetaData(String key, String value) {
        try {
            dbmsConnection.getConnection().createStatement().executeUpdate("INSERT INTO " + escape("METADATA") + "("
                    + escape("KEY") + ", " + escape("VALUE") + ") VALUES("
                    + escapeValue(key) + ", " + escapeValue(value) + ")");
        } catch (SQLException e) {
            Assert.fail(e.getMessage());
        }
    }

    private String escape(String expression) {
        return dbmsProcessor.escape(expression);
    }

    private String escapeValue(String value) {
        return "'" + value + "'";
    }

    @After
    public void clear() throws SQLException {
        TestManager.clearTables(dbmsConnection);
    }
}
