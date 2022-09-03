package org.jabref.model.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.logic.util.OS;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileHelperTest {

    @Test
    public void extractFileExtension() {
        final String filePath = FileHelperTest.class.getResource("pdffile.pdf").getPath();
        assertEquals(Optional.of("pdf"), FileHelper.getFileExtension(filePath));
    }

    @Test
    public void fileExtensionFromUrl() {
        final String filePath = "https://link.springer.com/content/pdf/10.1007%2Fs40955-018-0121-9.pdf";
        assertEquals(Optional.of("pdf"), FileHelper.getFileExtension(filePath));
    }

    @Test
    public void testFileNameEmpty() {
        Path path = Path.of("/");
        assertEquals(Optional.of(path), FileHelper.find("", path));
    }

    @ParameterizedTest
    @ValueSource(strings = {"*", "?", ">", "\""})
    public void testFileNameIllegal(String fileName) {
        Path path = Path.of("/");
        assertEquals(Optional.empty(), FileHelper.find(fileName, path));
    }

    @Test
    public void testFindsFileInDirectory(@TempDir Path temp) throws Exception {
        Path firstFilePath = temp.resolve("files");
        Files.createDirectories(firstFilePath);
        Path firstFile = Files.createFile(firstFilePath.resolve("test.pdf"));

        assertEquals(Optional.of(firstFile), FileHelper.find("test.pdf", temp.resolve("files")));
    }

    @Test
    public void testFindsFileStartingWithTheSameDirectory(@TempDir Path temp) throws Exception {
        Path firstFilePath = temp.resolve("files");
        Files.createDirectories(firstFilePath);
        Path firstFile = Files.createFile(firstFilePath.resolve("test.pdf"));

        assertEquals(Optional.of(firstFile), FileHelper.find("files/test.pdf", temp.resolve("files")));
    }

    @Test
    public void testDoesNotFindsFileStartingWithTheSameDirectoryHasASubdirectory(@TempDir Path temp) throws Exception {
        Path firstFilesPath = temp.resolve("files");
        Path secondFilesPath = firstFilesPath.resolve("files");
        Files.createDirectories(secondFilesPath);
        Path testFile = secondFilesPath.resolve("test.pdf");
        Files.createFile(testFile);
        assertEquals(Optional.of(testFile.toAbsolutePath()), FileHelper.find("files/test.pdf", firstFilesPath));
    }

    public void testCTemp() {
        String fileName = "c:\\temp.pdf";
        if (OS.WINDOWS) {
            assertFalse(FileHelper.detectBadFileName(fileName));
        } else {
            assertTrue(FileHelper.detectBadFileName(fileName));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"/mnt/tmp/test.pdf"})
    public void legalPaths(String fileName) {
        assertFalse(FileHelper.detectBadFileName(fileName));
    }

    @ParameterizedTest
    @ValueSource(strings = {"te{}mp.pdf"})
    public void illegalPaths(String fileName) {
        assertTrue(FileHelper.detectBadFileName(fileName));
    }
}
