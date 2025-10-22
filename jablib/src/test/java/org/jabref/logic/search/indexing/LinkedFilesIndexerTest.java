package org.jabref.logic.search.indexing;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.search.LuceneIndexer;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.types.StandardEntryType;

import org.apache.lucene.index.IndexReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LinkedFilesIndexerTest {
    private final CliPreferences preferences = mock(CliPreferences.class);
    private final FilePreferences filePreferences = mock(FilePreferences.class);

    private LuceneIndexer indexer;

    @BeforeEach
    void setUp(@TempDir Path indexDir) throws IOException {
        when(filePreferences.shouldFulltextIndexLinkedFiles()).thenReturn(true);
        when(preferences.getFilePreferences()).thenReturn(filePreferences);

        BibDatabaseContext context = mock(BibDatabaseContext.class);
        when(context.getDatabasePath()).thenReturn(Optional.of(Path.of("src/test/resources/pdfs/")));
        when(context.getFileDirectories(Mockito.any())).thenReturn(List.of(Path.of("src/test/resources/pdfs")));
        when(context.getFulltextIndexPath()).thenReturn(indexDir);

        this.indexer = new DefaultLinkedFilesIndexer(context, filePreferences);
    }

    @AfterEach
    void tearDown() {
        this.indexer.closeAndWait();
    }

    @Test
    void exampleThesisIndex() throws IOException {
        // given
        BibEntry entry = new BibEntry(StandardEntryType.PhdThesis)
                .withFiles(List.of(LinkedFile.of("Example Thesis", "thesis-example.pdf", StandardFileType.PDF.getName())));

        // when
        indexer.addToIndex(List.of(entry), mock(BackgroundTask.class));

        // then
        indexer.getSearcherManager().maybeRefreshBlocking();
        try (IndexReader reader = indexer.getSearcherManager().acquire().getIndexReader()) {
            assertEquals(33, reader.numDocs());
        }
    }

    @Test
    void dontIndexNonPdf() throws IOException {
        // given
        BibEntry entry = new BibEntry(StandardEntryType.PhdThesis)
                .withFiles(List.of(LinkedFile.of("Example Thesis", "thesis-example.pdf", StandardFileType.AUX.getName())));

        // when
        indexer.addToIndex(List.of(entry), mock(BackgroundTask.class));

        // then
        indexer.getSearcherManager().maybeRefreshBlocking();
        try (IndexReader reader = indexer.getSearcherManager().acquire().getIndexReader()) {
            assertEquals(0, reader.numDocs());
        }
    }

    @Test
    void dontIndexOnlineLinks() throws IOException {
        // given
        BibEntry entry = new BibEntry(StandardEntryType.PhdThesis)
                .withFiles(List.of(LinkedFile.of("Example Thesis", "https://raw.githubusercontent.com/JabRef/jabref/main/src/test/resources/pdfs/thesis-example.pdf", StandardFileType.PDF.getName())));

        // when
        indexer.addToIndex(List.of(entry), mock(BackgroundTask.class));

        // then
        indexer.getSearcherManager().maybeRefreshBlocking();
        try (IndexReader reader = indexer.getSearcherManager().acquire().getIndexReader()) {
            assertEquals(0, reader.numDocs());
        }
    }

    @Test
    void exampleThesisIndexWithKey() throws IOException {
        // given
        BibEntry entry = new BibEntry(StandardEntryType.PhdThesis)
                .withCitationKey("Example2017")
                .withFiles(List.of(LinkedFile.of("Example Thesis", "thesis-example.pdf", StandardFileType.PDF.getName())));

        // when
        indexer.addToIndex(List.of(entry), mock(BackgroundTask.class));

        // then
        indexer.getSearcherManager().maybeRefreshBlocking();
        try (IndexReader reader = indexer.getSearcherManager().acquire().getIndexReader()) {
            assertEquals(33, reader.numDocs());
        }
    }

    @Test
    void metaDataIndex() throws IOException {
        // given
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withFiles(List.of(LinkedFile.of("Example Thesis", "metaData.pdf", StandardFileType.PDF.getName())));

        // when
        indexer.addToIndex(List.of(entry), mock(BackgroundTask.class));

        // then
        indexer.getSearcherManager().maybeRefreshBlocking();
        try (IndexReader reader = indexer.getSearcherManager().acquire().getIndexReader()) {
            assertEquals(1, reader.numDocs());
        }
    }

    @Test
    void exampleThesisIndexAppendMetaData() throws IOException {
        // given
        BibEntry exampleThesis = new BibEntry(StandardEntryType.PhdThesis)
                .withCitationKey("ExampleThesis2017")
                .withFiles(List.of(LinkedFile.of("Example Thesis", "thesis-example.pdf", StandardFileType.PDF.getName())));

        // when
        indexer.addToIndex(List.of(exampleThesis), mock(BackgroundTask.class));

        // index with first entry
        indexer.getSearcherManager().maybeRefreshBlocking();
        try (IndexReader reader = indexer.getSearcherManager().acquire().getIndexReader()) {
            assertEquals(33, reader.numDocs());
        }

        BibEntry metadata = new BibEntry(StandardEntryType.Article)
                .withCitationKey("MetaData2017")
                .withFiles(List.of(LinkedFile.of("Metadata file", "metaData.pdf", StandardFileType.PDF.getName())));

        // when
        indexer.addToIndex(List.of(metadata), mock(BackgroundTask.class));

        // then
        indexer.getSearcherManager().maybeRefreshBlocking();
        try (IndexReader reader = indexer.getSearcherManager().acquire().getIndexReader()) {
            assertEquals(34, reader.numDocs());
        }
    }

    @Test
    public void flushIndex() throws IOException {
        // given
        BibEntry entry = new BibEntry(StandardEntryType.PhdThesis)
                .withCitationKey("Example2017")
                .withFiles(List.of(LinkedFile.of("Example Thesis", "thesis-example.pdf", StandardFileType.PDF.getName())));

        indexer.addToIndex(List.of(entry), mock(BackgroundTask.class));

        indexer.getSearcherManager().maybeRefreshBlocking();
        try (IndexReader reader = indexer.getSearcherManager().acquire().getIndexReader()) {
            assertEquals(33, reader.numDocs());
        }

        indexer.removeAllFromIndex();

        indexer.getSearcherManager().maybeRefreshBlocking();
        try (IndexReader reader = indexer.getSearcherManager().acquire().getIndexReader()) {
            assertEquals(0, reader.numDocs());
        }
    }
}
