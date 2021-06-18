package org.jabref.logic.pdf.search.indexing;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.pdf.search.SearchFieldConstants;

import org.apache.lucene.document.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DocumentReaderTest {

    private BibDatabaseContext databaseContext;

    @BeforeEach
    public void setup() {
        this.databaseContext = mock(BibDatabaseContext.class);
        when(databaseContext.getFileDirectories(Mockito.any())).thenReturn(Collections.singletonList(Path.of("src/test/resources/pdfs")));
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

    @Test
    public void noLinkedFiles() throws IOException {
        BibEntry entry = new BibEntry();
        assertThrows(IllegalStateException.class, () -> new DocumentReader(entry));
    }

    @Test
    public void exampleTest() throws IOException {
        // given
        BibEntry entry = new BibEntry(StandardEntryType.Article);
        entry.setCitationKey("Example2017");
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
        BibEntry entry = new BibEntry(StandardEntryType.PhdThesis);
        entry.setCitationKey("ThesisExample2017");
        entry.setFiles(Collections.singletonList(new LinkedFile("Example thesis", "thesis-example.pdf", "pdf")));

        // when
        List<Document> documents = new DocumentReader(entry).readLinkedPdfs(databaseContext);

        // then
        assertEquals(1, documents.size());

        Document doc = documents.get(0);
        assertEquals("ThesisExample2017", doc.get(SearchFieldConstants.KEY));
        assertFalse(doc.get(SearchFieldConstants.CONTENT).isEmpty());
    }

    @Test
    public void minimalTest() throws IOException {
        // given
        BibEntry entry = new BibEntry(StandardEntryType.Article);
        entry.setCitationKey("Minimal2017");
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
        entry.setCitationKey("MetaData2017");
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
