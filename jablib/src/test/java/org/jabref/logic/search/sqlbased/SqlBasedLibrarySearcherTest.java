package org.jabref.logic.search.sqlbased;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import javafx.beans.property.BooleanProperty;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.search.query.SearchQuery;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("embeddedPostgres")
public class SqlBasedLibrarySearcherTest {
    private static final TaskExecutor TASK_EXECUTOR = new CurrentThreadTaskExecutor();
    private BibDatabaseContext databaseContext;
    private final CliPreferences preferences = mock(CliPreferences.class);
    private final FilePreferences filePreferences = mock(FilePreferences.class);
    private final BibEntryPreferences bibEntryPreferences = mock(BibEntryPreferences.class);
    private PostgreServer postgreServer;

    @TempDir
    private Path indexDir;

    @BeforeEach
    void setUp() {
        when(preferences.getBibEntryPreferences()).thenReturn(bibEntryPreferences);
        when(preferences.getFilePreferences()).thenReturn(filePreferences);

        when(bibEntryPreferences.getKeywordSeparator()).thenReturn(',');

        when(filePreferences.shouldFulltextIndexLinkedFiles()).thenReturn(false);
        when(filePreferences.fulltextIndexLinkedFilesProperty()).thenReturn(mock(BooleanProperty.class));
        databaseContext = spy(new BibDatabaseContext());
        when(databaseContext.getFulltextIndexPath()).thenReturn(indexDir);

        postgreServer = new PostgreServer();
    }

    @AfterEach
    void tearDown() {
        postgreServer.close();
    }

    @ParameterizedTest
    @MethodSource("org.jabref.logic.search.LibrarySearcherTestCases#commonSearchCases")
    void commonSearchCases(List<BibEntry> expectedMatches, SearchQuery query, List<BibEntry> entries) throws IOException {
        for (BibEntry entry : entries) {
            databaseContext.getDatabase().insertEntry(entry);
        }
        List<BibEntry> matches = new SqlBasedLibrarySearcher(databaseContext, TASK_EXECUTOR, preferences, postgreServer).getMatches(query);
        // Order-insensitive comparison: SQL-backed searcher does not guarantee result ordering.
        assertEquals(Set.copyOf(expectedMatches), Set.copyOf(matches));
    }
}
