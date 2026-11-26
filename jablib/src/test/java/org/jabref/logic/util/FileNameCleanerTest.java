package org.jabref.logic.util;

import org.jabref.logic.util.io.FileNameCleaner;

import org.junit.jupiter.api.Test;
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

    @Test
    void cleanDirectoryNameRemovesEscaping() {
        assertEquals("Desktop", FileNameCleaner.cleanDirectoryName("../../../../Desktop"));
        assertEquals("Desktop", FileNameCleaner.cleanDirectoryName("..\\..\\..\\..\\Desktop"));
    }

    @Test
    void cleanDirectoryNameRemovesWindowsTrailingSpaceTraversal() {
        // Windows resolves ".. " and ".. ." to ".."
        assertEquals("Desktop", FileNameCleaner.cleanDirectoryName("..  /Desktop"));
        assertEquals("a/b", FileNameCleaner.cleanDirectoryName("a/.. ./b"));
    }

    @Test
    void cleanDirectoryNameKeepsSeparators() {
        // Patterns like [author]/[year]/..
        assertEquals("foo/bar/test", FileNameCleaner.cleanDirectoryName("foo/bar/test"));
        // Windows uses '\'
        assertEquals("foo\\bar\\test", FileNameCleaner.cleanDirectoryName("foo\\bar\\test"));
    }

    @Test
    void cleanDirectoryNameDropsLeadingRoot() {
        assertEquals("foo/bar", FileNameCleaner.cleanDirectoryName("/foo/bar"));
    }

    @ParameterizedTest
    @CsvSource({
            "Desktop, ../../../../Desktop",
            "a/b, a/../b",
            "a\\b, a\\..\\b",
            "outside, /outside",
            "..Desktop, ../..Desktop",
            "draft./2026, draft./2026"
    })
    void cleanDirectoryNameRemovesTraversal(String expected, String input) {
        assertEquals(expected, FileNameCleaner.cleanDirectoryName(input));
    }
}
