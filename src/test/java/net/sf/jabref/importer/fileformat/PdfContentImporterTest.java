package net.sf.jabref.importer.fileformat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import net.sf.jabref.model.entry.BibEntry;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PdfContentImporterTest {

    @Test
    public void doesNotHandleEncryptedPdfs() throws IOException, URISyntaxException {
        PdfContentImporter importer = new PdfContentImporter();
        Path file = Paths.get(PdfContentImporter.class.getResource("/pdfs/encrypted.pdf").toURI());
        List<BibEntry> result = importer.importDatabase(file, Charset.defaultCharset()).getDatabase().getEntries();
        assertEquals(Collections.emptyList(), result);
    }

}
