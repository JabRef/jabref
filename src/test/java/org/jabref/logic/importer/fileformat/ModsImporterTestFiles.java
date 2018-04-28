package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jabref.logic.importer.ImportFormatPreferences;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ModsImporterTestFiles {

    private static final String FILE_ENDING = ".xml";
    private ImportFormatPreferences importFormatPreferences;

    private static Stream<String> fileNames() throws IOException {
        Predicate<String> fileName = name -> name.startsWith("MODS") && name.endsWith(FILE_ENDING);
        return ImporterTestEngine.getTestFiles(fileName).stream();
    }

    @BeforeEach
    void setUp() {
        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.getKeywordSeparator()).thenReturn(',');
    }

    @ParameterizedTest
    @MethodSource("fileNames")
    void testIsRecognizedFormat(String fileName) throws IOException {
        ImporterTestEngine.testIsRecognizedFormat(new ModsImporter(importFormatPreferences), fileName);
    }

    @ParameterizedTest
    @MethodSource("fileNames")
    void testImportEntries(String fileName) throws Exception {
        ImporterTestEngine.testImportEntries(new ModsImporter(importFormatPreferences), fileName, FILE_ENDING);
    }
}
