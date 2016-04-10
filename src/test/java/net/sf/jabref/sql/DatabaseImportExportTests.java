package net.sf.jabref.sql;

import net.sf.jabref.BibDatabaseContext;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.bibtex.BibtexEntryAssert;
import net.sf.jabref.groups.GroupTreeNode;
import net.sf.jabref.groups.structure.GroupHierarchyType;
import net.sf.jabref.groups.structure.KeywordGroup;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.sql.DBExporterAndImporterFactory;
import net.sf.jabref.sql.DatabaseType;
import net.sf.jabref.sql.exporter.DatabaseExporter;
import net.sf.jabref.sql.importer.DatabaseImporter;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class DatabaseImportExportTests {

    @BeforeClass
    public static void setUp() {
        Globals.prefs = JabRefPreferences.getInstance();
    }

    @Test
    public void testExportToFileSingleEntry() throws Exception {
        BibDatabaseContext databaseContext = createContextWithSingleEntry();
        for (DatabaseType databaseType : DatabaseType.values()) {
            DatabaseExporter exporter = new DBExporterAndImporterFactory().getExporter(databaseType);
            DatabaseImporter importer = new DBExporterAndImporterFactory().getImporter(databaseType);

            Path tempFile = Files.createTempFile("jabref", "database-export" + databaseType.getFormattedName());
            exporter.exportDatabaseAsFile(databaseContext,
                    databaseContext.getDatabase().getEntries(),
                    tempFile.toAbsolutePath().toString(),
                    StandardCharsets.UTF_8);

            Path expectSqlFile = Paths.get("src/test/resources/net/sf/jabref/sql/database-export-single-entry-" + databaseType.getFormattedName() + ".sql");
            assertEquals(
                    String.join("\n", Files.readAllLines(expectSqlFile, StandardCharsets.UTF_8)),
                    String.join("\n", Files.readAllLines(tempFile, StandardCharsets.UTF_8))
            );
        }
    }

    @Test
    public void testExportToFileSingleEntrySingleGroup() throws Exception {
        BibDatabaseContext databaseContext = createContextWithSingleEntrySingleGroup();
        for (DatabaseType databaseType : DatabaseType.values()) {
            DatabaseExporter exporter = new DBExporterAndImporterFactory().getExporter(databaseType);
            DatabaseImporter importer = new DBExporterAndImporterFactory().getImporter(databaseType);

            Path tempFile = Files.createTempFile("jabref", "database-export" + databaseType.getFormattedName());
            exporter.exportDatabaseAsFile(databaseContext,
                    databaseContext.getDatabase().getEntries(),
                    tempFile.toAbsolutePath().toString(),
                    StandardCharsets.UTF_8);

            Path expectSqlFile = Paths.get("src/test/resources/net/sf/jabref/sql/database-export-single-entry-single-group-" + databaseType.getFormattedName() + ".sql");
            assertEquals(
                    String.join("\n", Files.readAllLines(expectSqlFile, StandardCharsets.UTF_8)),
                    String.join("\n", Files.readAllLines(tempFile, StandardCharsets.UTF_8))
            );
        }
    }

    private BibDatabaseContext createContextWithSingleEntry() {
        BibEntry entry = new BibEntry("id1");
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
