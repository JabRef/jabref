package org.jabref.logic.search;

import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.jabref.gui.Globals;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.logic.pdf.search.PdfIndexer;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.search.rules.SearchRules;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.preferences.FilePreferences;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SearchFunctionalityTest {

    private static BibEntry entry1A = new BibEntry(StandardEntryType.Misc)
            .withCitationKey("entry1")
            .withField(StandardField.AUTHOR, "Test")
            .withField(StandardField.TITLE, "cASe");
    private static BibEntry entry2A = new BibEntry(StandardEntryType.Misc)
            .withCitationKey("entry2")
            .withField(StandardField.AUTHOR, "test")
            .withField(StandardField.TITLE, "casE");
    private static BibEntry entry3A = new BibEntry(StandardEntryType.Misc)
            .withCitationKey("entry3")
            .withField(StandardField.AUTHOR, "tESt")
            .withField(StandardField.TITLE, "Case");
    private static BibEntry entry4A = new BibEntry(StandardEntryType.Misc)
            .withCitationKey("entry4")
            .withField(StandardField.AUTHOR, "tesT")
            .withField(StandardField.TITLE, "CASE");
    private static BibEntry entry5A = new BibEntry(StandardEntryType.Misc)
            .withCitationKey("entry5")
            .withField(StandardField.AUTHOR, "TEST")
            .withField(StandardField.TITLE, "case");

    private static BibEntry entry1B = new BibEntry(StandardEntryType.Misc)
            .withCitationKey("entry1")
            .withField(StandardField.AUTHOR, "Test")
            .withField(StandardField.TITLE, "Case");
    private static BibEntry entry4B = new BibEntry(StandardEntryType.Misc)
            .withCitationKey("entry4")
            .withField(StandardField.AUTHOR, "Special")
            .withField(StandardField.TITLE, "192? title.");

    private static BibEntry minimal1 = new BibEntry(StandardEntryType.Misc)
            .withCitationKey("minimal1")
            .withFiles(Collections.singletonList(new LinkedFile("Minimal 1", "minimal-1.pdf", StandardFileType.PDF.getName())));
    private static BibEntry minimal2 = new BibEntry(StandardEntryType.Misc)
            .withCitationKey("minimal2")
            .withFiles(Collections.singletonList(new LinkedFile("Minimal 2", "minimal-2.pdf", StandardFileType.PDF.getName())));
    private static BibEntry minimal3 = new BibEntry(StandardEntryType.Misc)
            .withCitationKey("minimal3")
            .withFiles(Collections.singletonList(new LinkedFile("Minimal 3", "minimal-3.pdf", StandardFileType.PDF.getName())));
    private static BibEntry minimalNote1 = new BibEntry(StandardEntryType.Misc)
            .withCitationKey("minimal-note1")
            .withFiles(Collections.singletonList(new LinkedFile("Minimalnote 1", "minimal-note-1.pdf", StandardFileType.PDF.getName())));
    private static BibEntry minimalNote2 = new BibEntry(StandardEntryType.Misc)
            .withCitationKey("minimal-note2")
            .withFiles(Collections.singletonList(new LinkedFile("Minimalnote 2", "minimal-note-2.pdf", StandardFileType.PDF.getName())));
    private static BibEntry minimalNote3 = new BibEntry(StandardEntryType.Misc)
            .withCitationKey("minimal-note3")
            .withFiles(Collections.singletonList(new LinkedFile("Minimalnote 3", "minimal-note-3.pdf", StandardFileType.PDF.getName())));

    FilePreferences filePreferences = mock(FilePreferences.class);

    @TempDir
    private Path indexDir;

    private BibDatabase initializeDatabaseFromPath(String testFile) throws Exception {
        return initializeDatabaseFromPath(Path.of(Objects.requireNonNull(SearchFunctionalityTest.class.getResource(testFile)).toURI()));
    }

    private BibDatabase initializeDatabaseFromPath(Path testFile) throws Exception {
        ParserResult result = new BibtexImporter(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS), new DummyFileUpdateMonitor()).importDatabase(testFile);
        BibDatabase database = result.getDatabase();

        BibDatabaseContext context = mock(BibDatabaseContext.class);
        when(context.getFileDirectories(Mockito.any())).thenReturn(List.of(testFile.getParent()));
        when(context.getFulltextIndexPath()).thenReturn(indexDir);
        when(context.getDatabase()).thenReturn(database);
        when(context.getEntries()).thenReturn(database.getEntries());

        // Required because of {@Link org.jabref.model.search.rules.FullTextSearchRule.FullTextSearchRule
        Globals.stateManager.setActiveDatabase(context);

        PdfIndexer pdfIndexer = PdfIndexer.of(context, filePreferences);
        pdfIndexer.rebuildIndex();
        return database;
    }

    private static Stream<Arguments> searchLibrary() {
        return Stream.of(
                Arguments.of(List.of(), "empty.bib", "Test", EnumSet.noneOf(SearchRules.SearchFlags.class)),

                Arguments.of(List.of(), "test-library-A.bib", "Best", EnumSet.noneOf(SearchRules.SearchFlags.class)),
                Arguments.of(List.of(entry1A, entry2A, entry3A, entry4A, entry5A), "test-library-A.bib", "Test", EnumSet.noneOf(SearchRules.SearchFlags.class)),

                Arguments.of(List.of(), "test-library-A.bib", "author=Case", EnumSet.noneOf(SearchRules.SearchFlags.class)),
                Arguments.of(List.of(entry1A, entry2A, entry3A, entry4A, entry5A), "test-library-A.bib", "author=Test", EnumSet.noneOf(SearchRules.SearchFlags.class)),

                Arguments.of(List.of(), "test-library-A.bib", "author=Test and title=Test", EnumSet.noneOf(SearchRules.SearchFlags.class)),
                Arguments.of(List.of(entry1A, entry2A, entry3A, entry4A, entry5A), "test-library-A.bib", "author=Test and title=Case", EnumSet.noneOf(SearchRules.SearchFlags.class)),

                Arguments.of(List.of(), "test-library-A.bib", "TesT", EnumSet.of(SearchRules.SearchFlags.CASE_SENSITIVE)),
                Arguments.of(List.of(entry1A), "test-library-A.bib", "Test", EnumSet.of(SearchRules.SearchFlags.CASE_SENSITIVE)),

                Arguments.of(List.of(), "test-library-A.bib", "author=Test and title=case", EnumSet.of(SearchRules.SearchFlags.CASE_SENSITIVE)),
                Arguments.of(List.of(entry1A), "test-library-A.bib", "author=Test and title=cASe", EnumSet.of(SearchRules.SearchFlags.CASE_SENSITIVE)),

                Arguments.of(List.of(), "test-library-B.bib", "[/8]", EnumSet.of(SearchRules.SearchFlags.REGULAR_EXPRESSION)),
                Arguments.of(List.of(entry4B), "test-library-B.bib", "[/9]", EnumSet.of(SearchRules.SearchFlags.REGULAR_EXPRESSION)),

                Arguments.of(List.of(), "test-library-B.bib", "\\bCas\\b", EnumSet.of(SearchRules.SearchFlags.REGULAR_EXPRESSION, SearchRules.SearchFlags.CASE_SENSITIVE)),
                Arguments.of(List.of(entry1B), "test-library-B.bib", "\\bCase\\b", EnumSet.of(SearchRules.SearchFlags.REGULAR_EXPRESSION, SearchRules.SearchFlags.CASE_SENSITIVE)),

                Arguments.of(List.of(), "test-library-C.bib", "This is a test.", EnumSet.of(SearchRules.SearchFlags.FULLTEXT, SearchRules.SearchFlags.CASE_SENSITIVE)),

                // minimal1.pdf contains "THIS is a short sentence, comma included"
                // minimalNote1.pdf contains "THIS IS A SHORT SENTENCE, COMMA INCLUDED."
                Arguments.of(List.of(minimal1, minimalNote1), "test-library-C.bib", "This is a short sentence, comma included.", EnumSet.of(SearchRules.SearchFlags.FULLTEXT)),
                Arguments.of(List.of(minimal1, minimalNote1), "test-library-C.bib", "THIS", EnumSet.of(SearchRules.SearchFlags.FULLTEXT, SearchRules.SearchFlags.CASE_SENSITIVE)),
                Arguments.of(List.of(minimal1), "test-library-C.bib", "THIS is a short sentence, comma included.", EnumSet.of(SearchRules.SearchFlags.FULLTEXT, SearchRules.SearchFlags.CASE_SENSITIVE)),
                Arguments.of(List.of(minimal1), "test-library-C.bib", "comma", EnumSet.of(SearchRules.SearchFlags.FULLTEXT)),
                Arguments.of(List.of(minimal1), "test-library-C.bib", "comma", EnumSet.of(SearchRules.SearchFlags.FULLTEXT, SearchRules.SearchFlags.CASE_SENSITIVE)),
                Arguments.of(List.of(minimalNote1), "test-library-C.bib", "THIS IS A SHORT SENTENCE, COMMA INCLUDED.", EnumSet.of(SearchRules.SearchFlags.FULLTEXT, SearchRules.SearchFlags.CASE_SENSITIVE)),
                Arguments.of(List.of(), "test-library-C.bib", "This is a short sentence, comma included.", EnumSet.of(SearchRules.SearchFlags.FULLTEXT, SearchRules.SearchFlags.CASE_SENSITIVE)),

                Arguments.of(List.of(), "test-library-C.bib", "User Test", EnumSet.of(SearchRules.SearchFlags.FULLTEXT, SearchRules.SearchFlags.CASE_SENSITIVE)),

                // minimal-note1.pdf contains "HELLO WORLD" as pdf comment, which should be found
                Arguments.of(List.of(minimalNote1), "test-library-C.bib", "world", EnumSet.of(SearchRules.SearchFlags.FULLTEXT)),
                Arguments.of(List.of(minimalNote1), "test-library-C.bib", "Hello World", EnumSet.of(SearchRules.SearchFlags.FULLTEXT)),
                Arguments.of(List.of(minimalNote1), "test-library-C.bib", "HELLO WORLD", EnumSet.of(SearchRules.SearchFlags.FULLTEXT, SearchRules.SearchFlags.CASE_SENSITIVE)),
                Arguments.of(List.of(), "test-library-C.bib", "Hello World", EnumSet.of(SearchRules.SearchFlags.FULLTEXT, SearchRules.SearchFlags.CASE_SENSITIVE))
        );
    }

    @ParameterizedTest
    @MethodSource
    public void searchLibrary(List<BibEntry> expected, String testFile, String query, EnumSet<SearchRules.SearchFlags> searchFlags) throws Exception {
        BibDatabase database = initializeDatabaseFromPath(testFile);
        List<BibEntry> matches = new DatabaseSearcher(new SearchQuery(query, searchFlags), database).getMatches();
        assertEquals(expected, matches);
    }

    /*
    private static Stream<Arguments> testSimplePDFFulltextSearch() {
        return Stream.of()
    }

            /*    @Test
    public void testSimplePDFNoteFulltextSearch() throws Exception {
        initializeDatabaseFromPath(Path.of(Objects.requireNonNull(SearchFunctionalityTest.class.getResource("test-library-C.bib");

        //@Test uses PDFReader

        //Positive search test
        PdfSearchResults resultsPositive = search.search("Hello World", 10);
        assertEquals(3, resultsPositive.numSearchResults());
        //Negative search test
        PdfSearchResults resultsNegative = search.search("User Test", 10);
        assertEquals(0, resultsNegative.numSearchResults());
    }


    @ParameterizedTest
    @MethodSource
    public void testSimplePDFFulltextSearch(List<BibEntry> expected, String query, EnumSet<SearchRules.SearchFlags> searchFlags) throws Exception {
        BibDatabase bibDatabase = initializeDatabaseFromPath("test-library-C.bib");

        //Positive search test
        PdfSearchResults resultsPositive = search.search("This is a short sentence, comma included.", 10);
        assertEquals(3, resultsPositive.numSearchResults());
        //Negative search test
        PdfSearchResults resultsNegative = search.search("This is a test.", 10);
        assertEquals(0, resultsNegative.numSearchResults());
    }
*/
}


