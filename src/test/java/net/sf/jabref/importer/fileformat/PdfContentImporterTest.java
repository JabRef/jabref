package net.sf.jabref.importer.fileformat;

import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.sf.jabref.model.entry.BibEntry;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PdfContentImporterTest {

    private final PdfContentImporter importer = new PdfContentImporter();

    @Test
    public void testsGetExtensions() {
        assertEquals(Arrays.asList(".pdf"), importer.getExtensions());
    }

    @Test
    public void testGetDescription() {
        assertEquals("PdfContentImporter parses data of the first page of the PDF and creates a BibTeX entry. Currently, Springer and IEEE formats are supported.", importer.getDescription());
    }

    @Test
    public void doesNotHandleEncryptedPdfs() throws URISyntaxException {
        Path file = Paths.get(PdfContentImporter.class.getResource("/pdfs/encrypted.pdf").toURI());
        List<BibEntry> result = importer.importDatabase(file, Charset.defaultCharset()).getDatabase().getEntries();
        assertEquals(Collections.emptyList(), result);
    }

}
