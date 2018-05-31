package org.jabref.logic.importer;

import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;

import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Answers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.getEncoding()).thenReturn(StandardCharsets.UTF_8);
        reader.resetImportFormats(importFormatPreferences, mock(XmpPreferences.class), new DummyFileUpdateMonitor());
    }

    @Test
    public void testImportUnknownFormat() throws Exception {
        ImportFormatReader.UnknownFormatImport unknownFormat = reader.importUnknownFormat(file, new DummyFileUpdateMonitor());
        assertEquals(count, unknownFormat.parserResult.getDatabase().getEntryCount());
    }

    @Test
    public void testImportFormatFromFile() throws Exception {
        assertEquals(count, reader.importFromFile(format, file).getDatabase().getEntries().size());
    }

    @Test
    public void testImportUnknownFormatFromString() throws Exception {
        String data = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        assertEquals(count, reader.importUnknownFormat(data).parserResult.getDatabase().getEntries().size());
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
