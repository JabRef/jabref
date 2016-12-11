package net.sf.jabref.logic.importer;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;

import net.sf.jabref.preferences.JabRefPreferences;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class ImportFormatReaderIntegrationTest {

    private ImportFormatReader reader;

    private final int count;
    public final String format;
    private final Path file;


    public ImportFormatReaderIntegrationTest(String resource, String format, int count) throws URISyntaxException {
        this.format = format;
        this.count = count;
        this.file = Paths.get(ImportFormatReaderIntegrationTest.class.getResource(resource).toURI());

    }

    @Before
    public void setUp() {
        reader = new ImportFormatReader();
        reader.resetImportFormats(JabRefPreferences.getInstance().getImportFormatPreferences(),
                JabRefPreferences.getInstance().getXMPPreferences());
    }

    @Test
    public void testImportUnknownFormat() throws Exception {
        ImportFormatReader.UnknownFormatImport unknownFormat = reader.importUnknownFormat(file);
        assertEquals(count, unknownFormat.parserResult.getDatabase().getEntryCount());
    }

    @Test
    public void testImportFormatFromFile() throws Exception {
        assertEquals(count, reader.importFromFile(format, file).getDatabase().getEntries().size());
    }

    @Parameterized.Parameters(name = "{index}: {1}")
    public static Collection<Object[]> importFormats() {
        Collection<Object[]> result = new ArrayList<>();
        result.add(new Object[] {"fileformat/RisImporterTest1.ris", "ris", 1});
        result.add(new Object[] {"fileformat/IsiImporterTest1.isi", "isi", 1});
        result.add(new Object[] {"fileformat/SilverPlatterImporterTest1.txt", "silverplatter", 1});
        result.add(new Object[] {"fileformat/RepecNepImporterTest2.txt", "repecnep", 1});
        result.add(new Object[] {"fileformat/OvidImporterTest3.txt", "ovid", 1});
        result.add(new Object[] {"fileformat/Endnote.entries.enw", "refer", 5});
        result.add(new Object[] {"fileformat/MsBibImporterTest4.xml", "msbib", 1});
        result.add(new Object[] {"fileformat/MsBibImporterTest4.bib", "bibtex", 1});
        return result;
    }

}
