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
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.search.SearchFlags;
import org.jabref.model.search.query.SearchQuery;
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

    private final CliPreferences preferences = mock(CliPreferences.class);
    private final FilePreferences filePreferences = mock(FilePreferences.class);
    private final BibEntryPreferences bibEntryPreferences = mock(BibEntryPreferences.class);

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

        when(preferences.getBibEntryPreferences()).thenReturn(bibEntryPreferences);
        when(preferences.getFilePreferences()).thenReturn(filePreferences);

        when(bibEntryPreferences.getKeywordSeparator()).thenReturn(',');
        return databaseContext;
    }

    private static Stream<Arguments> searchLibrary() {
        return Stream.of(
                // empty library
                Arguments.of(List.of(), "empty.bib", "Test", false),

                // test-library-title-casing
                Arguments.of(List.of(), "test-library-title-casing.bib", "NotExisting", false),
                Arguments.of(List.of(TITLE_SENTENCE_CASED, TITLE_MIXED_CASED, TITLE_UPPER_CASED), "test-library-title-casing.bib", "Title", false),

                Arguments.of(List.of(), "test-library-title-casing.bib", "title = NotExisting", false),
                Arguments.of(List.of(TITLE_SENTENCE_CASED, TITLE_MIXED_CASED, TITLE_UPPER_CASED), "test-library-title-casing.bib", "title = Title", false),

                 Arguments.of(List.of(), "test-library-title-casing.bib", "title =! TiTLE", false),
                 Arguments.of(List.of(TITLE_SENTENCE_CASED), "test-library-title-casing.bib", "title =! Title", false),

                 Arguments.of(List.of(), "test-library-title-casing.bib", "any =! TiTLE", false),
                 Arguments.of(List.of(TITLE_MIXED_CASED), "test-library-title-casing.bib", "any =! TiTle", false),

                 Arguments.of(List.of(), "test-library-title-casing.bib", "title =! NotExisting", false),
                 Arguments.of(List.of(TITLE_MIXED_CASED), "test-library-title-casing.bib", "title =! TiTle", false),

                Arguments.of(List.of(), "test-library-title-casing.bib", "any =~ [Y]", false),

                // test-library-with-attached-files
                Arguments.of(List.of(), "test-library-with-attached-files.bib", "NotExisting.", true),
                Arguments.of(List.of(MINIMAL_SENTENCE_CASE, MINIMAL_ALL_UPPER_CASE, MINIMAL_MIXED_CASE), "test-library-with-attached-files.bib", "\"This is a short sentence, comma included.\"", true),
                Arguments.of(List.of(MINIMAL_SENTENCE_CASE, MINIMAL_ALL_UPPER_CASE, MINIMAL_MIXED_CASE), "test-library-with-attached-files.bib", "comma", true),

                Arguments.of(List.of(), "test-library-with-attached-files.bib", "NotExisting", true),
                Arguments.of(List.of(MINIMAL_NOTE_SENTENCE_CASE, MINIMAL_NOTE_ALL_UPPER_CASE, MINIMAL_NOTE_MIXED_CASE), "test-library-with-attached-files.bib", "world", true),
                Arguments.of(List.of(MINIMAL_NOTE_SENTENCE_CASE, MINIMAL_NOTE_ALL_UPPER_CASE, MINIMAL_NOTE_MIXED_CASE), "test-library-with-attached-files.bib", "\"Hello World\"", true)
        );
    }

    @ParameterizedTest
    @MethodSource
    void searchLibrary(List<BibEntry> expected, String testFile, String query, boolean isFullText) throws Exception {
        BibDatabaseContext databaseContext = initializeDatabaseFromPath(testFile);
        EnumSet<SearchFlags> flags = isFullText ? EnumSet.of(SearchFlags.FULLTEXT) : EnumSet.noneOf(SearchFlags.class);
        List<BibEntry> matches = new DatabaseSearcher(new SearchQuery(query, flags), databaseContext, TASK_EXECUTOR, preferences).getMatches();
        assertThat(expected, Matchers.containsInAnyOrder(matches.toArray()));
    }
}
