package org.jabref.logic.pdf.search.retrieval;

import java.io.IOException;
import java.util.Optional;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.logic.pdf.search.indexing.Indexer;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.pdf.search.ResultSet;

import org.apache.lucene.queryparser.classic.ParseException;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PdfSearcherTest {


    private PdfSearcher search;

    @Before
    public void setUp() throws IOException {
        this.search = new PdfSearcher();
        Indexer indexer = new Indexer();
        ObservableList<BibEntry> entryList = FXCollections.observableArrayList();

        BibEntry examplePdf = mock(BibEntry.class);
        when(examplePdf.hasField(FieldName.FILE)).thenReturn(true);
        when(examplePdf.getField(FieldName.FILE)).thenReturn(Optional.of("src/test/resources/pdfs/example.pdf"));
        when(examplePdf.getCiteKeyOptional()).thenReturn(Optional.of("Example2017"));
        entryList.add(examplePdf);

        BibEntry metaDataEntry = mock(BibEntry.class);
        when(metaDataEntry.hasField(FieldName.FILE)).thenReturn(true);
        when(metaDataEntry.getField(FieldName.FILE)).thenReturn(Optional.of("src/test/resources/pdfs/metaData.pdf"));
        when(metaDataEntry.getCiteKeyOptional()).thenReturn(Optional.of("MetaData2017"));
        entryList.add(metaDataEntry);

        BibEntry exampleThesis = mock(BibEntry.class);
        when(exampleThesis.hasField(FieldName.FILE)).thenReturn(true);
        when(exampleThesis.getField(FieldName.FILE)).thenReturn(Optional.of("src/test/resources/pdfs/thesis-example.pdf"));
        when(exampleThesis.getCiteKeyOptional()).thenReturn(Optional.of("ExampleThesis"));
        entryList.add(exampleThesis);

        BibDatabase database = mock(BibDatabase.class);
        when(database.getEntries()).thenReturn(entryList);

        indexer.createIndex(database);
    }


    @Test
    public void searchForTest() throws IOException, ParseException {
        ResultSet result = search.search("test", 10);
        assertEquals(2, result.numSearchResults());
    }

    @Test
    public void searchForUniversity() throws IOException, ParseException {
        ResultSet result = search.search("University", 10);
        assertEquals(1, result.numSearchResults());
    }

    @Test
    public void searchForStopWord() throws IOException, ParseException {
        ResultSet result = search.search("and", 10);
        assertEquals(0, result.numSearchResults());
    }

    @Test
    public void searchForSecond() throws IOException, ParseException {
        ResultSet result = search.search("second", 10);
        assertEquals(2, result.numSearchResults());
    }

    @Test
    public void searchForEmptyString() throws IOException {
        ResultSet result = search.search("", 10);
        assertEquals(0, result.numSearchResults());
    }

    @Test(expected = NullPointerException.class)
    public void searchWithNullString() throws IOException {
        search.search(null, 10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void searchForZeroResults() throws IOException {
        search.search("test", 0);
    }
}
