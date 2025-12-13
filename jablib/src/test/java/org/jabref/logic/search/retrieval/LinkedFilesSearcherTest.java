package org.jabref.logic.search.retrieval;

import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.search.LuceneIndexer;
import org.jabref.logic.search.indexing.DefaultLinkedFilesIndexer;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.search.SearchFlags;
import org.jabref.model.search.query.SearchQuery;
import org.jabref.model.search.query.SearchResults;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LinkedFilesSearcherTest {

    private static final Path TEST_PDF_DIR = Path.of("src", "test", "resources", "pdfs");

    private final CliPreferences preferences = mock(CliPreferences.class);
    private final FilePreferences filePreferences = mock(FilePreferences.class);

    private BibDatabaseContext context;
    private LuceneIndexer indexer;
    private LinkedFilesSearcher searcher;

    @BeforeEach
    void setUp(@TempDir Path indexDir) throws IOException {
        when(filePreferences.shouldFulltextIndexLinkedFiles()).thenReturn(true);
        when(preferences.getFilePreferences()).thenReturn(filePreferences);

        context = mock(BibDatabaseContext.class);

        when(context.getDatabasePath()).thenReturn(Optional.of(TEST_PDF_DIR));
        when(context.getFileDirectories(Mockito.any())).thenReturn(List.of(TEST_PDF_DIR));
        when(context.getFulltextIndexPath()).thenReturn(indexDir);

        this.indexer = new DefaultLinkedFilesIndexer(context, filePreferences);
        this.searcher = new LinkedFilesSearcher(context, indexer, filePreferences);
    }

    @AfterEach
    void tearDown() {
        if (this.indexer != null) {
            this.indexer.closeAndWait();
        }
    }

    @Test
    void searchWithMixedQueryMatchesContentAndMetaData() throws IOException {
        // 1. Setup: Create entry with specific Author (matches regex .*dorf)
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Stuttgart2016")
                .withField(StandardField.AUTHOR, "Author Name");

        // 2. Setup: Link the PDF
        LinkedFile linkedFile = new LinkedFile("Test File", "thesis-example.pdf", StandardFileType.PDF.getName());
        entry.setFiles(List.of(linkedFile));

        when(context.getEntries()).thenReturn(List.of(entry));

        // Index the file
        indexer.addToIndex(List.of(entry), mock(BackgroundTask.class));
        indexer.getSearcherManager().maybeRefreshBlocking();

        // 3. Action: Search using Mixed Query
        // Logic: "Find entry where Author matches regex AND file contains partial word 'Univer'"
        SearchQuery searchQuery = new SearchQuery(
                "author=~.*Name AND Univer",
                EnumSet.of(SearchFlags.FULLTEXT)
        );
        SearchResults results = searcher.search(searchQuery);

        assertThat(
                "Should find entry matching Author Regex AND File Content",
                results.getMatchedEntries(),
                Matchers.not(Matchers.empty())
        );
    }
}
