package org.jabref.logic.git.io;

import java.io.IOException;
import java.io.Reader;

import org.jabref.logic.JabRefException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.util.DummyFileUpdateMonitor;

public class GitBibParser {
    public static BibDatabaseContext parseBibFromGit(String bibContent, ImportFormatPreferences importFormatPreferences) throws JabRefException {
        BibtexParser parser = new BibtexParser(importFormatPreferences, new DummyFileUpdateMonitor());
        ParserResult result;
        try {
            Reader reader = Reader.of(bibContent);
            result = parser.parse(reader);
            return result.getDatabaseContext();
        } catch (IOException e) {
            throw new JabRefException("Failed to parse BibTeX content from Git", e);
        }
    }
}
