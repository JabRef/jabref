package net.sf.jabref.sql;

import net.sf.jabref.BibDatabaseContext;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.bibtex.BibtexEntryAssert;
import net.sf.jabref.groups.GroupTreeNode;
import net.sf.jabref.groups.structure.GroupHierarchyType;
import net.sf.jabref.groups.structure.KeywordGroup;
import net.sf.jabref.logic.CustomEntryTypesManager;
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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.Collections;
import java.util.List;

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

    private void testDatabaseExport(BibDatabaseContext databaseContext, DatabaseType databaseType, String databaseName, DBStrings strings) throws Exception {
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
            BibtexEntryAssert.assertEquals(databaseContext.getDatabase().getEntries(),
                    results.get(0).getDatabaseContext().getDatabase().getEntries());

            assertEquals(databaseContext.getMetaData().getGroups(), results.get(0).getDatabaseContext().getMetaData().getGroups());
        }
    }

    @Test
    public void testExportToFileSingleEntry() throws Exception {
        BibDatabaseContext databaseContext = createContextWithSingleEntry();
        for (DatabaseType databaseType : DatabaseType.values()) {
            testExportToFile(databaseContext, "src/test/resources/net/sf/jabref/sql/database-export-single-entry.sql", databaseType);
        }
    }

    @Test
    public void testExportToFileSingleEntrySingleGroup() throws Exception {
        BibDatabaseContext databaseContext = createContextWithSingleEntrySingleGroup();
        for (DatabaseType databaseType : DatabaseType.values()) {
            testExportToFile(databaseContext, "src/test/resources/net/sf/jabref/sql/database-export-single-entry-single-group.sql", databaseType);
        }
    }

    private void testExportToFile(BibDatabaseContext databaseContext, String path, DatabaseType databaseType) throws Exception {
        DatabaseExporter exporter = new DBExporterAndImporterFactory().getExporter(databaseType);

        Path tempFile = Files.createTempFile("jabref", "database-export" + databaseType.getFormattedName());
        exporter.exportDatabaseAsFile(databaseContext,
                databaseContext.getDatabase().getEntries(),
                tempFile.toAbsolutePath().toString(),
                StandardCharsets.UTF_8);

        Path expectSqlFile = Paths.get(path);
        assertEquals(
                String.join("\n", Files.readAllLines(expectSqlFile, StandardCharsets.UTF_8)),
                String.join("\n", Files.readAllLines(tempFile, StandardCharsets.UTF_8))
        );
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

    private BibDatabaseContext createContextWithSingleEntrySingleGroup() {
        BibDatabaseContext databaseContext = createContextWithSingleEntry();
        databaseContext.getMetaData().setGroups(new GroupTreeNode(new KeywordGroup("test", "asdf", "fdas", false, true, GroupHierarchyType.INCLUDING)));
        return databaseContext;
    }

}
