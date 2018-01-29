package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jabref.logic.bibtex.BibEntryAssert;
import org.jabref.logic.importer.Importer;
import org.jabref.logic.util.FileType;
import org.jabref.model.entry.BibEntry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;

public class SilverPlatterImporterTest {

    private static final String FILE_ENDING = ".txt";

    private Importer testImporter;

    @BeforeEach
    public void setUp() throws Exception {
        testImporter = new SilverPlatterImporter();
    }


    private static Stream<String> fileNames() throws IOException {
        Predicate<String> fileName = name -> name.startsWith("SilverPlatterImporterTest") && name.endsWith(FILE_ENDING);
        return ImporterTestEngine.getTestFiles(fileName).stream();
    }

    @ParameterizedTest
    @MethodSource("fileNames")
    public void testIsRecognizedFormat(String fileName) throws IOException {
        ImporterTestEngine.testIsRecognizedFormat(testImporter, fileName);
    }

    @ParameterizedTest
    @MethodSource("fileNames")
    public void testImportEntries(String fileName) throws IOException {
        ImporterTestEngine.testImportEntries(testImporter, fileName, FILE_ENDING);
    }

    @Test
    public void testsGetExtensions() {
        assertEquals(FileType.SILVER_PLATTER, testImporter.getFileType());
    }

    @Test
    public void testGetDescription() {
        assertEquals("Imports a SilverPlatter exported file.", testImporter.getDescription());
    }
    
}
