package org.jabref.logic.search.inmemory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.search.SearchFlags;
import org.jabref.model.search.query.SearchQuery;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@NullMarked
        // [utest->req~jabgui.search.fulltext.lucene-without-postgres~1]
class InMemoryLuceneSearchBackendTest {

    private static final TaskExecutor TASK_EXECUTOR = new CurrentThreadTaskExecutor();

    @TempDir
    private Path indexDirectory;

    private @Nullable InMemoryLuceneSearchBackend searchBackend;

    @AfterEach
    void tearDown() {
        Optional.ofNullable(searchBackend).ifPresent(InMemoryLuceneSearchBackend::close);
    }

    static Stream<Arguments> searchesLinkedFileContentsWithoutPostgres() {
        return Stream.of(
                Arguments.of(Set.of("minimal-sentence-case", "minimal-all-upper-case", "minimal-mixed-case"), "comma"),

                // case-sensitive search - https://github.com/JabRef/jabref/issues/13048
                // [utest->req~jabgui.search.fulltext.case-sensitive~1]
                Arguments.of(Set.of("minimal-sentence-case", "minimal-mixed-case"), "any =! comma"),
                Arguments.of(Set.of("minimal-all-upper-case"), "any =! COMMA"),
                Arguments.of(Set.of("minimal-note-sentence-case"), "any ==! Hello"),
                Arguments.of(Set.of("minimal-note-all-upper-case"), "any ==! HELLO"),
                Arguments.of(Set.of(), "any =! Comma")
        );
    }

    @ParameterizedTest
    @MethodSource
    void searchesLinkedFileContentsWithoutPostgres(Set<String> expectedCitationKeys, String query) throws IOException, URISyntaxException {
        assertEquals(expectedCitationKeys, search(query, EnumSet.of(SearchFlags.FULLTEXT)));
    }

    /// [Issue 9482](https://github.com/JabRef/jabref/issues/9482): a quotation mark used to make the search throw.
    @Test
    void searchesPhraseInLinkedFileContents() throws IOException, URISyntaxException {
        assertEquals(
                Set.of("minimal-sentence-case", "minimal-all-upper-case", "minimal-mixed-case"),
                search("\"short sentence\"", EnumSet.of(SearchFlags.FULLTEXT)));
    }

    @Test
    void searchesPhraseWordsSeparatelyOnMissingClosingQuote() throws IOException, URISyntaxException {
        assertEquals(
                Set.of("minimal-sentence-case", "minimal-all-upper-case", "minimal-mixed-case"),
                search("\"short sentence", EnumSet.of(SearchFlags.FULLTEXT)));
    }

    @Test
    void findsNothingForPhraseNotContainedInLinkedFileContents() throws IOException, URISyntaxException {
        assertEquals(Set.of(), search("\"sentence short\"", EnumSet.of(SearchFlags.FULLTEXT)));
    }

    /// Lucene reads `"` as syntax of its own regular expression dialect, `java.util.regex` does not.
    /// The metadata results still have to arrive, only the linked files are left out.
    // [utest->req~jabgui.search.fulltext.lenient-query-parsing~1]
    @Test
    void keepsMetadataResultsOnRegularExpressionLuceneCannotParse() throws IOException, URISyntaxException {
        assertEquals(
                Set.of("minimal-mixed-case"),
                search("\"?minimal-mixed-case", EnumSet.of(SearchFlags.FULLTEXT, SearchFlags.REGULAR_EXPRESSION)));
    }

    // [utest->req~jabgui.search.fulltext.lenient-query-parsing~1]
    @ParameterizedTest
    @ValueSource(strings = {"\"", "\"a", "a\"", "a\"b", "<", "a<b"})
    void doesNotThrowOnRegularExpressionLuceneCannotParse(String searchExpression) {
        assertDoesNotThrow(() -> search(searchExpression, EnumSet.of(SearchFlags.FULLTEXT, SearchFlags.REGULAR_EXPRESSION)));
    }

    private Set<String> search(String searchExpression, EnumSet<SearchFlags> searchFlags) throws IOException, URISyntaxException {
        BibDatabaseContext databaseContext = initializeDatabaseContext("test-library-with-attached-files.bib");
        searchBackend = new InMemoryLuceneSearchBackend(
                databaseContext,
                BibEntryPreferences.getDefault(),
                FilePreferences.getDefault(),
                TASK_EXECUTOR);

        return searchBackend.search(new SearchQuery(searchExpression, searchFlags))
                            .getMatchedEntries()
                            .stream()
                            .map(entryId -> databaseContext.getDatabase().getEntryById(entryId).orElseThrow())
                            .map(entry -> entry.getCitationKey().orElseThrow())
                            .collect(Collectors.toUnmodifiableSet());
    }

    private BibDatabaseContext initializeDatabaseContext(String testFile) throws URISyntaxException, IOException {
        URL bibResource = Optional.ofNullable(
                                          InMemoryLuceneSearchBackendTest.class.getResource("/org/jabref/logic/search/" + testFile))
                                  .orElseThrow();
        Path bibFile = Path.of(bibResource.toURI());
        ParserResult result = new BibtexImporter(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS), new DummyFileUpdateMonitor()).importDatabase(bibFile);
        BibDatabaseContext databaseContext = spy(result.getDatabaseContext());
        when(databaseContext.getFulltextIndexPath()).thenReturn(indexDirectory);
        return databaseContext;
    }
}
