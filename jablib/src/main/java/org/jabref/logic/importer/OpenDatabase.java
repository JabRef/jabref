package org.jabref.logic.importer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;

import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.model.util.FileUpdateMonitor;

public class OpenDatabase {

    private OpenDatabase() {
    }

    /**
     * Load database (bib-file)
     *
     * @param fileToOpen Name of the BIB-file to open
     * @return ParserResult which never is null
     */
    public static ParserResult loadDatabase(Path fileToOpen, ImportFormatPreferences importFormatPreferences, FileUpdateMonitor fileMonitor)
            throws IOException {
        return new BibtexImporter(importFormatPreferences, fileMonitor).importDatabase(fileToOpen);
    }

    public static ParserResult loadDatabase(InputStream inputStream, ImportFormatPreferences importFormatPreferences, FileUpdateMonitor fileUpdateMonitor)
            throws IOException {
        return new BibtexImporter(importFormatPreferences, fileUpdateMonitor).importDatabase(inputStream, new BibtexImporter.EncodingResult(Charset.defaultCharset(), true));
    }
}
