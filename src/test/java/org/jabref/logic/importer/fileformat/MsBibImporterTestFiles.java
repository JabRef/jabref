package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class MsBibImporterTestFiles {

    private static final String FILE_ENDING = ".xml";

    private static Stream<String> fileNames() throws IOException {
        Predicate<String> fileName = name -> name.startsWith("MsBib")
                && name.endsWith(FILE_ENDING);
        return ImporterTestEngine.getTestFiles(fileName).stream();
    }

    private static Stream<String> invalidFileNames() throws IOException {
        Predicate<String> fileName = name -> !name.contains("MsBib");
        return ImporterTestEngine.getTestFiles(fileName).stream();
    }

    @ParameterizedTest
    @MethodSource("fileNames")
    public void testIsRecognizedFormat(String fileName) throws IOException {
        ImporterTestEngine.testIsRecognizedFormat(new MsBibImporter(), fileName);
    }

    @ParameterizedTest
    @MethodSource("invalidFileNames")
    public void testIsNotRecognizedFormat(String fileName) throws IOException {
        ImporterTestEngine.testIsNotRecognizedFormat(new MsBibImporter(), fileName);
    }

    @ParameterizedTest
    @MethodSource("fileNames")
    public void testImportEntries(String fileName) throws Exception {
        ImporterTestEngine.testImportEntries(new MsBibImporter(), fileName, FILE_ENDING);
    }

}
