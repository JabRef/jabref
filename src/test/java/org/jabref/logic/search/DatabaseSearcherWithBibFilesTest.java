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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DatabaseSearcherWithBibFilesTest {

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

    private static BibEntry mininimalSentenceCase = new BibEntry(StandardEntryType.Misc)
            .withCitationKey("minimal-sentence-case")
            .withFiles(Collections.singletonList(new LinkedFile("", "minimal-sentence-case.pdf", StandardFileType.PDF.getName())));
    private static BibEntry minimalAllUpperCase = new BibEntry(StandardEntryType.Misc)
            .withCitationKey("minimal-all-upper-case")
            .withFiles(Collections.singletonList(new LinkedFile("", "minimal-all-upper-case.pdf", StandardFileType.PDF.getName())));
    private static BibEntry minimalMixedCase = new BibEntry(StandardEntryType.Misc)
            .withCitationKey("minimal-mixed-case")
            .withFiles(Collections.singletonList(new LinkedFile("", "minimal-mixed-case.pdf", StandardFileType.PDF.getName())));
    private static BibEntry minimalNoteSentenceCase = new BibEntry(StandardEntryType.Misc)
            .withCitationKey("minimal-note-sentence-case")
            .withFiles(Collections.singletonList(new LinkedFile("", "minimal-note-sentence-case.pdf", StandardFileType.PDF.getName())));
    private static BibEntry minimalNoteAllUpperCase = new BibEntry(StandardEntryType.Misc)
            .withCitationKey("minimal-note-all-upper-case")
            .withFiles(Collections.singletonList(new LinkedFile("", "minimal-note-all-upper-case.pdf", StandardFileType.PDF.getName())));
    private static BibEntry minimalNoteMixedCase = new BibEntry(StandardEntryType.Misc)
            .withCitationKey("minimal-note-mixed-case")
            .withFiles(Collections.singletonList(new LinkedFile("", "minimal-note-mixed-case.pdf", StandardFileType.PDF.getName())));

    FilePreferences filePreferences = mock(FilePreferences.class);

    @TempDir
    private Path indexDir;
    private PdfIndexer pdfIndexer;

    private BibDatabase initializeDatabaseFromPath(String testFile) throws Exception {
        return initializeDatabaseFromPath(Path.of(Objects.requireNonNull(DatabaseSearcherWithBibFilesTest.class.getResource(testFile)).toURI()));
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
        // Alternative - For debugging with Luke (part of the Apache Lucene distribution)
        // pdfIndexer = PdfIndexer.of(context, Path.of("C:\\temp\\index"), filePreferences);

        pdfIndexer.rebuildIndex();
        return database;
    }

    @AfterEach
    public void tearDown() throws Exception {
        pdfIndexer.close();
    }

    private static Stream<Arguments> searchLibrary() {
        return Stream.of(
                // empty library
                Arguments.of(List.of(), "empty.bib", "Test", EnumSet.noneOf(SearchRules.SearchFlags.class)),

                // test-library-A

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

                // test-library-B

                Arguments.of(List.of(), "test-library-B.bib", "[/8]", EnumSet.of(SearchRules.SearchFlags.REGULAR_EXPRESSION)),
                Arguments.of(List.of(entry4B), "test-library-B.bib", "[/9]", EnumSet.of(SearchRules.SearchFlags.REGULAR_EXPRESSION)),

                Arguments.of(List.of(), "test-library-B.bib", "\\bCas\\b", EnumSet.of(SearchRules.SearchFlags.REGULAR_EXPRESSION, SearchRules.SearchFlags.CASE_SENSITIVE)),
                Arguments.of(List.of(entry1B), "test-library-B.bib", "\\bCase\\b", EnumSet.of(SearchRules.SearchFlags.REGULAR_EXPRESSION, SearchRules.SearchFlags.CASE_SENSITIVE)),

                // test-library-with-attached-files

                Arguments.of(List.of(), "test-library-with-attached-files.bib", "This is a test.", EnumSet.of(SearchRules.SearchFlags.FULLTEXT, SearchRules.SearchFlags.CASE_SENSITIVE)),

                Arguments.of(List.of(mininimalSentenceCase, minimalAllUpperCase, minimalMixedCase), "test-library-with-attached-files.bib", "This is a short sentence, comma included.", EnumSet.of(SearchRules.SearchFlags.FULLTEXT)),
                Arguments.of(List.of(mininimalSentenceCase, minimalAllUpperCase, minimalMixedCase), "test-library-with-attached-files.bib", "comma", EnumSet.of(SearchRules.SearchFlags.FULLTEXT)),
                // TODO: PDF search does not support case sensitive search (yet)
                // Arguments.of(List.of(minimalAllUpperCase, minimalMixedCase), "test-library-with-attached-files.bib", "THIS", EnumSet.of(SearchRules.SearchFlags.FULLTEXT, SearchRules.SearchFlags.CASE_SENSITIVE)),
                // Arguments.of(List.of(minimalAllUpperCase), "test-library-with-attached-files.bib", "THIS is a short sentence, comma included.", EnumSet.of(SearchRules.SearchFlags.FULLTEXT, SearchRules.SearchFlags.CASE_SENSITIVE)),
                // Arguments.of(List.of(minimalSentenceCase, minimalAllUpperCase, minimalMixedCase), "test-library-with-attached-files.bib", "comma", EnumSet.of(SearchRules.SearchFlags.FULLTEXT, SearchRules.SearchFlags.CASE_SENSITIVE)),
                // Arguments.of(List.of(minimalNoteAllUpperCase), "test-library-with-attached-files.bib", "THIS IS A SHORT SENTENCE, COMMA INCLUDED.", EnumSet.of(SearchRules.SearchFlags.FULLTEXT, SearchRules.SearchFlags.CASE_SENSITIVE)),

                Arguments.of(List.of(), "test-library-with-attached-files.bib", "NotExisting", EnumSet.of(SearchRules.SearchFlags.FULLTEXT)),

                Arguments.of(List.of(minimalNoteSentenceCase, minimalNoteAllUpperCase, minimalNoteMixedCase), "test-library-with-attached-files.bib", "world", EnumSet.of(SearchRules.SearchFlags.FULLTEXT)),
                Arguments.of(List.of(minimalNoteSentenceCase, minimalNoteAllUpperCase, minimalNoteMixedCase), "test-library-with-attached-files.bib", "Hello World", EnumSet.of(SearchRules.SearchFlags.FULLTEXT)),
                // TODO: PDF search does not support case sensitive search (yet)
                // Arguments.of(List.of(minimalNoteAllUpperCase), "test-library-with-attached-files.bib", "HELLO WORLD", EnumSet.of(SearchRules.SearchFlags.FULLTEXT, SearchRules.SearchFlags.CASE_SENSITIVE)),
                Arguments.of(List.of(), "test-library-with-attached-files.bib", "NotExisting", EnumSet.of(SearchRules.SearchFlags.FULLTEXT, SearchRules.SearchFlags.CASE_SENSITIVE))
        );
    }

    @ParameterizedTest(name = "{index} => query={2}, searchFlags={3}, testFile={1}, expected={0}")
    @MethodSource
    public void searchLibrary(List<BibEntry> expected, String testFile, String query, EnumSet<SearchRules.SearchFlags> searchFlags) throws Exception {
        BibDatabase database = initializeDatabaseFromPath(testFile);
        List<BibEntry> matches = new DatabaseSearcher(new SearchQuery(query, searchFlags), database).getMatches();
        assertEquals(expected, matches);
    }
}


