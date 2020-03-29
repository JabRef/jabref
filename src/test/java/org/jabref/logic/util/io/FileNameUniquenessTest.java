package org.jabref.logic.util.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class FileNameUniquenessTest {

    @TempDir
    protected Path tempDir;

    @Test
    public void testGetNonOverWritingFileNameReturnsSameName() throws IOException {
        assertFalse(Files.exists(tempDir.resolve("sameFile.txt")));

        String outputFileName = FileNameUniqueness.getNonOverWritingFileName(tempDir, "sameFile.txt");
        assertEquals("sameFile.txt", outputFileName);
    }

    @Test
    public void testGetNonOverWritingFileNameReturnsUniqueNameOver1Conflict() throws IOException {
        Path dummyFilePath1 = tempDir.resolve("differentFile.txt");

        Files.createFile(dummyFilePath1);

        String outputFileName = FileNameUniqueness.getNonOverWritingFileName(tempDir, "differentFile.txt");
        assertEquals("differentFile (1).txt", outputFileName);
    }

    @Test
    public void testGetNonOverWritingFileNameReturnsUniqueNameOverNConflicts() throws IOException {
        Path dummyFilePath1 = tempDir.resolve("manyfiles.txt");
        Path dummyFilePath2 = tempDir.resolve("manyfiles (1).txt");

        Files.createFile(dummyFilePath1);
        Files.createFile(dummyFilePath2);

        String outputFileName = FileNameUniqueness.getNonOverWritingFileName(tempDir, "manyfiles.txt");
        assertEquals("manyfiles (2).txt", outputFileName);
    }
}
