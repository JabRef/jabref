package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jabref.logic.importer.ImportFormatPreferences;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.mockito.Mockito.mock;

class PdfContentImporterTestFiles {

    private static final String FILE_ENDING = ".pdf";

    private static Stream<String> fileNames() throws IOException {
        Predicate<String> fileName = name -> name.startsWith("LNCS-minimal")
                && name.endsWith(FILE_ENDING);
        return ImporterTestEngine.getTestFiles(fileName).stream();
    }

    @ParameterizedTest
    @MethodSource("fileNames")
    void testIsRecognizedFormat(String fileName) throws IOException {
        ImporterTestEngine.testIsRecognizedFormat(new PdfContentImporter(mock(ImportFormatPreferences.class)), fileName);
    }

    @ParameterizedTest
    @MethodSource("fileNames")
    @Disabled("bib file does not contain linked file")
    void testImportEntries(String fileName) throws Exception {
        ImporterTestEngine.testImportEntries(new PdfContentImporter(mock(ImportFormatPreferences.class)), fileName, FILE_ENDING);
    }
}
