package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.util.StandardFileType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RepecNepImporterTest {

    private static final String FILE_ENDING = ".txt";

    private RepecNepImporter testImporter;

    @BeforeEach
    public void setUp() {
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class);
        when(importFormatPreferences.getKeywordSeparator()).thenReturn(',');
        testImporter = new RepecNepImporter(importFormatPreferences);
    }

    private static Stream<String> fileNames() throws IOException {
        Predicate<String> fileName = name -> name.startsWith("RepecNepImporter")
                && name.endsWith(FILE_ENDING);
        return ImporterTestEngine.getTestFiles(fileName).stream();
    }

    private static Stream<String> invalidFileNames() throws IOException {
        Predicate<String> fileName = name -> !name.contains("RepecNepImporter");
        return ImporterTestEngine.getTestFiles(fileName).stream();
    }

    @ParameterizedTest
    @MethodSource("fileNames")
    public void testIsRecognizedFormat(String fileName) throws IOException {
        ImporterTestEngine.testIsRecognizedFormat(testImporter, fileName);
    }

    @ParameterizedTest
    @MethodSource("invalidFileNames")
    public void testIsNotRecognizedFormat(String fileName) throws IOException {
        ImporterTestEngine.testIsNotRecognizedFormat(testImporter, fileName);
    }

    @ParameterizedTest
    @MethodSource("fileNames")
    public void testImportEntries(String fileName) throws Exception {
        ImporterTestEngine.testImportEntries(testImporter, fileName, FILE_ENDING);
    }

    @Test
    public final void testGetFormatName() {
        assertEquals("REPEC New Economic Papers (NEP)", testImporter.getName());
    }

    @Test
    public final void testGetCliId() {
        assertEquals("repecnep", testImporter.getId());
    }

    @Test
    public void testGetExtension() {
        assertEquals(StandardFileType.TXT, testImporter.getFileType());
    }

    @Test
    public final void testGetDescription() {
        assertEquals("Imports a New Economics Papers-Message from the REPEC-NEP Service.",
                testImporter.getDescription());
    }
}
