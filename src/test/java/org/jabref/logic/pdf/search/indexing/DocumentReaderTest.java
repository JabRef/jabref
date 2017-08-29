package org.jabref.logic.pdf.search.indexing;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.pdf.search.SearchFieldConstants;

import org.apache.lucene.document.Document;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DocumentReaderTest {

    private BibDatabaseContext databaseContext;

    @Before
    public void setup() {
        this.databaseContext = mock(BibDatabaseContext.class);
        when(databaseContext.getFileDirectoriesAsPaths(Mockito.any())).thenReturn(Collections.singletonList(Paths.get("src/test/resources/pdfs")));
    }

    @Test
    public void unknownFileTestShouldReturnEmptyList() throws IOException {
        // given
        BibEntry entry = new BibEntry();
        entry.setFiles(Collections.singletonList(new LinkedFile("Wrong path", "NOT_PRESENT.pdf", "Type")));

        // when
        final List<Document> emptyDocumentList = new DocumentReader(entry).readLinkedPdfs(databaseContext);

        // then
        assertEquals(Collections.emptyList(), emptyDocumentList);
    }

    @Test(expected = IllegalStateException.class)
    public void noLinkedFiles() throws IOException {
        // given
        BibEntry entry = new BibEntry();

        // when
        new DocumentReader(entry);
    }


    @Test
    public void exampleTest() throws IOException {
        // given
        BibEntry entry = new BibEntry("article");
        entry.setCiteKey("Example2017");
        entry.setFiles(Collections.singletonList(new LinkedFile("Example", "example.pdf", "pdf")));
        // when
        List<Document> documents = new DocumentReader(entry).readLinkedPdfs(databaseContext);

        // then
        assertEquals(1, documents.size());

        Document doc = documents.get(0);
        assertEquals("Example2017", doc.get(SearchFieldConstants.KEY));
        assertFalse(doc.get(SearchFieldConstants.CONTENT).isEmpty());
    }

    @Test
    public void thesisExampleTest() throws IOException {
        // given
        BibEntry entry = new BibEntry("PHDThesis");
        entry.setCiteKey("ThesisExample2017");
        entry.setFiles(Collections.singletonList(new LinkedFile("Example thesis", "thesis-example.pdf", "pdf")));

        // when
        List<Document> documents = new DocumentReader(entry).readLinkedPdfs(databaseContext);

        //then
        assertEquals(1, documents.size());

        Document doc = documents.get(0);
        assertEquals("ThesisExample2017", doc.get(SearchFieldConstants.KEY));
        assertFalse(doc.get(SearchFieldConstants.CONTENT).isEmpty());
    }

    @Test
    public void minimalTest() throws IOException {
        // given
        BibEntry entry = new BibEntry("article");
        entry.setCiteKey("Minimal2017");
        entry.setFiles(Collections.singletonList(new LinkedFile("Example thesis", "minimal.pdf", "pdf")));

        // when
        List<Document> documents = new DocumentReader(entry).readLinkedPdfs(databaseContext);

        // then
        assertEquals(1, documents.size());

        Document doc = documents.get(0);
        assertEquals("Minimal2017", doc.get(SearchFieldConstants.KEY));
        assertEquals("Hello World\n1\n", doc.get(SearchFieldConstants.CONTENT));
    }

    @Test
    public void metaDataTest() throws IOException {
        // given
        BibEntry entry = new BibEntry();
        entry.setCiteKey("MetaData2017");
        entry.setFiles(Collections.singletonList(new LinkedFile("Minimal", "metaData.pdf", "pdf")));

        // when
        List<Document> documents = new DocumentReader(entry).readLinkedPdfs(databaseContext);

        // then
        assertEquals(1, documents.size());

        Document doc = documents.get(0);
        assertEquals("MetaData2017", doc.get(SearchFieldConstants.KEY));
        assertEquals("Test\n", doc.get(SearchFieldConstants.CONTENT));
        assertEquals("Author Name", doc.get(SearchFieldConstants.AUTHOR));
        assertEquals("A Subject", doc.get(SearchFieldConstants.SUBJECT));
        assertEquals("Test, Whatever, Metadata, JabRef", doc.get(SearchFieldConstants.KEYWORDS));
    }
}
