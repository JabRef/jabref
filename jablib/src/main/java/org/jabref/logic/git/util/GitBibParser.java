package org.jabref.logic.git.util;

import java.io.StringReader;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.util.DummyFileUpdateMonitor;

public class GitBibParser {
    // TODO: exception handling
    public static BibDatabaseContext parseBibFromGit(String bibContent, ImportFormatPreferences importFormatPreferences) throws Exception {
        BibtexParser parser = new BibtexParser(importFormatPreferences, new DummyFileUpdateMonitor());
        ParserResult result = parser.parse(new StringReader(bibContent));
        return result.getDatabaseContext();
    }
}
