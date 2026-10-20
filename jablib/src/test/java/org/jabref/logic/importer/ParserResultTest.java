package org.jabref.logic.importer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

// Other tests for reading can be found at [org.jabref.logic.importer.fileformat.BibtexImporterTest]
@ResourceLock("Localization.lang")
class ParserResultTest {
    @Test
    void isEmptyForNewParseResult() {
        ParserResult empty = new ParserResult();
        assertTrue(empty.isEmpty());
    }

    @Test
    void isNotEmptyForBibDatabaseWithOneEntry() {
        BibEntry bibEntry = new BibEntry();
        BibDatabase bibDatabase = new BibDatabase(List.of(bibEntry));
        ParserResult parserResult = new ParserResult(bibDatabase);
        assertFalse(parserResult.isEmpty());
    }

    @Test
    void getDatabaseContextReturnsSameContextOnEveryCall() {
        ParserResult parserResult = new ParserResult();
        assertSame(parserResult.getDatabaseContext(), parserResult.getDatabaseContext());
    }

    @Test
    void pathSetOnParserResultIsSeenByContext() {
        ParserResult parserResult = new ParserResult();
        BibDatabaseContext context = parserResult.getDatabaseContext();
        Path path = Path.of("library.bib");

        parserResult.setPath(path);

        assertEquals(Optional.of(path), context.getDatabasePath());
    }

    @Test
    void pathSetOnContextIsSeenByParserResult() {
        ParserResult parserResult = new ParserResult();
        Path path = Path.of("library.bib");

        parserResult.getDatabaseContext().setDatabasePath(path);

        assertEquals(Optional.of(path), parserResult.getPath());
    }

    @Test
    void setMetaDataKeepsDatabaseAndPath() {
        BibDatabase database = new BibDatabase();
        ParserResult parserResult = new ParserResult(database);
        Path path = Path.of("library.bib");
        parserResult.setPath(path);
        MetaData metaData = new MetaData();

        parserResult.setMetaData(metaData);

        BibDatabaseContext context = parserResult.getDatabaseContext();
        assertSame(database, context.getDatabase());
        assertSame(metaData, context.getMetaData());
        assertEquals(Optional.of(path), context.getDatabasePath());
    }

    @Test
    void setDatabaseContextIsReturnedByGetDatabaseContext() {
        ParserResult parserResult = new ParserResult();
        BibDatabaseContext context = new BibDatabaseContext();

        parserResult.setDatabaseContext(context);

        assertSame(context, parserResult.getDatabaseContext());
    }

    @Test
    void warningsAddedMatchErrorMessage() {
        ParserResult parserResult = new ParserResult();
        parserResult.addWarning("Warning 1 ");
        parserResult.addWarning("Warning 2 ");
        assertEquals("Warning 1 \nWarning 2 ", parserResult.getErrorMessage());
    }

    @Test
    void hasEmptyMessageForNoWarnings() {
        ParserResult parserResult = new ParserResult();
        assertEquals("", parserResult.getErrorMessage());
    }

    @Test
    void doesNotHaveDuplicateWarnings() {
        ParserResult parserResult = new ParserResult();
        parserResult.addWarning("Duplicate Warning");
        parserResult.addWarning("Duplicate Warning");
        assertEquals("Duplicate Warning", parserResult.getErrorMessage());
    }

    @Test
    void warningAddedForWhitespaceInCitationKeyImport(@TempDir Path tmpDir) throws IOException {
        // whitespace after citation key "myArticle "
        String bibtexEntry = """
                @article{ myArticle ,
                  author    = "Author Name",
                  title     = "Title of the Article",
                  journal   = "Journal Name",
                  year      = "2024",
                  pages     = "1-10",
                  publisher = "Publisher Name"
                }
                """;
        Path tempFile = tmpDir.resolve("invalidBibTex.bib");
        Files.write(tempFile, bibtexEntry.getBytes());
        ParserResult parserResult = OpenDatabase.loadDatabase(tempFile, mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS), new DummyFileUpdateMonitor());
        assertEquals("Line 1: Found corrupted citation key  (contains whitespaces).", parserResult.warnings().getFirst());
    }

    @Test
    void warningAddedForMissingCommaInCitationKeyImport(@TempDir Path tmpDir) throws IOException {
        // Comma replaced by whitespace instead in citation key "myArticle "
        String bibtexEntry = """
                @article{myArticle\s
                   author    = "Author Name",
                   title     = "Title of the Article",
                   journal   = "Journal Name",
                   year      = "2024",
                   pages     = "1-10",
                   publisher = "Publisher Name"
                 }
                """;
        Path tempFile = tmpDir.resolve("invalidBibTex.bib");
        Files.write(tempFile, bibtexEntry.getBytes());
        ParserResult parserResult = OpenDatabase.loadDatabase(tempFile, mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS), new DummyFileUpdateMonitor());
        assertEquals("Line 1: Found corrupted citation key  (comma missing).", parserResult.warnings().getFirst());
    }

    @Test
    void warningAddedForCorruptedCitationKeyInImport(@TempDir Path tmpDir) throws IOException {
        String bibtexEntry = """
                @article{myArticle
                   author    = "Author Name",
                   title     = "Title of the Article",
                   journal   = "Journal Name",
                   year      = "2024",
                   pages     = "1-10",
                   publisher = "Publisher Name"
                 }
                """;

        Path tempFile = tmpDir.resolve("invalidBibTex.bib");
        Files.write(tempFile, bibtexEntry.getBytes());
        ParserResult parserResult = OpenDatabase.loadDatabase(tempFile, mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS), new DummyFileUpdateMonitor());
        assertEquals("Line 2: Found corrupted citation key .", parserResult.warnings().getFirst());
    }

    @Test
    void skipsImportEntryForImproperSyntax(@TempDir Path tmpDir) throws IOException {
        // Comma after '=' character on line 2 throws error
        String bibtexEntry = """
                @article{myArticle,
                   author    =, "Author Name",
                   title     = "Title of the Article",
                   journal   = "Journal Name",
                   year      = "2024",
                   pages     = "1-10",
                   publisher = "Publisher Name"
                 }
                """;
        Path tempFile = tmpDir.resolve("invalidBibTex.bib");
        Files.write(tempFile, bibtexEntry.getBytes());
        ParserResult parserResult = OpenDatabase.loadDatabase(tempFile, mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS), new DummyFileUpdateMonitor());
        assertFalse(parserResult.getDatabase().hasEntries());
    }
}
