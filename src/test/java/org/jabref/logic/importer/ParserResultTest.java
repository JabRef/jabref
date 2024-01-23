package org.jabref.logic.importer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

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
    public void warningsAddedMatchErrorMessage() {
        ParserResult parserResult = new ParserResult();
        parserResult.addWarning("Warning 1 ");
        parserResult.addWarning("Warning 2 ");
        assertEquals("Warning 1 \nWarning 2 ", parserResult.getErrorMessage());
    }

    @Test
    public void hasEmptyMessageForNoWarnings() {
        ParserResult parserResult = new ParserResult();
        assertEquals("", parserResult.getErrorMessage());
    }

    @Test
    public void doesNotHaveDuplicateWarnings() {
        ParserResult parserResult = new ParserResult();
        parserResult.addWarning("Duplicate Warning");
        parserResult.addWarning("Duplicate Warning");
        assertEquals("Duplicate Warning", parserResult.getErrorMessage());
    }

    @Test
    public void warningAddedForWhitespaceInCitationKeyImport(@TempDir Path tmpDir) throws IOException {
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
        File tempFile = new File(tmpDir.toFile(), "invalidBibTex.bib");
        Files.write(tempFile.toPath(), bibtexEntry.getBytes());
        ParserResult parserResult = OpenDatabase.loadDatabase(tempFile.toPath(), mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS), new DummyFileUpdateMonitor());
        assertEquals(parserResult.warnings().getFirst(), "Line 1: Found corrupted citation key  (contains whitespaces).");
    }

    @Test
    public void warningAddedForMissingCommaInCitationKeyImport(@TempDir Path tmpDir) throws IOException {
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
        File tempFile = new File(tmpDir.toFile(), "invalidBibTex.bib");
        Files.write(tempFile.toPath(), bibtexEntry.getBytes());
        ParserResult parserResult = OpenDatabase.loadDatabase(tempFile.toPath(), mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS), new DummyFileUpdateMonitor());
        assertEquals(parserResult.warnings().getFirst(), "Line 1: Found corrupted citation key  (comma missing).");
    }

    @Test
    public void warningAddedForCorruptedCitationKeyInImport(@TempDir Path tmpDir) throws IOException {
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

        File tempFile = new File(tmpDir.toFile(), "invalidBibTex.bib");
        Files.write(tempFile.toPath(), bibtexEntry.getBytes());
        ParserResult parserResult = OpenDatabase.loadDatabase(tempFile.toPath(), mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS), new DummyFileUpdateMonitor());
        assertEquals(parserResult.warnings().getFirst(), "Line 2: Found corrupted citation key .");
    }

    @Test
    public void skipsImportEntryForImproperSyntax(@TempDir Path tmpDir) throws IOException {
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
        File tempFile = new File(tmpDir.toFile(), "invalidBibTex.bib");
        Files.write(tempFile.toPath(), bibtexEntry.getBytes());
        ParserResult parserResult = OpenDatabase.loadDatabase(tempFile.toPath(), mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS), new DummyFileUpdateMonitor());
        assertFalse(parserResult.getDatabase().hasEntries());
    }
}
