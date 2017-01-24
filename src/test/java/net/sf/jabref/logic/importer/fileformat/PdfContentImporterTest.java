package net.sf.jabref.logic.importer.fileformat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import net.sf.jabref.logic.importer.ImportFormatPreferences;
import net.sf.jabref.logic.util.FileExtensions;
import net.sf.jabref.model.entry.BibEntry;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PdfContentImporterTest {

    private PdfContentImporter importer;

    @Before
    public void setUp() {
        importer = new PdfContentImporter(new ImportFormatPreferences());
    }

    @Test
    public void testsGetExtensions() {
        assertEquals(FileExtensions.PDF_CONTENT, importer.getExtensions());
    }

    @Test
    public void doesNotHandleEncryptedPdfs() throws Exception {
        Path file = Paths.get(PdfContentImporter.class.getResource("/pdfs/encrypted.pdf").toURI());
        List<BibEntry> result = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();
        assertEquals(Collections.emptyList(), result);
    }

}
