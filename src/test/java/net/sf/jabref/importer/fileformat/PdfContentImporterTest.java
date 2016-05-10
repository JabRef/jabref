package net.sf.jabref.importer.fileformat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import net.sf.jabref.model.entry.BibEntry;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PdfContentImporterTest {

    @Test
    public void doesNotHandleEncryptedPdfs() throws IOException {
        PdfContentImporter importer = new PdfContentImporter();
        try (InputStream is = PdfContentImporter.class.getResourceAsStream("/pdfs/encrypted.pdf")) {
            List<BibEntry> result = importer.importEntries(is, null);
            assertEquals(Collections.emptyList(), result);
        }
    }

}
