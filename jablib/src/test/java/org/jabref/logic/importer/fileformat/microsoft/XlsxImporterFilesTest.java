package org.jabref.logic.importer.fileformat.microsoft;

import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jabref.logic.importer.ImportException;
import org.jabref.logic.importer.fileformat.ImporterTestEngine;
import org.jabref.logic.importer.util.Constants;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class XlsxImporterFilesTest {
    private static final String FILE_ENDING = ".xlsx";
    private static final List<String> EXCLUDE_EXTENSIONS = Constants.ZIP_FILES_EXTENSIONS
            .stream()
            .filter(ext -> !ext.equals(FILE_ENDING))
            .toList();

    private XlsxImporter importer;

    @BeforeEach
    void setUp() {
        importer = new XlsxImporter();
    }

    private static Stream<String> fileNames() throws IOException {
        Predicate<String> fileName = name -> name.startsWith("XlsxImporterTest") && name.endsWith(FILE_ENDING);
        return ImporterTestEngine.getTestFiles(fileName).stream();
    }

    private static Stream<String> invalidFileNames() throws IOException {
        Predicate<String> fileName = name -> !name.startsWith("XlsxImporterTest") && EXCLUDE_EXTENSIONS.stream().noneMatch(name::endsWith);
        return ImporterTestEngine.getTestFiles(fileName).stream();
    }

    @ParameterizedTest
    @MethodSource("fileNames")
    void isRecognizedFormat(String fileName) throws IOException {
        ImporterTestEngine.testIsRecognizedFormat(importer, fileName);
    }

    @ParameterizedTest
    @MethodSource("invalidFileNames")
    void isNotRecognizedFormat(String fileName) throws IOException {
        ImporterTestEngine.testIsNotRecognizedFormat(importer, fileName);
    }

    @ParameterizedTest
    @MethodSource("fileNames")
    void importEntries(String fileName) throws ImportException, IOException {
        ImporterTestEngine.testImportEntries(importer, fileName, FILE_ENDING);
    }
}
