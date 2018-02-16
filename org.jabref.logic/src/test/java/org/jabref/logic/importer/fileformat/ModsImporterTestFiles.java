package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jabref.logic.importer.ImportFormatPreferences;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;

import static org.mockito.Mockito.mock;

public class ModsImporterTestFiles {

    private static final String FILE_ENDING = ".xml";

    private static Stream<String> fileNames() throws IOException {
        Predicate<String> fileName = name -> name.startsWith("MODS")
                && name.endsWith(FILE_ENDING);
        return ImporterTestEngine.getTestFiles(fileName).stream();
    }

    @ParameterizedTest
    @MethodSource("fileNames")
    public void testIsRecognizedFormat(String fileName) throws IOException {
        ImporterTestEngine.testIsRecognizedFormat(new ModsImporter(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS)), fileName);
    }

    @ParameterizedTest
    @MethodSource("fileNames")
    public void testImportEntries(String fileName) throws Exception {
        ImporterTestEngine.testImportEntries(new ModsImporter(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS)
        ), fileName, FILE_ENDING);
    }
}
