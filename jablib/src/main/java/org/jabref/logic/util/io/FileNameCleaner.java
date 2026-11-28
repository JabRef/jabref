package org.jabref.logic.util.io;

import java.util.regex.Pattern;

/// This class is based on http://stackoverflow.com/a/5626340/873282
/// extended with LEFT CURLY BRACE and RIGHT CURLY BRACE
/// Replaces illegal characters in given file paths.
///
/// Regarding the maximum length, see [FileUtil#getValidFileName(String)]
public class FileNameCleaner {

    /// Matches "." / ".." segments, also Windows forms like ".. " and ".. .". Keeps "..." (valid on Unix).
    private static final Pattern DOT_SEGMENT = Pattern.compile("(?<![^/\\\\])\\.{1,2}+(?: ++[ .]*+)?+(?:[/\\\\]++|$)");

    /// Matches separators at the start of the path (e.g. leading '/' or '\\').
    private static final Pattern LEADING_SEPARATORS = Pattern.compile("^[/\\\\]+");

    private FileNameCleaner() {
    }

    /// Replaces illegal characters in given fileName by '_'
    ///
    /// @param badFileName the fileName to clean
    /// @return a clean filename
    public static String cleanFileName(String badFileName) {
        StringBuilder cleanName = new StringBuilder(badFileName.length());
        for (int i = 0; i < badFileName.length(); i++) {
            char c = badFileName.charAt(i);
            if (FileUtil.isCharLegal(c) && (c != '/') && (c != '\\')) {
                cleanName.append(c);
            } else {
                cleanName.append('_');
            }
        }
        return cleanName.toString().trim();
    }

    /// Replaces illegal characters in given directoryName by '_'.
    /// Directory name may contain directory separators, e.g. 'deep/in/a/tree'; these are left untouched.
    ///
    /// @param badFileName the fileName to clean
    /// @return a clean filename
    public static String cleanDirectoryName(String badFileName) {
        StringBuilder cleanName = new StringBuilder(badFileName.length());
        for (int i = 0; i < badFileName.length(); i++) {
            char c = badFileName.charAt(i);
            if (FileUtil.isCharLegal(c)) {
                cleanName.append(c);
            } else {
                cleanName.append('_');
            }
        }
        // Drop "." / ".." segments and leading separators: result stays inside base directory
        String result = cleanName.toString().trim();
        result = DOT_SEGMENT.matcher(result).replaceAll("");
        return LEADING_SEPARATORS.matcher(result).replaceAll("");
    }
}
