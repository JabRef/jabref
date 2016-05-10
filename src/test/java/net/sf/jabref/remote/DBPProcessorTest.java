package net.sf.jabref.remote;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.jabref.model.entry.BibEntry;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class DBPProcessorTest {

    private static Connection connection;
    private DBProcessor dbProcessor;
    private DBHelper dbHelper;

    @Parameter
    public DBType dbType;


    @Before
    public void setUp() {

        // Get only one connection for each parameter
        if (TestConnector.currentConnectionType != dbType) {
            try {
                connection = TestConnector.getTestConnection(dbType);
            } catch (Exception e) {
                Assert.fail(e.getMessage());
            }
        }
        dbProcessor = new DBProcessor(connection, dbType);
        dbHelper = new DBHelper(connection);
        dbProcessor.setUpRemoteDatabase();

    }

    @Parameters(name = "Test with {0} database system")
    public static Collection<DBType> getTestingDatabaseSystems() {
        Set<DBType> dbTypes = new HashSet<>();
        dbTypes.add(DBType.MYSQL);
        dbTypes.add(DBType.POSTGRESQL);

        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            dbTypes.add(DBType.ORACLE);
        } catch (ClassNotFoundException e) {
            // In case that Oracle interface is not available do not perform tests for this system.
            System.out.println("Oracle driver not available. Skipping tests for this system...");
        }
        return dbTypes;
    }

    @Test
    public void testCheckBaseIntegrity() {
        Assert.assertTrue(dbProcessor.checkBaseIntegrity());
        clear();
        Assert.assertFalse(dbProcessor.checkBaseIntegrity());
    }

    @Test
    public void testSetUpRemoteDatabase() {
        clear();
        dbProcessor.setUpRemoteDatabase();
        Assert.assertTrue(dbProcessor.checkBaseIntegrity());
    }

    @Test
    public void testInsertEntry() {

        BibEntry realEntry = new BibEntry();
        realEntry.setType("inproceedings");
        realEntry.setField("author", "Wirthlin, Michael J and Hutchings, Brad L and Gilson, Kent L");
        realEntry.setField("title", "The nano processor: a low resource reconfigurable processor");
        realEntry.setField("booktitle", "FPGAs for Custom Computing Machines, 1994. Proceedings. IEEE Workshop on");
        realEntry.setField("year", "1994");
        realEntry.setCiteKey("nanoproc1994");
        realEntry.setRemoteId(1);

        dbProcessor.insertEntry(realEntry);

        BibEntry emptyEntry = new BibEntry();
        emptyEntry.setRemoteId(1);
        dbProcessor.insertEntry(emptyEntry); // does not insert, due to same remoteId.

        try (ResultSet resultSet = selectFrom(DBProcessor.ENTRY)) {

            Assert.assertTrue(resultSet.next());
            Assert.assertEquals(realEntry.getType(), resultSet.getString("ENTRYTYPE"));
            Assert.assertEquals(realEntry.getField("author"), resultSet.getString("AUTHOR"));
            Assert.assertEquals(realEntry.getField("title"), resultSet.getString("TITLE"));
            Assert.assertEquals(realEntry.getField("booktitle"), resultSet.getString("BOOKTITLE"));
            Assert.assertEquals(realEntry.getField("year"), resultSet.getString("YEAR"));
            Assert.assertEquals(realEntry.getCiteKey(), resultSet.getString("BIBTEXKEY"));
            Assert.assertFalse(resultSet.next());

        } catch (SQLException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testUpdateEntry() {
        BibEntry bibEntry = getBibEntryExample();
        dbProcessor.insertEntry(bibEntry);

        try {
            try (ResultSet resultSet = selectFrom(DBProcessor.ENTRY)) {

                Assert.assertTrue(resultSet.next());
                Assert.assertEquals(bibEntry.getType(), resultSet.getString("ENTRYTYPE"));
                Assert.assertEquals(bibEntry.getField("author"), resultSet.getString("AUTHOR"));
                Assert.assertEquals(bibEntry.getField("title"), resultSet.getString("TITLE"));
                Assert.assertFalse(resultSet.next());
            }

            bibEntry.setType("booklet");
            bibEntry.setField("author", "Brad L and Gilson, Kent L");
            bibEntry.setField("title", "The nano multiplexer");
            dbProcessor.updateEntry(bibEntry);

            try (ResultSet resultSet = selectFrom(DBProcessor.ENTRY)) {
                Assert.assertTrue(resultSet.next());
                Assert.assertEquals(bibEntry.getType(), resultSet.getString("ENTRYTYPE"));
                Assert.assertEquals(bibEntry.getField("author"), resultSet.getString("AUTHOR"));
                Assert.assertEquals(bibEntry.getField("title"), resultSet.getString("TITLE"));
                Assert.assertFalse(resultSet.next());
            }

        } catch (SQLException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testRemoveEntry() {
        BibEntry bibEntry = getBibEntryExample();
        dbProcessor.insertEntry(bibEntry);
        dbProcessor.removeEntry(bibEntry);

        try (ResultSet resultSet = selectFrom(DBProcessor.ENTRY)) {
            Assert.assertFalse(resultSet.next());
        } catch (SQLException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testPrepareEntryTableStructure() {
        BibEntry bibEntry = getBibEntryExample();

        dbProcessor.prepareEntryTableStructure(bibEntry);
        Set<String> actualColumns = dbHelper.allToUpperCase(dbHelper.getColumnNames(DBProcessor.ENTRY));

        Set<String> expectedColumns = new HashSet<>();
        expectedColumns.add(DBProcessor.ENTRY_REMOTE_ID);
        expectedColumns.add(DBProcessor.ENTRY_ENTRYTYPE);
        expectedColumns.add("AUTHOR");
        expectedColumns.add("TITLE");

        Assert.assertEquals(expectedColumns, actualColumns);

    }

    @Test
    public void testNormalizeEntryTable() {

        BibEntry bibEntry = getBibEntryExampleWithEmptyFields();

        dbProcessor.insertEntry(bibEntry);
        dbProcessor.normalizeEntryTable();

        Set<String> actualColumns = dbHelper.allToUpperCase(dbHelper.getColumnNames(DBProcessor.ENTRY));
        Set<String> expectedColumns = new HashSet<>();

        expectedColumns.add(DBProcessor.ENTRY_REMOTE_ID);
        expectedColumns.add(DBProcessor.ENTRY_ENTRYTYPE);
        expectedColumns.add("AUTHOR");

        Assert.assertEquals(expectedColumns, actualColumns);

    }

    @Test
    public void testGetRemoteEntries() {
        BibEntry bibEntry = getBibEntryExampleWithEmptyFields();

        dbProcessor.insertEntry(bibEntry);

        List<BibEntry> expectedEntries = Arrays.asList(bibEntry);
        List<BibEntry> actualEntries = dbProcessor.getRemoteEntries();

        Assert.assertEquals(expectedEntries, actualEntries);
    }

    @Test
    public void testGetRemoteMetaData() {
        insertMetaData(1, "databaseType", null, "bibtex");
        insertMetaData(2, "protectedFlag", null, "true");
        insertMetaData(3, "saveActions", null, "enabled");
        insertMetaData(4, "saveActions", "author", "capitalize");
        insertMetaData(5, "saveActions", "title", "title_case");
        insertMetaData(6, "saveActions", "title", "html_to_latex");
        insertMetaData(7, "saveOrderConfig", null, "specified");
        insertMetaData(8, "saveOrderConfig", "title", "false");
        insertMetaData(9, "saveOrderConfig", "author", "false");
        insertMetaData(10, "saveOrderConfig", "year", "false");

        Map<String, List<String>> expectedMetaData = getMetaDataExample();
        Map<String, List<String>> actualMetaData = dbProcessor.getRemoteMetaData();

        Assert.assertEquals(expectedMetaData, actualMetaData);

    }

    @Test
    public void testSetRemoteMetaData() {
        Map<String, List<String>> expectedMetaData = getMetaDataExample();

        dbProcessor.setRemoteMetaData(expectedMetaData);
        Map<String, List<String>> actualMetaData = dbProcessor.getRemoteMetaData();

        Assert.assertEquals(expectedMetaData, actualMetaData);
    }

    @Test
    public void testEscape() {

        if (dbType == DBType.MYSQL) {
            Assert.assertEquals("`TABLE`", dbProcessor.escape("TABLE"));
            Assert.assertEquals("`TABLE`", DBProcessor.escape("TABLE", dbType));
        } else if (dbType == DBType.POSTGRESQL) {
            Assert.assertEquals("TABLE", dbProcessor.escape("TABLE"));
            Assert.assertEquals("TABLE", DBProcessor.escape("TABLE", dbType));
        } else if (dbType == DBType.ORACLE) {
            Assert.assertEquals("\"TABLE\"", dbProcessor.escape("TABLE"));
            Assert.assertEquals("\"TABLE\"", DBProcessor.escape("TABLE", dbType));
        }

        Assert.assertEquals("TABLE", DBProcessor.escape("TABLE", null));
    }

    @Test
    public void testEscapeValue() {
        Assert.assertEquals("NULL", DBProcessor.escapeValue(null));
        Assert.assertEquals("'value'", DBProcessor.escapeValue("value"));
        Assert.assertEquals("1", DBProcessor.escapeValue(1));
    }

    private Map<String, List<String>> getMetaDataExample() {
        Map<String, List<String>> expectedMetaData = new HashMap<>();

        expectedMetaData.put("databaseType", Arrays.asList("bibtex"));
        expectedMetaData.put("protectedFlag", Arrays.asList("true"));
        expectedMetaData.put("saveActions",
                Arrays.asList("enabled", "author[capitalize]\ntitle[title_case,html_to_latex]"));
        expectedMetaData.put("saveOrderConfig",
                Arrays.asList("specified", "title", "false", "author", "false", "year", "false"));

        return expectedMetaData;
    }

    private BibEntry getBibEntryExampleWithEmptyFields() {
        BibEntry bibEntry = new BibEntry();
        bibEntry.setField("author", "Author");
        bibEntry.setField("title", "");
        bibEntry.setField("year", "");
        bibEntry.setRemoteId(1);
        return bibEntry;
    }

    private BibEntry getBibEntryExample() {
        BibEntry bibEntry = new BibEntry();
        bibEntry.setType("book");
        bibEntry.setField("author", "Wirthlin, Michael J");
        bibEntry.setField("title", "The nano processor");
        return bibEntry;
    }

    private ResultSet selectFrom(String table) {
        try {
            return connection.createStatement().executeQuery("SELECT * FROM " + escape(table));
        } catch (SQLException e) {
            Assert.fail(e.getMessage());
            return null;
        }
    }

    // Oracle does not support multiple tuple insertion in one INSERT INTO command.
    // Therefore this function was defined to improve the readability and to keep the code short.
    private void insertMetaData(int sortId, String key, String field, String value) {
        try {
            connection.createStatement().executeUpdate("INSERT INTO " + escape(DBProcessor.METADATA) + "("
                    + escape(DBProcessor.METADATA_SORT_ID) + ", " + escape(DBProcessor.METADATA_KEY) + ", "
                    + escape(DBProcessor.METADATA_FIELD) + ", " + escape(DBProcessor.METADATA_VALUE) + ") VALUES(" +
                    escapeValue(sortId) + ", " +
                    escapeValue(key) + ", " +
                    escapeValue(field) + ", " +
                    escapeValue(value) + ")");
        } catch (SQLException e) {
            Assert.fail(e.getMessage());
        }
    }

    private String escape(String expression) {
        return dbProcessor.escape(expression);
    }

    private String escapeValue(Object value) {
        return DBProcessor.escapeValue(value);
    }

    @After
    public void clear() {
        try {
            if ((dbType == DBType.MYSQL) || (dbType == DBType.POSTGRESQL)) {
                connection.createStatement().executeUpdate("DROP TABLE IF EXISTS " + escape(DBProcessor.ENTRY));
                connection.createStatement().executeUpdate("DROP TABLE IF EXISTS " + escape(DBProcessor.METADATA));
            } else if (dbType == DBType.ORACLE) {
                connection.createStatement().executeUpdate(
                            "BEGIN\n" +
                            "EXECUTE IMMEDIATE 'DROP TABLE " + escape(DBProcessor.ENTRY) + "';\n" +
                            "EXECUTE IMMEDIATE 'DROP TABLE " + escape(DBProcessor.METADATA) + "';\n" +
                            "EXECUTE IMMEDIATE 'DROP SEQUENCE " + escape(DBProcessor.ENTRY + "_SEQ") + "';\n" +
                            "EXCEPTION\n" +
                            "WHEN OTHERS THEN\n" +
                            "IF SQLCODE != -942 THEN\n" +
                            "RAISE;\n" +
                            "END IF;\n" +
                            "END;");
            }
        } catch (SQLException e) {
            Assert.fail(e.getMessage());
        }
    }
}
