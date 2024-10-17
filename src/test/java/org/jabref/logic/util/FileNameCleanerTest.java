package org.jabref.logic.util;

import org.jabref.logic.util.io.FileNameCleaner;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FileNameCleanerTest {

    @ParameterizedTest
    @CsvSource({
        "legalFilename.txt, legalFilename.txt",
        "illegalFilename/?*<>|.txt, illegalFilename______.txt",
        "illegalFileName{.txt, illegalFileName_.txt",
        "The Evolution of Sentiment} Analysis}A Review of Research Topics, Venues, and Top Cited Papers.PDF, The Evolution of Sentiment_ Analysis_A Review of Research Topics, Venues, and Top Cited Papers.PDF"
    })
    void cleanFileName(String input, String expected) {
        assertEquals(expected, FileNameCleaner.cleanFileName(input));
    }

    @ParameterizedTest
    @CsvSource({
        "legalFilename.txt, legalFilename.txt",
        "subdir/legalFilename.txt, subdir/legalFilename.txt",
        "illegalFilename/?*<>|.txt, illegalFilename/_____.txt"
    })
    void cleanDirectoryName(String input, String expected) {
        assertEquals(expected, FileNameCleaner.cleanDirectoryName(input));
    }

    @ParameterizedTest
    @CsvSource({
        "legalFilename.txt, legalFilename.txt",
        "subdir\\legalFilename.txt, subdir\\legalFilename.txt",
        "illegalFilename\\?*<>|.txt, illegalFilename\\_____.txt"
    })
    void cleanDirectoryNameForWindows(String input, String expected) {
        assertEquals(expected, FileNameCleaner.cleanDirectoryName(input));
    }
}
