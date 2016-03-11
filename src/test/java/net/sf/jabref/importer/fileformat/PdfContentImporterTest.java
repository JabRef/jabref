package net.sf.jabref.importer.fileformat;

import net.sf.jabref.model.entry.BibEntry;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class PdfContentImporterTest {
    @Test
    public void doesNotHandleEncryptedPdfs() throws IOException {
        PdfContentImporter importer = new PdfContentImporter();
        List<BibEntry> result = importer.importEntries(PdfContentImporter.class.getResourceAsStream("encrypted.pdf"), null);

        assertEquals(Collections.emptyList(), result);
    }
}