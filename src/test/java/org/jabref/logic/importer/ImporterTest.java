package org.jabref.logic.importer;

import java.io.BufferedReader;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.jabref.logic.importer.fileformat.BibTeXMLImporter;
import org.jabref.logic.importer.fileformat.BiblioscapeImporter;
import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.logic.importer.fileformat.CopacImporter;
import org.jabref.logic.importer.fileformat.EndnoteImporter;
import org.jabref.logic.importer.fileformat.InspecImporter;
import org.jabref.logic.importer.fileformat.IsiImporter;
import org.jabref.logic.importer.fileformat.MedlineImporter;
import org.jabref.logic.importer.fileformat.MedlinePlainImporter;
import org.jabref.logic.importer.fileformat.ModsImporter;
import org.jabref.logic.importer.fileformat.MsBibImporter;
import org.jabref.logic.importer.fileformat.OvidImporter;
import org.jabref.logic.importer.fileformat.PdfContentImporter;
import org.jabref.logic.importer.fileformat.PdfXmpImporter;
import org.jabref.logic.importer.fileformat.RepecNepImporter;
import org.jabref.logic.importer.fileformat.RisImporter;
import org.jabref.logic.importer.fileformat.SilverPlatterImporter;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ImporterTest {

    @ParameterizedTest
    @MethodSource("instancesToTest")
    public void isRecognizedFormatWithNullForBufferedReaderThrowsException(Importer format) {
        assertThrows(NullPointerException.class, () -> format.isRecognizedFormat((BufferedReader) null));
    }

    @ParameterizedTest
    @MethodSource("instancesToTest")
    public void isRecognizedFormatWithNullForStringThrowsException(Importer format) {
        assertThrows(NullPointerException.class, () -> format.isRecognizedFormat((String) null));
    }

    @ParameterizedTest
    @MethodSource("instancesToTest")
    public void importDatabaseWithNullForBufferedReaderThrowsException(Importer format) {
        assertThrows(NullPointerException.class, () -> format.importDatabase((BufferedReader) null));
    }

    @ParameterizedTest
    @MethodSource("instancesToTest")
    public void importDatabaseWithNullForStringThrowsException(Importer format) {
        assertThrows(NullPointerException.class, () -> format.importDatabase((String) null));
    }

    @ParameterizedTest
    @MethodSource("instancesToTest")
    public void getFormatterNameDoesNotReturnNull(Importer format) {
        assertNotNull(format.getName());
    }

    @ParameterizedTest
    @MethodSource("instancesToTest")
    public void getFileTypeDoesNotReturnNull(Importer format) {
        assertNotNull(format.getFileType());
    }

    @ParameterizedTest
    @MethodSource("instancesToTest")
    public void getIdDoesNotReturnNull(Importer format) {
        assertNotNull(format.getId());
    }

    @ParameterizedTest
    @MethodSource("instancesToTest")
    public void getIdDoesNotContainWhitespace(Importer format) {
        Pattern whitespacePattern = Pattern.compile("\\s");
        assertFalse(whitespacePattern.matcher(format.getId()).find());
    }

    @ParameterizedTest
    @MethodSource("instancesToTest")
    public void getIdStripsSpecialCharactersAndConvertsToLowercase(Importer format) {
        Importer importer = mock(Importer.class, Mockito.CALLS_REAL_METHODS);
        when(importer.getName()).thenReturn("*Test-Importer");
        assertEquals("testimporter", importer.getId());
    }

    @ParameterizedTest
    @MethodSource("instancesToTest")
    public void getDescriptionDoesNotReturnNull(Importer format) {
        assertNotNull(format.getDescription());
    }

    public static Stream<Importer> instancesToTest() {
        // all classes implementing {@link Importer}
        // sorted alphabetically

        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class);
        XmpPreferences xmpPreferences = mock(XmpPreferences.class);
        // @formatter:off
        return Stream.of(
                new BiblioscapeImporter(),
                new BibtexImporter(importFormatPreferences, new DummyFileUpdateMonitor()),
                new BibTeXMLImporter(),
                new CopacImporter(),
                new EndnoteImporter(importFormatPreferences),
                new InspecImporter(),
                new IsiImporter(),
                new MedlineImporter(),
                new MedlinePlainImporter(),
                new ModsImporter(importFormatPreferences),
                new MsBibImporter(),
                new OvidImporter(),
                new PdfContentImporter(importFormatPreferences),
                new PdfXmpImporter(xmpPreferences),
                new RepecNepImporter(importFormatPreferences),
                new RisImporter(),
                new SilverPlatterImporter()
        );
        // @formatter:on
    }
}
