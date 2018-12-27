package org.jabref.logic.util.io;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FileNameCleanerTest {

    @Test
    public void testCleanFileName() {
        assertEquals("legalFilename.txt", FileNameCleaner.cleanFileName("legalFilename.txt"));
        assertEquals("illegalFilename______.txt", FileNameCleaner.cleanFileName("illegalFilename/?*<>|.txt"));
    }

    @Test
    public void testCleanDirectoryName() {
        assertEquals("legalFilename.txt", FileNameCleaner.cleanDirectoryName("legalFilename.txt"));
        assertEquals("subdir/legalFilename.txt", FileNameCleaner.cleanDirectoryName("subdir/legalFilename.txt"));
        assertEquals("illegalFilename/_____.txt", FileNameCleaner.cleanDirectoryName("illegalFilename/?*<>|.txt"));
    }

    @Test
    public void testCleanDirectoryNameForWindows() {
        assertEquals("legalFilename.txt", FileNameCleaner.cleanDirectoryName("legalFilename.txt"));
        assertEquals("subdir\\legalFilename.txt", FileNameCleaner.cleanDirectoryName("subdir\\legalFilename.txt"));
        assertEquals("illegalFilename\\_____.txt", FileNameCleaner.cleanDirectoryName("illegalFilename\\?*<>|.txt"));
    }
}
