package net.sf.jabref.importer;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.model.entry.BibEntry;

public class ImportFormatReaderTestParameterless {

    private ImportFormatReader reader;
    private ImportFormatReader.UnknownFormatImport unknownFormat;
    public List<BibEntry> result;
    @Before
    public void setUp() {
        Globals.prefs = JabRefPreferences.getInstance();
        reader = new ImportFormatReader();
        reader.resetImportFormats();
    }

    @Test
    public void testImportUnknownFormatNotWorking() {
        String fileName = ImportFormatReaderTestParameterless.class.getResource("fileformat/emptyFile.xml").getFile();
        unknownFormat = reader.importUnknownFormat(fileName);
        assertNull(unknownFormat);
    }

    @Test(expected = NullPointerException.class)
    public void testNullImportUnknownFormat() {
        unknownFormat = reader.importUnknownFormat(null);
        fail();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testImportFromFileUnknownFormat() throws IOException {
        result = reader.importFromFile("someunknownformat", "doesn't matter", new OutputPrinterToNull());
        fail();
    }
}
