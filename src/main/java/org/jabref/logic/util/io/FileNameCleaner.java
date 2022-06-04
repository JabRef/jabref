package org.jabref.logic.util.io;

import org.jabref.model.util.FileHelper;

/**
 * This class is based on http://stackoverflow.com/a/5626340/873282
 * extended with LEFT CURLY BRACE and RIGHT CURLY BRACE
 * Replaces illegal characters in given file paths.
 */
public class FileNameCleaner {

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
            if (FileHelper.isCharLegal(c) && (c != '/') && (c != '\\')) {
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
            if (FileHelper.isCharLegal(c)) {
                cleanName.append(c);
            } else {
                cleanName.append('_');
            }
        }
        return cleanName.toString().trim();
    }
}
