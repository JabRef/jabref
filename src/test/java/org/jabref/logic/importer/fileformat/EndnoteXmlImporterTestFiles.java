package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jabref.logic.importer.ImportFormatPreferences;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EndnoteXmlImporterTestFiles {

    private static final String FILE_ENDING = ".xml";
    private ImportFormatPreferences preferences;

    private static Stream<String> fileNames() throws IOException {
        Predicate<String> fileName = name -> name.startsWith("EndnoteXmlImporterTest") && name.endsWith(FILE_ENDING);
        return ImporterTestEngine.getTestFiles(fileName).stream();
    }

    private static Stream<String> invalidFileNames() throws IOException {
        Predicate<String> fileName = name -> !name.startsWith("EndnoteXmlImporterTest");
        return ImporterTestEngine.getTestFiles(fileName).stream();
    }

    @BeforeEach
    void setUp() {
        preferences = mock(ImportFormatPreferences.class);
        when(preferences.getKeywordSeparator()).thenReturn(';');
    }

    @ParameterizedTest
    @MethodSource("fileNames")
    void testIsRecognizedFormat(String fileName) throws IOException {
        ImporterTestEngine.testIsRecognizedFormat(new EndnoteXmlImporter(preferences), fileName);
    }

    @ParameterizedTest
    @MethodSource("invalidFileNames")
    void testIsNotRecognizedFormat(String fileName) throws IOException {
        ImporterTestEngine.testIsNotRecognizedFormat(new EndnoteXmlImporter(preferences), fileName);
    }

    @ParameterizedTest
    @MethodSource("fileNames")
    void testImportEntries(String fileName) throws Exception {
        ImporterTestEngine.testImportEntries(new EndnoteXmlImporter(preferences), fileName, FILE_ENDING);
    }
}
