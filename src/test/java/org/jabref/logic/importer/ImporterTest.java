package org.jabref.logic.importer;

import java.io.BufferedReader;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.jabref.logic.importer.fileformat.BiblioscapeImporter;
import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.logic.importer.fileformat.CitaviXmlImporter;
import org.jabref.logic.importer.fileformat.CopacImporter;
import org.jabref.logic.importer.fileformat.EndnoteImporter;
import org.jabref.logic.importer.fileformat.InspecImporter;
import org.jabref.logic.importer.fileformat.IsiImporter;
import org.jabref.logic.importer.fileformat.MedlineImporter;
import org.jabref.logic.importer.fileformat.MedlinePlainImporter;
import org.jabref.logic.importer.fileformat.ModsImporter;
import org.jabref.logic.importer.fileformat.MsBibImporter;
import org.jabref.logic.importer.fileformat.OvidImporter;
import org.jabref.logic.importer.fileformat.PdfMergeMetadataImporter;
import org.jabref.logic.importer.fileformat.RepecNepImporter;
import org.jabref.logic.importer.fileformat.RisImporter;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ImporterTest {

    @ParameterizedTest
    @MethodSource("instancesToTest")
    void isRecognizedFormatWithNullForBufferedReaderThrowsException(Importer format) {
        assertThrows(NullPointerException.class, () -> format.isRecognizedFormat((BufferedReader) null));
    }

    @ParameterizedTest
    @MethodSource("instancesToTest")
    void isRecognizedFormatWithNullForStringThrowsException(Importer format) {
        assertThrows(NullPointerException.class, () -> format.isRecognizedFormat((String) null));
    }

    @ParameterizedTest
    @MethodSource("instancesToTest")
    void getFormatterNameDoesNotReturnNull(Importer format) {
        assertNotNull(format.getName());
    }

    @ParameterizedTest
    @MethodSource("instancesToTest")
    void getFileTypeDoesNotReturnNull(Importer format) {
        assertNotNull(format.getFileType());
    }

    @ParameterizedTest
    @MethodSource("instancesToTest")
    void getIdDoesNotReturnNull(Importer format) {
        assertNotNull(format.getId());
    }

    @ParameterizedTest
    @MethodSource("instancesToTest")
    void getIdDoesNotContainWhitespace(Importer format) {
        Pattern whitespacePattern = Pattern.compile("\\s");
        assertFalse(whitespacePattern.matcher(format.getId()).find());
    }

    @ParameterizedTest
    @MethodSource("instancesToTest")
    void getDescriptionDoesNotReturnNull(Importer format) {
        assertNotNull(format.getDescription());
    }

    public static Stream<Importer> instancesToTest() {
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.bibEntryPreferences().getKeywordSeparator()).thenReturn(',');
        XmpPreferences xmpPreferences = mock(XmpPreferences.class);
        return Stream.of(
                // all classes implementing {@link Importer}
                // sorted alphabetically
                new BiblioscapeImporter(),
                new BibtexImporter(importFormatPreferences, new DummyFileUpdateMonitor()),
                new CitaviXmlImporter(),
                new CopacImporter(),
                new EndnoteImporter(),
                new InspecImporter(),
                new IsiImporter(),
                new MedlineImporter(),
                new MedlinePlainImporter(importFormatPreferences),
                new ModsImporter(importFormatPreferences),
                new MsBibImporter(),
                new OvidImporter(),
                new PdfMergeMetadataImporter(importFormatPreferences),
                new RepecNepImporter(importFormatPreferences),
                new RisImporter()
        );
    }
}
