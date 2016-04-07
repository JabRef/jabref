package net.sf.jabref.importer;

import static org.junit.Assert.*;

import java.io.IOException;
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
        String fileName = ImportFormatReaderTest.class.getResource(resourceName).getFile();
        ImportFormatReader.UnknownFormatImport unknownFormat = reader.importUnknownFormat(fileName);
        assertEquals(count, unknownFormat.parserResult.getDatabase().getEntryCount());
    }

    @Test
    public void testImportFormatFromFile() throws IOException {
        String fileName = ImportFormatReaderTest.class.getResource(resourceName).getFile();
        OutputPrinter nullPrinter = new OutputPrinterToNull();
        assertEquals(count, reader.importFromFile(format, fileName, nullPrinter).size());
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
        return result;
    }

}
