package net.sf.jabref.sql;

import java.sql.Connection;
import java.util.Collections;
import java.util.List;

import net.sf.jabref.BibDatabaseContext;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.importer.fileformat.ParseException;
import net.sf.jabref.logic.groups.AllEntriesGroup;
import net.sf.jabref.logic.groups.GroupHierarchyType;
import net.sf.jabref.logic.groups.GroupTreeNode;
import net.sf.jabref.logic.groups.KeywordGroup;
import net.sf.jabref.model.EntryTypes;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.sql.exporter.DatabaseExporter;
import net.sf.jabref.sql.importer.DBImporterResult;
import net.sf.jabref.sql.importer.DatabaseImporter;
import net.sf.jabref.support.DevEnvironment;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DatabaseImportExportTests {

    @BeforeClass
    public static void setUp() {
        Globals.prefs = JabRefPreferences.getInstance();
        EntryTypes.removeAllCustomEntryTypes();
    }

    @Test
    public void testExportToMySQLSingleEntry() throws Exception {
        Assume.assumeTrue(DevEnvironment.isCIServer());

        BibDatabaseContext databaseContext = createContextWithSingleEntry();
        DatabaseType databaseType = DatabaseType.MYSQL;

        String databaseName = "jabref";
        DBStrings strings = new DBStrings();
        strings.setPassword("");
        strings.setDbPreferences(new DBStringsPreferences("mysql", "localhost", "root", "jabref"));

        testDatabaseExport(databaseContext, databaseType, databaseName, strings);
    }

    @Test
    public void testExportToMySQLSingleEntryUsingQuoteSymbol() throws Exception {
        Assume.assumeTrue(DevEnvironment.isCIServer());

        BibDatabaseContext databaseContext = createContextWithSingleEntryUsingQuoteSymbol();
        DatabaseType databaseType = DatabaseType.MYSQL;

        String databaseName = "jabref";
        DBStrings strings = new DBStrings();
        strings.setPassword("");
        strings.setDbPreferences(new DBStringsPreferences("mysql", "localhost", "root", "jabref"));

        testDatabaseExport(databaseContext, databaseType, databaseName, strings);
    }

    @Test
    public void testExportToMySQLSingleEntrySingleGroup() throws Exception {
        Assume.assumeTrue(DevEnvironment.isCIServer());

        BibDatabaseContext databaseContext = createContextWithSingleEntrySingleGroup();
        DatabaseType databaseType = DatabaseType.MYSQL;

        String databaseName = "jabref";
        DBStrings strings = new DBStrings();
        strings.setPassword("");
        strings.setDbPreferences(new DBStringsPreferences("mysql", "localhost", "root", "jabref"));

        testDatabaseExport(databaseContext, databaseType, databaseName, strings);
    }

    @Test
    public void testExportToPostgresSingleEntry() throws Exception {
        Assume.assumeTrue(DevEnvironment.isCIServer());

        BibDatabaseContext databaseContext = createContextWithSingleEntry();
        DatabaseType databaseType = DatabaseType.POSTGRESQL;

        String databaseName = "jabref";
        DBStrings strings = new DBStrings();
        strings.setPassword("");
        strings.setDbPreferences(new DBStringsPreferences("postgresql", "localhost", "postgres", "jabref"));

        testDatabaseExport(databaseContext, databaseType, databaseName, strings);
    }

    @Test
    public void testExportToPostgresSingleEntryUsingQuoteSymbol() throws Exception {
        Assume.assumeTrue(DevEnvironment.isCIServer());

        BibDatabaseContext databaseContext = createContextWithSingleEntryUsingQuoteSymbol();
        DatabaseType databaseType = DatabaseType.POSTGRESQL;

        String databaseName = "jabref";
        DBStrings strings = new DBStrings();
        strings.setPassword("");
        strings.setDbPreferences(new DBStringsPreferences("postgresql", "localhost", "postgres", "jabref"));

        testDatabaseExport(databaseContext, databaseType, databaseName, strings);
    }

    @Test
    public void testExportToPostgresSingleEntrySingleGroup() throws Exception {
        Assume.assumeTrue(DevEnvironment.isCIServer());

        BibDatabaseContext databaseContext = createContextWithSingleEntrySingleGroup();
        DatabaseType databaseType = DatabaseType.POSTGRESQL;

        String databaseName = "jabref";
        DBStrings strings = new DBStrings();
        strings.setPassword("");
        strings.setDbPreferences(new DBStringsPreferences("postgresql", "localhost", "postgres", "jabref"));

        testDatabaseExport(databaseContext, databaseType, databaseName, strings);
    }

    private void testDatabaseExport(BibDatabaseContext databaseContext, DatabaseType databaseType, String databaseName, DBStrings strings)
            throws Exception {
        DatabaseExporter db = new DBExporterAndImporterFactory().getExporter(databaseType);
        try (Connection connection = db.connectToDB(strings)) {
            db.createTables(connection);
            DatabaseUtil.removeDB(databaseName, connection, databaseContext);
        }

        DatabaseExporter exporter = new DBExporterAndImporterFactory().getExporter(databaseType);
        try (Connection connection = exporter.connectToDB(strings)) {
            exporter.createTables(connection);
            exporter.performExport(databaseContext,
                    databaseContext.getDatabase().getEntries(),
                    connection, databaseName);
        }

        DatabaseImporter importer = new DBExporterAndImporterFactory().getImporter(databaseType);
        try (Connection connection = importer.connectToDB(strings)) {
            List<DBImporterResult> results = importer.performImport(strings, Collections.singletonList(databaseName), databaseContext.getMode());
            assertEquals(1, results.size());
            assertEquals(databaseContext.getDatabase().getEntries(),
                    results.get(0).getDatabaseContext().getDatabase().getEntries());

            assertEquals(databaseContext.getMetaData().getGroups(), results.get(0).getDatabaseContext().getMetaData().getGroups());
        }
    }

    private BibDatabaseContext createContextWithSingleEntry() {
        BibEntry entry = new BibEntry("id1");
        entry.setCiteKey("einstein");
        entry.setType("article");
        entry.setField("author", "Albert Einstein");
        entry.setField("title", "Die grundlage der allgemeinen relativitätstheorie}");
        BibDatabase database = new BibDatabase();
        database.insertEntry(entry);
        BibDatabaseContext databaseContext = new BibDatabaseContext(database);
        return databaseContext;
    }

    private BibDatabaseContext createContextWithSingleEntryUsingQuoteSymbol() {
        BibEntry entry = new BibEntry("id1");
        entry.setCiteKey("einstein");
        entry.setType("article");
        entry.setField("author", "Albert L{\\'{u}}cia Einstein");
        entry.setField("title", "Die grundlage der allgemeinen relativitätstheorie}");
        BibDatabase database = new BibDatabase();
        database.insertEntry(entry);
        BibDatabaseContext databaseContext = new BibDatabaseContext(database);
        return databaseContext;
    }

    private BibDatabaseContext createContextWithSingleEntrySingleGroup() throws ParseException {
        BibDatabaseContext databaseContext = createContextWithSingleEntry();

        GroupTreeNode root = new GroupTreeNode(new AllEntriesGroup());
        KeywordGroup group = new KeywordGroup("test", "asdf", "fdas", false, true, GroupHierarchyType.INCLUDING);
        root.addSubgroup(group);
        databaseContext.getMetaData().setGroups(root);
        return databaseContext;
    }

}
