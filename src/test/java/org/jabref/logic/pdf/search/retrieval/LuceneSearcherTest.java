package org.jabref.logic.pdf.search.retrieval;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;

import org.jabref.logic.search.SearchQuery;
import org.jabref.logic.search.indexing.LuceneIndexer;
import org.jabref.logic.search.retrieval.LuceneSearcher;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.pdf.search.LuceneSearchResults;
import org.jabref.model.search.rules.SearchRules;
import org.jabref.preferences.FilePreferences;
import org.jabref.preferences.PreferencesService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LuceneSearcherTest {

    private LuceneSearcher search;

    @BeforeEach
    public void setUp(@TempDir Path indexDir) throws IOException {
        FilePreferences filePreferences = mock(FilePreferences.class);
        when(filePreferences.shouldFulltextIndexLinkedFiles()).thenReturn(true);
        PreferencesService preferencesService = mock(PreferencesService.class);
        when(preferencesService.getFilePreferences()).thenReturn(filePreferences);
        // given
        BibDatabase database = new BibDatabase();
        BibDatabaseContext context = mock(BibDatabaseContext.class);
        when(context.getFileDirectories(Mockito.any())).thenReturn(Collections.singletonList(Path.of("src/test/resources/pdfs")));
        when(context.getFulltextIndexPath()).thenReturn(indexDir);
        when(context.getDatabase()).thenReturn(database);
        when(context.getEntries()).thenReturn(database.getEntries());
        BibEntry examplePdf = new BibEntry(StandardEntryType.Article);
        examplePdf.setFiles(Collections.singletonList(new LinkedFile("Example Entry", "example.pdf", StandardFileType.PDF.getName())));
        database.insertEntry(examplePdf);

        BibEntry metaDataEntry = new BibEntry(StandardEntryType.Article);
        metaDataEntry.setFiles(Collections.singletonList(new LinkedFile("Metadata Entry", "metaData.pdf", StandardFileType.PDF.getName())));
        metaDataEntry.setCitationKey("MetaData2017");
        database.insertEntry(metaDataEntry);

        BibEntry exampleThesis = new BibEntry(StandardEntryType.PhdThesis);
        exampleThesis.setFiles(Collections.singletonList(new LinkedFile("Example Thesis", "thesis-example.pdf", StandardFileType.PDF.getName())));
        exampleThesis.setCitationKey("ExampleThesis");
        database.insertEntry(exampleThesis);

        LuceneIndexer indexer = LuceneIndexer.of(context, preferencesService);
        search = LuceneSearcher.of(context);

        indexer.createIndex();
        for (BibEntry bibEntry : context.getEntries()) {
            indexer.addBibFieldsToIndex(bibEntry);
            indexer.addLinkedFilesToIndex(bibEntry);
        }
    }

    @Test
    public void searchForTest() {
        HashMap<BibEntry, LuceneSearchResults> searchResults = search.search(new SearchQuery("", EnumSet.noneOf(SearchRules.SearchFlags.class)));
        int hits = searchResults.keySet().stream().mapToInt((key) -> searchResults.get(key).numSearchResults()).sum();
        assertEquals(8, hits);
    }

    @Test
    public void searchForUniversity() {
        HashMap<BibEntry, LuceneSearchResults> searchResults = search.search(new SearchQuery("University", EnumSet.noneOf(SearchRules.SearchFlags.class)));
        int hits = searchResults.keySet().stream().mapToInt((key) -> searchResults.get(key).numSearchResults()).sum();
        assertEquals(1, hits);
    }

    @Test
    public void searchForStopWord() {
        HashMap<BibEntry, LuceneSearchResults> searchResults = search.search(new SearchQuery("and", EnumSet.noneOf(SearchRules.SearchFlags.class)));
        int hits = searchResults.keySet().stream().mapToInt((key) -> searchResults.get(key).numSearchResults()).sum();
        assertEquals(0, hits);
    }

    @Test
    public void searchForSecond() {
        HashMap<BibEntry, LuceneSearchResults> searchResults = search.search(new SearchQuery("second", EnumSet.noneOf(SearchRules.SearchFlags.class)));
        int hits = searchResults.keySet().stream().mapToInt((key) -> searchResults.get(key).numSearchResults()).sum();
        assertEquals(4, hits);
    }

    @Test
    public void searchForAnnotation() {
        HashMap<BibEntry, LuceneSearchResults> searchResults = search.search(new SearchQuery("annotation", EnumSet.noneOf(SearchRules.SearchFlags.class)));
        int hits = searchResults.keySet().stream().mapToInt((key) -> searchResults.get(key).numSearchResults()).sum();
        assertEquals(2, hits);
    }

    @Test
    public void searchForEmptyString() {
        HashMap<BibEntry, LuceneSearchResults> searchResults = search.search(new SearchQuery("", EnumSet.noneOf(SearchRules.SearchFlags.class)));
        int hits = searchResults.keySet().stream().mapToInt((key) -> searchResults.get(key).numSearchResults()).sum();
        assertEquals(0, hits);
    }

    @Test
    public void searchWithNullString() {
        assertThrows(NullPointerException.class, () -> search.search(null));
    }
}
