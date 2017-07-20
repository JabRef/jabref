package net.sf.jabref.logic.importer;

import java.nio.file.Path;
import java.nio.file.Paths;

import net.sf.jabref.preferences.JabRefPreferences;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ImportFormatCsvIntegrationTest {

    @Test
    public void testImportFormatFromFile() throws Exception {
        ImportFormatReader reader = new ImportFormatReader();
        reader.resetImportFormats(JabRefPreferences.getInstance().getImportFormatPreferences(),
                JabRefPreferences.getInstance().getXMPPreferences());
        Path file = Paths.get(ImportFormatCsvIntegrationTest.class.
                getResource("ezboys/CsvImportTest.csv").toURI());
        assertEquals(1, reader.importFromFile("csv", file).getDatabase().getEntries().size());
    }
}
