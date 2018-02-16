package org.jabref.pdfimport;

import java.io.File;
import java.io.FileFilter;

public class PdfFileFilter implements FileFilter {

    @Override
    public boolean accept(File file) {
        String path = file.getPath();

        return isMatchingFileFilter(path);
    }

    public static boolean accept(String path) {
        if ((path == null) || path.isEmpty() || !path.contains(".")) {
            return false;
        }

        return isMatchingFileFilter(path);
    }

    private static boolean isMatchingFileFilter(String path) {
        String extension = path.substring(path.lastIndexOf('.') + 1);
        return "pdf".equalsIgnoreCase(extension);
    }

}
