package org.jabref.logic.util.io;

import java.util.Arrays;

/**
 * This class is based on http://stackoverflow.com/a/5626340/873282
 * extended with LEFT CURLY BRACE and RIGHT CURLY BRACE
 * Replaces illegal characters in given file paths.
 */
public class FileNameCleaner {

    /**
     * MUST ALWAYS BE A SORTED ARRAY because it is used in a binary search
     */
    // @formatter:off
    private static final int[] ILLEGAL_CHARS = {
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
            10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
            20, 21, 22, 23, 24, 25, 26, 27, 28, 29,
            30, 31, 34,
            42,
            58,
            60, 62, 63,
            123, 124, 125
    };
    // @formatter:on

    private FileNameCleaner() {
    }

    /**
     * Replaces illegal characters in given fileName by '_'
     *
     * @param badFileName the fileName to clean
     * @return a clean filename
     */
    public static String cleanFileName(String badFileName) {
        StringBuilder cleanName = new StringBuilder(badFileName.length());
        for (int i = 0; i < badFileName.length(); i++) {
            char c = badFileName.charAt(i);
            if (FileNameCleaner.isCharLegal(c) && (c != '/') && (c != '\\')) {
                cleanName.append(c);
            } else {
                cleanName.append('_');
            }
        }
        return cleanName.toString().trim();
    }

    /**
     * Replaces illegal characters in given directoryName by '_'.
     * Directory name may contain directory separators, e.g. 'deep/in/a/tree'; these are left untouched.
     *
     * @param badFileName the fileName to clean
     * @return a clean filename
     */
    public static String cleanDirectoryName(String badFileName) {
        StringBuilder cleanName = new StringBuilder(badFileName.length());
        for (int i = 0; i < badFileName.length(); i++) {
            char c = badFileName.charAt(i);
            if (FileNameCleaner.isCharLegal(c)) {
                cleanName.append(c);
            } else {
                cleanName.append('_');
            }
        }
        return cleanName.toString().trim();
    }

    private static boolean isCharLegal(char c) {
        return Arrays.binarySearch(FileNameCleaner.ILLEGAL_CHARS, c) < 0;
    }
}
