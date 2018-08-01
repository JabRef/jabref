package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class BibTeXMLImporterTestFiles {

    private static final String FILE_ENDING = ".xml";

    @SuppressWarnings("unused")
    private static Stream<String> fileNames() throws IOException {
        Predicate<String> fileName = name -> name.startsWith("BibTeXMLImporterTest")
                && name.endsWith(FILE_ENDING);
        return ImporterTestEngine.getTestFiles(fileName).stream();
    }

    @SuppressWarnings("unused")
    private static Stream<String> nonBibTeXMLfileNames() throws IOException {
        Predicate<String> fileName = name -> !name.startsWith("BibTeXMLImporterTest");
        return ImporterTestEngine.getTestFiles(fileName).stream();
    }

    @ParameterizedTest
    @MethodSource("fileNames")
    public void testIsRecognizedFormat(String fileName) throws IOException {
        ImporterTestEngine.testIsRecognizedFormat(new BibTeXMLImporter(), fileName);
    }

    @ParameterizedTest
    @MethodSource("nonBibTeXMLfileNames")
    public void testIsNotRecognizedFormat(String fileName) throws IOException {
        ImporterTestEngine.testIsNotRecognizedFormat(new BibTeXMLImporter(), fileName);
    }

    @ParameterizedTest
    @MethodSource("fileNames")
    public void testImportEntries(String fileName) throws Exception {
        ImporterTestEngine.testImportEntries(new BibTeXMLImporter(), fileName, FILE_ENDING);
    }

}
