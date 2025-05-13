package org.jabref.logic.util;

import org.jabref.logic.util.io.FileNameCleaner;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FileNameCleanerTest {

    @ParameterizedTest
    @CsvSource({
        "legalFilename.txt, legalFilename.txt",
        "illegalFilename______.txt, illegalFilename/?*<>|.txt",
        "illegalFileName_.txt, illegalFileName{.txt",
        "_The Evolution of Sentiment_ Analysis_.PDF, ?The Evolution of Sentiment} Analysis}.PDF",
        "'The Evolution of Sentiment_ Analysis_A Review of Research Topics, Venues, and Top Cited Papers.PDF', 'The Evolution of Sentiment} Analysis}A Review of Research Topics, Venues, and Top Cited Papers.PDF'"
    })
    void cleanFileName(String expected, String input) {
        assertEquals(expected, FileNameCleaner.cleanFileName(input));
    }

    @ParameterizedTest
    @CsvSource({
        "legalFilename.txt, legalFilename.txt",
        "subdir/legalFilename.txt, subdir/legalFilename.txt",
        "illegalFilename/_____.txt, illegalFilename/?*<>|.txt"
    })
    void cleanDirectoryName(String expected, String input) {
        assertEquals(expected, FileNameCleaner.cleanDirectoryName(input));
    }

    @ParameterizedTest
    @CsvSource({
        "legalFilename.txt, legalFilename.txt",
        "subdir\\legalFilename.txt, subdir\\legalFilename.txt",
        "illegalFilename\\_____.txt, illegalFilename\\?*<>|.txt"
    })
    void cleanDirectoryNameForWindows(String expected, String input) {
        assertEquals(expected, FileNameCleaner.cleanDirectoryName(input));
    }
}
