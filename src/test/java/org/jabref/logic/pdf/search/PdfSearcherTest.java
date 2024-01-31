package org.jabref.logic.pdf.search;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.pdf.search.PdfSearchResults;
import org.jabref.model.pdf.search.SearchResult;
import org.jabref.preferences.FilePreferences;

import org.apache.lucene.queryparser.classic.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PdfSearcherTest {

    private PdfSearcher search;

    @BeforeEach
    public void setUp(@TempDir Path indexDir) throws IOException {
        FilePreferences filePreferences = mock(FilePreferences.class);

        BibDatabase database = new BibDatabase();

        BibDatabaseContext context = mock(BibDatabaseContext.class);
        when(context.getFileDirectories(Mockito.any())).thenReturn(Collections.singletonList(Path.of("src/test/resources/pdfs")));
        when(context.getFulltextIndexPath()).thenReturn(indexDir);
        when(context.getDatabase()).thenReturn(database);
        when(context.getEntries()).thenReturn(database.getEntries());

        BibEntry examplePdf = new BibEntry(StandardEntryType.Article)
                .withFiles(Collections.singletonList(new LinkedFile("Example Entry", "example.pdf", StandardFileType.PDF.getName())));
        database.insertEntry(examplePdf);

        BibEntry metaDataEntry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("MetaData2017")
                .withFiles(Collections.singletonList(new LinkedFile("Metadata Entry", "metaData.pdf", StandardFileType.PDF.getName())));
        database.insertEntry(metaDataEntry);

        BibEntry exampleThesis = new BibEntry(StandardEntryType.PhdThesis)
                .withCitationKey("ExampleThesis")
                .withFiles(Collections.singletonList(new LinkedFile("Example Thesis", "thesis-example.pdf", StandardFileType.PDF.getName())));
        database.insertEntry(exampleThesis);

        PdfIndexer indexer = PdfIndexer.of(context, filePreferences);
        search = PdfSearcher.of(indexer);

        indexer.rebuildIndex();
    }

    @Test
    public void searchForTest() throws IOException, ParseException {
        PdfSearchResults result = search.search("test", 10);
        assertEquals(10, result.numSearchResults());
    }

    @Test
    public void searchForUniversity() throws IOException, ParseException {
        PdfSearchResults result = search.search("University", 10);
        assertEquals(2, result.numSearchResults());
        List<SearchResult> searchResults = result.getSearchResults();
        assertEquals(0, searchResults.getFirst().getPageNumber());
        assertEquals(9, searchResults.get(1).getPageNumber());
    }

    @Test
    public void searchForStopWord() throws IOException, ParseException {
        PdfSearchResults result = search.search("and", 10);
        assertEquals(0, result.numSearchResults());
    }

    @Test
    public void searchForSecond() throws IOException, ParseException {
        PdfSearchResults result = search.search("second", 10);
        assertEquals(4, result.numSearchResults());
    }

    @Test
    public void searchForAnnotation() throws IOException, ParseException {
        PdfSearchResults result = search.search("annotation", 10);
        assertEquals(2, result.numSearchResults());
    }

    @Test
    public void searchForEmptyString() throws IOException {
        PdfSearchResults result = search.search("", 10);
        assertEquals(0, result.numSearchResults());
    }

    @Test
    public void searchWithNullString() throws IOException {
        assertThrows(NullPointerException.class, () -> search.search(null, 10));
    }

    @Test
    public void searchForZeroResults() throws IOException {
        assertThrows(IllegalArgumentException.class, () -> search.search("test", 0));
    }
}
