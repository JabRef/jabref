package org.jabref.logic.search;

import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.search.SearchFlags;
import org.jabref.model.search.SearchQuery;
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

class DatabaseSearcherWithBibFilesTest {

    private static final BibEntry TITLE_SENTENCE_CASED = new BibEntry(StandardEntryType.Misc)
            .withCitationKey("title-sentence-cased")
            .withField(StandardField.TITLE, "Title Sentence Cased");
    private static final BibEntry TITLE_MIXED_CASED = new BibEntry(StandardEntryType.Misc)
            .withCitationKey("title-mixed-cased")
            .withField(StandardField.TITLE, "TiTle MiXed CaSed");
    private static final BibEntry TITLE_UPPER_CASED = new BibEntry(StandardEntryType.Misc)
            .withCitationKey("title-upper-cased")
            .withField(StandardField.TITLE, "TITLE UPPER CASED");

    private static final BibEntry MINIMAL_SENTENCE_CASE = new BibEntry(StandardEntryType.Misc)
            .withCitationKey("minimal-sentence-case")
            .withFiles(Collections.singletonList(new LinkedFile("", "minimal-sentence-case.pdf", StandardFileType.PDF.getName())));
    private static final BibEntry MINIMAL_ALL_UPPER_CASE = new BibEntry(StandardEntryType.Misc)
            .withCitationKey("minimal-all-upper-case")
            .withFiles(Collections.singletonList(new LinkedFile("", "minimal-all-upper-case.pdf", StandardFileType.PDF.getName())));
    private static final BibEntry MINIMAL_MIXED_CASE = new BibEntry(StandardEntryType.Misc)
            .withCitationKey("minimal-mixed-case")
            .withFiles(Collections.singletonList(new LinkedFile("", "minimal-mixed-case.pdf", StandardFileType.PDF.getName())));
    private static final BibEntry MINIMAL_NOTE_SENTENCE_CASE = new BibEntry(StandardEntryType.Misc)
            .withCitationKey("minimal-note-sentence-case")
            .withFiles(Collections.singletonList(new LinkedFile("", "minimal-note-sentence-case.pdf", StandardFileType.PDF.getName())));
    private static final BibEntry MINIMAL_NOTE_ALL_UPPER_CASE = new BibEntry(StandardEntryType.Misc)
            .withCitationKey("minimal-note-all-upper-case")
            .withFiles(Collections.singletonList(new LinkedFile("", "minimal-note-all-upper-case.pdf", StandardFileType.PDF.getName())));
    private static final BibEntry MINIMAL_NOTE_MIXED_CASE = new BibEntry(StandardEntryType.Misc)
            .withCitationKey("minimal-note-mixed-case")
            .withFiles(Collections.singletonList(new LinkedFile("", "minimal-note-mixed-case.pdf", StandardFileType.PDF.getName())));

    private static final FilePreferences FILE_PREFERENCES = mock(FilePreferences.class);
    @TempDir
    private Path indexDir;

    private BibDatabaseContext initializeDatabaseFromPath(String testFile) throws Exception {
        return initializeDatabaseFromPath(Path.of(Objects.requireNonNull(DatabaseSearcherWithBibFilesTest.class.getResource(testFile)).toURI()));
    }

    private BibDatabaseContext initializeDatabaseFromPath(Path testFile) throws Exception {
        ParserResult result = new BibtexImporter(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS), new DummyFileUpdateMonitor()).importDatabase(testFile);
        BibDatabaseContext databaseContext = result.getDatabaseContext();

        BibDatabaseContext context = mock(BibDatabaseContext.class);
        when(context.getFileDirectories(Mockito.any())).thenReturn(List.of(testFile.getParent()));
        when(context.getFulltextIndexPath()).thenReturn(indexDir);
        when(FILE_PREFERENCES.shouldFulltextIndexLinkedFiles()).thenReturn(true);

        return databaseContext;
    }

    private static Stream<Arguments> searchLibrary() {
        return Stream.of(
                // empty library
                Arguments.of(List.of(), "empty.bib", "Test", EnumSet.noneOf(SearchFlags.class)),

                // test-library-title-casing

                Arguments.of(List.of(), "test-library-title-casing.bib", "NotExisting", EnumSet.noneOf(SearchFlags.class)),
                Arguments.of(List.of(TITLE_SENTENCE_CASED, TITLE_MIXED_CASED, TITLE_UPPER_CASED), "test-library-title-casing.bib", "Title", EnumSet.noneOf(SearchFlags.class)),

                Arguments.of(List.of(), "test-library-title-casing.bib", "title=NotExisting", EnumSet.noneOf(SearchFlags.class)),
                Arguments.of(List.of(TITLE_SENTENCE_CASED, TITLE_MIXED_CASED, TITLE_UPPER_CASED), "test-library-title-casing.bib", "title=Title", EnumSet.noneOf(SearchFlags.class)),

                // Arguments.of(List.of(), "test-library-title-casing.bib", "title=TiTLE", EnumSet.of(SearchFlags.CASE_SENSITIVE)),
                // Arguments.of(List.of(titleSentenceCased), "test-library-title-casing.bib", "title=Title", EnumSet.of(SearchFlags.CASE_SENSITIVE)),

                // Arguments.of(List.of(), "test-library-title-casing.bib", "TiTLE", EnumSet.of(SearchFlags.CASE_SENSITIVE)),
                // Arguments.of(List.of(titleMixedCased), "test-library-title-casing.bib", "TiTle", EnumSet.of(SearchFlags.CASE_SENSITIVE)),

                // Arguments.of(List.of(), "test-library-title-casing.bib", "title=NotExisting", EnumSet.of(SearchFlags.CASE_SENSITIVE)),
                // Arguments.of(List.of(titleMixedCased), "test-library-title-casing.bib", "title=TiTle", EnumSet.of(SearchFlags.CASE_SENSITIVE)),

                Arguments.of(List.of(), "test-library-title-casing.bib", "[Y]", EnumSet.of(SearchFlags.REGULAR_EXPRESSION)),
                Arguments.of(List.of(TITLE_UPPER_CASED), "test-library-title-casing.bib", "[U]", EnumSet.of(SearchFlags.REGULAR_EXPRESSION)),

                // Word boundaries
                // Arguments.of(List.of(), "test-library-title-casing.bib", "\\bTit\\b", EnumSet.of(SearchFlags.REGULAR_EXPRESSION, SearchFlags.CASE_SENSITIVE)),
                // Arguments.of(List.of(titleSentenceCased), "test-library-title-casing.bib", "\\bTitle\\b", EnumSet.of(SearchFlags.REGULAR_EXPRESSION, SearchFlags.CASE_SENSITIVE)),

                // test-library-with-attached-files

                // Arguments.of(List.of(), "test-library-with-attached-files.bib", "This is a test.", EnumSet.of(SearchFlags.FULLTEXT, SearchFlags.CASE_SENSITIVE)),

                Arguments.of(List.of(MINIMAL_SENTENCE_CASE, MINIMAL_ALL_UPPER_CASE, MINIMAL_MIXED_CASE), "test-library-with-attached-files.bib", "This is a short sentence, comma included.", EnumSet.of(SearchFlags.FULLTEXT)),
                Arguments.of(List.of(MINIMAL_SENTENCE_CASE, MINIMAL_ALL_UPPER_CASE, MINIMAL_MIXED_CASE), "test-library-with-attached-files.bib", "comma", EnumSet.of(SearchFlags.FULLTEXT)),
                // TODO: PDF search does not support case sensitive search (yet)
                // Arguments.of(List.of(minimalAllUpperCase, minimalMixedCase), "test-library-with-attached-files.bib", "THIS", EnumSet.of(SearchFlags.FULLTEXT, SearchFlags.CASE_SENSITIVE)),
                // Arguments.of(List.of(minimalAllUpperCase), "test-library-with-attached-files.bib", "THIS is a short sentence, comma included.", EnumSet.of(SearchFlags.FULLTEXT, SearchFlags.CASE_SENSITIVE)),
                // Arguments.of(List.of(minimalSentenceCase, minimalAllUpperCase, minimalMixedCase), "test-library-with-attached-files.bib", "comma", EnumSet.of(SearchFlags.FULLTEXT, SearchFlags.CASE_SENSITIVE)),
                // Arguments.of(List.of(minimalNoteAllUpperCase), "test-library-with-attached-files.bib", "THIS IS A SHORT SENTENCE, COMMA INCLUDED.", EnumSet.of(SearchFlags.FULLTEXT, SearchFlags.CASE_SENSITIVE)),

                Arguments.of(List.of(), "test-library-with-attached-files.bib", "NotExisting", EnumSet.of(SearchFlags.FULLTEXT)),

                Arguments.of(List.of(MINIMAL_NOTE_SENTENCE_CASE, MINIMAL_NOTE_ALL_UPPER_CASE, MINIMAL_NOTE_MIXED_CASE), "test-library-with-attached-files.bib", "world", EnumSet.of(SearchFlags.FULLTEXT)),
                Arguments.of(List.of(MINIMAL_NOTE_SENTENCE_CASE, MINIMAL_NOTE_ALL_UPPER_CASE, MINIMAL_NOTE_MIXED_CASE), "test-library-with-attached-files.bib", "Hello World", EnumSet.of(SearchFlags.FULLTEXT))
                // TODO: PDF search does not support case sensitive search (yet)
                // Arguments.of(List.of(minimalNoteAllUpperCase), "test-library-with-attached-files.bib", "HELLO WORLD", EnumSet.of(SearchFlags.FULLTEXT, SearchFlags.CASE_SENSITIVE)),
                // Arguments.of(List.of(), "test-library-with-attached-files.bib", "NotExisting", EnumSet.of(SearchFlags.FULLTEXT, SearchFlags.CASE_SENSITIVE))
        );
    }

    @ParameterizedTest(name = "{index} => query={2}, searchFlags={3}, testFile={1}, expected={0}")
    @MethodSource
    void searchLibrary(List<BibEntry> expected, String testFile, String query, EnumSet<SearchFlags> searchFlags) throws Exception {
        BibDatabaseContext databaseContext = initializeDatabaseFromPath(testFile);
        List<BibEntry> matches = new DatabaseSearcher(new SearchQuery(query, searchFlags), databaseContext, FILE_PREFERENCES).getMatches();
        assertEquals(expected, matches);
    }
}


