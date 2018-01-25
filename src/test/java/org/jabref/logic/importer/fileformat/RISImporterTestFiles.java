package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class RISImporterTestFiles {

    private static Stream<String> fileNames() {
        return Stream.of("RisImporterTest1", "RisImporterTest3", "RisImporterTest4a", "RisImporterTest4b",
                "RisImporterTest4c", "RisImporterTest5a", "RisImporterTest5b", "RisImporterTest6", "RisImporterTest7",
                "RisImporterTestDoiAndJournalTitle", "RisImporterTestScopus", "RisImporterTestScience");
    }

    @ParameterizedTest
    @MethodSource("fileNames")
    public void testIsRecognizedFormat(String fileName) throws IOException {
        ImporterTestFiles.testIsRecognizedFormat(new RisImporter(), fileName, ".ris");
    }

    @ParameterizedTest
    @MethodSource("fileNames")
    public void testImportEntries(String fileName) throws IOException {
        ImporterTestFiles.testImportEntries(new RisImporter(), fileName, ".ris");
    }
}
