package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jabref.logic.importer.Importer;
import org.jabref.logic.util.StandardFileType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SilverPlatterImporterTest {

    private static final String FILE_ENDING = ".txt";

    private Importer testImporter;

    @BeforeEach
    void setUp() throws Exception {
        testImporter = new SilverPlatterImporter();
    }

    private static Stream<String> fileNames() throws IOException {
        Predicate<String> fileName = name -> name.startsWith("SilverPlatterImporterTest") && name.endsWith(FILE_ENDING);
        return ImporterTestEngine.getTestFiles(fileName).stream();
    }

    private static Stream<String> invalidFileNames() throws IOException {
        Predicate<String> fileName = name -> !name.startsWith("SilverPlatterImporterTest");
        return ImporterTestEngine.getTestFiles(fileName).stream();
    }

    @ParameterizedTest
    @MethodSource("fileNames")
    void testIsRecognizedFormat(String fileName) throws IOException {
        ImporterTestEngine.testIsRecognizedFormat(testImporter, fileName);
    }

    @ParameterizedTest
    @MethodSource("invalidFileNames")
    void testIsNotRecognizedFormat(String fileName) throws IOException {
        ImporterTestEngine.testIsNotRecognizedFormat(testImporter, fileName);
    }

    @ParameterizedTest
    @MethodSource("fileNames")
    void testImportEntries(String fileName) throws Exception {
        ImporterTestEngine.testImportEntries(testImporter, fileName, FILE_ENDING);
    }

    @Test
    void testsGetExtensions() {
        assertEquals(StandardFileType.SILVER_PLATTER, testImporter.getFileType());
    }

    @Test
    void testGetDescription() {
        assertEquals("Imports a SilverPlatter exported file.", testImporter.getDescription());
    }
}
