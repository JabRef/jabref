package org.jabref.logic.util;

import org.jabref.logic.util.io.FileNameCleaner;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FileNameCleanerTest {

    @Test
    void cleanFileName() {
        assertEquals("legalFilename.txt", FileNameCleaner.cleanFileName("legalFilename.txt"));
        assertEquals("illegalFilename______.txt", FileNameCleaner.cleanFileName("illegalFilename/?*<>|.txt"));
        assertEquals("illegalFileName_.txt", FileNameCleaner.cleanFileName("illegalFileName{.txt"));
    }

    @Test
    void cleanDirectoryName() {
        assertEquals("legalFilename.txt", FileNameCleaner.cleanDirectoryName("legalFilename.txt"));
        assertEquals("subdir/legalFilename.txt", FileNameCleaner.cleanDirectoryName("subdir/legalFilename.txt"));
        assertEquals("illegalFilename/_____.txt", FileNameCleaner.cleanDirectoryName("illegalFilename/?*<>|.txt"));
    }

    @Test
    void cleanDirectoryNameForWindows() {
        assertEquals("legalFilename.txt", FileNameCleaner.cleanDirectoryName("legalFilename.txt"));
        assertEquals("subdir\\legalFilename.txt", FileNameCleaner.cleanDirectoryName("subdir\\legalFilename.txt"));
        assertEquals("illegalFilename\\_____.txt", FileNameCleaner.cleanDirectoryName("illegalFilename\\?*<>|.txt"));
    }

    @Test
    void cleanCurlyBracesAsWell() {
        assertEquals("The Evolution of Sentiment_ Analysis_A Review of Research Topics, Venues, and Top Cited Papers.PDF", FileNameCleaner.cleanFileName("The Evolution of Sentiment} Analysis}A Review of Research Topics, Venues, and Top Cited Papers.PDF"));
    }
}
