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
            "'The Evolution of Sentiment_ Analysis_A Review of Research Topics, Venues, and Top Cited Papers.PDF', 'The Evolution of Sentiment} Analysis}A Review of Research Topics, Venues, and Top Cited Papers.PDF'",
            "file_with_colon.txt, file:with:colon.txt",
            "file_with_quote.txt, file\"with\"quote.txt",
            "file_with_backslash.txt, file\\with\\backslash.txt"
    })
    void cleanFileName(String expected, String input) {
        assertEquals(expected, FileNameCleaner.cleanFileName(input));
    }

    @ParameterizedTest
    @CsvSource({
            "legalFilename.txt, legalFilename.txt",
            "subdir/legalFilename.txt, subdir/legalFilename.txt",
            "illegalFilename/_____.txt, illegalFilename/?*<>|.txt",
            "draft., draft.",
            "a/archive.../b, a/archive.../b"
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

    @ParameterizedTest
    @CsvSource({
            // "." and ".." segments dropped
            "Desktop, ../../../../Desktop",
            "Desktop, ..\\..\\..\\..\\Desktop",
            "a/b, a/../b",
            "a\\b, a\\..\\b",
            // Windows resolves ".. " and ".. ." to ".."
            "Desktop, '..  /Desktop'",
            "a/b, 'a/.. ./b'",
            // leading separator dropped, else resolve() ignores base directory
            "foo/bar, /foo/bar",
            "outside, /outside",
            "outside, ' /outside'",
            // only whole segments are dropped
            "..Desktop, ../..Desktop",
            "draft./2026, draft./2026"
    })
    void cleanDirectoryNameRemovesTraversal(String expected, String input) {
        assertEquals(expected, FileNameCleaner.cleanDirectoryName(input));
    }
}
