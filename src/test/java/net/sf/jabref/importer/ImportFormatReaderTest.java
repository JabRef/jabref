package net.sf.jabref.importer;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;

@RunWith(Parameterized.class)
public class ImportFormatReaderTest {

    private ImportFormatReader reader;

    private final String resourceName;
    private final int count;
    public final String format;

    public ImportFormatReaderTest(String resource, String format, int count) {
        this.resourceName = resource;
        this.format = format;
        this.count = count;
    }

    @Before
    public void setUp() {
        Globals.prefs = JabRefPreferences.getInstance();
        reader = new ImportFormatReader();
        reader.resetImportFormats();
    }

    @Test
    public void testImportUnknownFormat() {
        String risFileName = ImportFormatReaderTest.class.getResource(resourceName).getFile();
        ImportFormatReader.UnknownFormatImport unknownFormat = reader.importUnknownFormat(risFileName);
        assertEquals(count, unknownFormat.parserResult.getDatabase().getEntryCount());
    }

    @Parameterized.Parameters(name = "{index}: {1}")
    public static Collection<Object[]> importFormats() {
        Collection<Object[]> result = new ArrayList<>();
        result.add(new Object[] {"fileformat/RisImporterTest1.ris", "RIS", 1});
        result.add(new Object[] {"fileformat/IsiImporterTest1.isi", "ISI", 1});
        result.add(new Object[] {"fileformat/SilverPlatterImporterTest1.txt", "SilverPlatter", 1});
        result.add(new Object[] {"fileformat/RepecNepImporterTest2.txt", "RepecNep", 1});
        result.add(new Object[] {"fileformat/OvidImporterTest3.txt", "Ovid", 1});
        result.add(new Object[] {"fileformat/Endnote.entries.enw", "Endnote", 5});
        result.add(new Object[] {"fileformat/MsBibImporterTest4.xml", "MsBib", 1});
        return result;
    }
}
