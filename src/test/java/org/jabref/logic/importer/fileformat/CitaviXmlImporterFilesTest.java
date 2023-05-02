package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class CitaviXmlImporterFilesTest {

    private static final String FILE_ENDING = ".ctv6bak";
    private final CitaviXmlImporter citaviXmlImporter = new CitaviXmlImporter();

    private static Stream<String> fileNames() throws IOException {
        Predicate<String> fileName = name -> name.startsWith("CitaviXmlImporterTest") && name.endsWith(FILE_ENDING);
        return ImporterTestEngine.getTestFiles(fileName).stream();
    }

    private static Stream<String> invalidFileNames() throws IOException {
        Predicate<String> fileName = name -> !name.startsWith("CitaviXmlImporterTest");
        return ImporterTestEngine.getTestFiles(fileName).stream();
    }

    @ParameterizedTest
    @MethodSource("fileNames")
    void testIsRecognizedFormat(String fileName) throws IOException {
        ImporterTestEngine.testIsRecognizedFormat(citaviXmlImporter, fileName);
    }

    @ParameterizedTest
    @MethodSource("invalidFileNames")
    void testIsNotRecognizedFormat(String fileName) throws IOException {
        ImporterTestEngine.testIsNotRecognizedFormat(citaviXmlImporter, fileName);
    }

    @ParameterizedTest
    @MethodSource("fileNames")
    void testImportEntries(String fileName) throws Exception {
        ImporterTestEngine.testImportEntries(citaviXmlImporter, fileName, FILE_ENDING);
    }
}
