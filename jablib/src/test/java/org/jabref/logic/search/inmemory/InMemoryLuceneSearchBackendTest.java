package org.jabref.logic.search.inmemory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
import org.mockito.Answers;

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

    @Test
    void searchesLinkedFileContentsWithoutPostgres() throws IOException, URISyntaxException {
        BibDatabaseContext databaseContext = initializeDatabaseContext("test-library-with-attached-files.bib");
        searchBackend = new InMemoryLuceneSearchBackend(
                databaseContext,
                BibEntryPreferences.getDefault(),
                FilePreferences.getDefault(),
                TASK_EXECUTOR);

        SearchQuery searchQuery = new SearchQuery("comma", EnumSet.of(SearchFlags.FULLTEXT));

        Set<String> matchedCitationKeys = searchBackend.search(searchQuery)
                                                      .getMatchedEntries()
                                                      .stream()
                                                      .map(entryId -> databaseContext.getDatabase().getEntryById(entryId).orElseThrow())
                                                      .map(entry -> entry.getCitationKey().orElseThrow())
                                                      .collect(Collectors.toUnmodifiableSet());

        assertEquals(Set.of("minimal-sentence-case", "minimal-all-upper-case", "minimal-mixed-case"), matchedCitationKeys);
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
