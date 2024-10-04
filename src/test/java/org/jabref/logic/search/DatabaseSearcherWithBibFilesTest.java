package org.jabref.logic.search;

import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import javafx.beans.property.SimpleBooleanProperty;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.search.SearchFlags;
import org.jabref.model.search.SearchQuery;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class DatabaseSearcherWithBibFilesTest {
    private static final TaskExecutor TASK_EXECUTOR = new CurrentThreadTaskExecutor();
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
            .withFiles(List.of(new LinkedFile("", "minimal-sentence-case.pdf", StandardFileType.PDF.getName())));
    private static final BibEntry MINIMAL_ALL_UPPER_CASE = new BibEntry(StandardEntryType.Misc)
            .withCitationKey("minimal-all-upper-case")
            .withFiles(List.of(new LinkedFile("", "minimal-all-upper-case.pdf", StandardFileType.PDF.getName())));
    private static final BibEntry MINIMAL_MIXED_CASE = new BibEntry(StandardEntryType.Misc)
            .withCitationKey("minimal-mixed-case")
            .withFiles(List.of(new LinkedFile("", "minimal-mixed-case.pdf", StandardFileType.PDF.getName())));
    private static final BibEntry MINIMAL_NOTE_SENTENCE_CASE = new BibEntry(StandardEntryType.Misc)
            .withCitationKey("minimal-note-sentence-case")
            .withFiles(List.of(new LinkedFile("", "minimal-note-sentence-case.pdf", StandardFileType.PDF.getName())));
    private static final BibEntry MINIMAL_NOTE_ALL_UPPER_CASE = new BibEntry(StandardEntryType.Misc)
            .withCitationKey("minimal-note-all-upper-case")
            .withFiles(List.of(new LinkedFile("", "minimal-note-all-upper-case.pdf", StandardFileType.PDF.getName())));
    private static final BibEntry MINIMAL_NOTE_MIXED_CASE = new BibEntry(StandardEntryType.Misc)
            .withCitationKey("minimal-note-mixed-case")
            .withFiles(List.of(new LinkedFile("", "minimal-note-mixed-case.pdf", StandardFileType.PDF.getName())));

    private final FilePreferences filePreferences = mock(FilePreferences.class);

    @TempDir
    private Path indexDir;

    private BibDatabaseContext initializeDatabaseFromPath(String testFile) throws Exception {
        return initializeDatabaseFromPath(Path.of(Objects.requireNonNull(DatabaseSearcherWithBibFilesTest.class.getResource(testFile)).toURI()));
    }

    private BibDatabaseContext initializeDatabaseFromPath(Path testFile) throws Exception {
        ParserResult result = new BibtexImporter(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS), new DummyFileUpdateMonitor()).importDatabase(testFile);
        BibDatabaseContext databaseContext = spy(result.getDatabaseContext());
        when(databaseContext.getFulltextIndexPath()).thenReturn(indexDir);

        when(filePreferences.shouldFulltextIndexLinkedFiles()).thenReturn(true);
        when(filePreferences.fulltextIndexLinkedFilesProperty()).thenReturn(new SimpleBooleanProperty(true));
        return databaseContext;
    }

    private static Stream<Arguments> searchLibrary() {
        return Stream.of(
                // empty library
                Arguments.of(List.of(), "empty.bib", "Test", EnumSet.noneOf(SearchFlags.class)),

                // test-library-title-casing
                Arguments.of(List.of(), "test-library-title-casing.bib", "NotExisting", EnumSet.noneOf(SearchFlags.class)),
                Arguments.of(List.of(TITLE_SENTENCE_CASED, TITLE_MIXED_CASED, TITLE_UPPER_CASED), "test-library-title-casing.bib", "Title", EnumSet.noneOf(SearchFlags.class)),

                Arguments.of(List.of(), "test-library-title-casing.bib", "title:NotExisting", EnumSet.noneOf(SearchFlags.class)),
                Arguments.of(List.of(TITLE_SENTENCE_CASED, TITLE_MIXED_CASED, TITLE_UPPER_CASED), "test-library-title-casing.bib", "title:Title", EnumSet.noneOf(SearchFlags.class)),

                // Arguments.of(List.of(), "test-library-title-casing.bib", "title:TiTLE", EnumSet.of(SearchFlags.CASE_SENSITIVE)),
                // Arguments.of(List.of(titleSentenceCased), "test-library-title-casing.bib", "title:Title", EnumSet.of(SearchFlags.CASE_SENSITIVE)),

                // Arguments.of(List.of(), "test-library-title-casing.bib", "TiTLE", EnumSet.of(SearchFlags.CASE_SENSITIVE)),
                // Arguments.of(List.of(titleMixedCased), "test-library-title-casing.bib", "TiTle", EnumSet.of(SearchFlags.CASE_SENSITIVE)),

                // Arguments.of(List.of(), "test-library-title-casing.bib", "title:NotExisting", EnumSet.of(SearchFlags.CASE_SENSITIVE)),
                // Arguments.of(List.of(titleMixedCased), "test-library-title-casing.bib", "title:TiTle", EnumSet.of(SearchFlags.CASE_SENSITIVE)),

                Arguments.of(List.of(), "test-library-title-casing.bib", "/[Y]/", EnumSet.noneOf(SearchFlags.class)),

                // test-library-with-attached-files
                Arguments.of(List.of(), "test-library-with-attached-files.bib", "NotExisting.", EnumSet.of(SearchFlags.FULLTEXT)),
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

    @ParameterizedTest
    @MethodSource
    void searchLibrary(List<BibEntry> expected, String testFile, String query, EnumSet<SearchFlags> searchFlags) throws Exception {
        BibDatabaseContext databaseContext = initializeDatabaseFromPath(testFile);
        List<BibEntry> matches = new DatabaseSearcher(new SearchQuery(query, searchFlags), databaseContext, TASK_EXECUTOR, filePreferences).getMatches();
        assertThat(expected, Matchers.containsInAnyOrder(matches.toArray()));
    }
}
