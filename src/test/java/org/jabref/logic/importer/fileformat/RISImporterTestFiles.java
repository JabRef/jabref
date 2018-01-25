package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.jabref.logic.bibtex.BibEntryAssert;
import org.jabref.model.entry.BibEntry;

import org.junit.Assert;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class RISImporterTestFiles {

    private RisImporter risImporter;

    private Path risFile;

    public void setUp(String fileName) throws IOException {
        risImporter = new RisImporter();
        try {
            risFile = Paths.get(RISImporterTest.class.getResource(fileName + ".ris").toURI());
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    private static Stream<String> fileNames() {
        return Stream.of("RisImporterTest1", "RisImporterTest3", "RisImporterTest4a", "RisImporterTest4b",
                "RisImporterTest4c", "RisImporterTest5a", "RisImporterTest5b", "RisImporterTest6", "RisImporterTest7",
                "RisImporterTestDoiAndJournalTitle", "RisImporterTestScopus", "RisImporterTestScience");
    }

    @ParameterizedTest
    @MethodSource("fileNames")
    public void testIsRecognizedFormat(String fileName) throws IOException {
        setUp(fileName);

        Assert.assertTrue(risImporter.isRecognizedFormat(risFile, StandardCharsets.UTF_8));
    }

    @ParameterizedTest
    @MethodSource("fileNames")
    public void testImportEntries(String fileName) throws IOException {
        setUp(fileName);

        List<BibEntry> risEntries = risImporter.importDatabase(risFile, StandardCharsets.UTF_8).getDatabase()
                .getEntries();
        BibEntryAssert.assertEquals(RISImporterTest.class, fileName + ".bib", risEntries);
    }
}
