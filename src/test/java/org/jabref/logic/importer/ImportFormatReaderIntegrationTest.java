package org.jabref.logic.importer;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;

import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ImportFormatReaderIntegrationTest {

    private ImportFormatReader reader;

    @BeforeEach
    void setUp() {
        reader = new ImportFormatReader();
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.getEncoding()).thenReturn(StandardCharsets.UTF_8);
        reader.resetImportFormats(importFormatPreferences, mock(XmpPreferences.class), new DummyFileUpdateMonitor());
    }

    @ParameterizedTest
    @MethodSource("importFormats")
    void testImportUnknownFormat(String resource, String format, int count) throws Exception {
        Path file = Paths.get(ImportFormatReaderIntegrationTest.class.getResource(resource).toURI());
        ImportFormatReader.UnknownFormatImport unknownFormat = reader.importUnknownFormat(file, new DummyFileUpdateMonitor());
        assertEquals(count, unknownFormat.parserResult.getDatabase().getEntryCount());
    }

    @ParameterizedTest
    @MethodSource("importFormats")
    void testImportFormatFromFile(String resource, String format, int count) throws Exception {
        Path file = Paths.get(ImportFormatReaderIntegrationTest.class.getResource(resource).toURI());
        assertEquals(count, reader.importFromFile(format, file).getDatabase().getEntries().size());
    }

    @ParameterizedTest
    @MethodSource("importFormats")
    void testImportUnknownFormatFromString(String resource, String format, int count) throws Exception {
        Path file = Paths.get(ImportFormatReaderIntegrationTest.class.getResource(resource).toURI());
        String data = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        assertEquals(count, reader.importUnknownFormat(data).parserResult.getDatabase().getEntries().size());
    }

    private static Stream<Object[]> importFormats() {
        Collection<Object[]> result = new ArrayList<>();
        result.add(new Object[] {"fileformat/RisImporterTest1.ris", "ris", 1});
        result.add(new Object[] {"fileformat/IsiImporterTest1.isi", "isi", 1});
        result.add(new Object[] {"fileformat/SilverPlatterImporterTest1.txt", "silverplatter", 1});
        result.add(new Object[] {"fileformat/RepecNepImporterTest2.txt", "repecnep", 1});
        result.add(new Object[] {"fileformat/OvidImporterTest3.txt", "ovid", 1});
        result.add(new Object[] {"fileformat/Endnote.entries.enw", "refer", 5});
        result.add(new Object[] {"fileformat/MsBibImporterTest4.xml", "msbib", 1});
        result.add(new Object[] {"fileformat/MsBibImporterTest4.bib", "bibtex", 1});
        return result.stream();
    }

}
