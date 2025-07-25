package org.jabref.logic.git.io;

import java.io.IOException;
import java.io.Reader;
import java.util.Optional;

import org.jabref.logic.JabRefException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.metadata.MetaData;
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

    public static BibDatabaseContext parseBibFromGit(Optional<String> maybeContent, ImportFormatPreferences prefs) throws JabRefException {
        if (maybeContent.isEmpty()) {
            BibDatabase emptyDatabase = new BibDatabase();
            MetaData emptyMetaData = new MetaData();
            return new BibDatabaseContext(emptyDatabase, emptyMetaData, null);
        }
        return parseBibFromGit(maybeContent.get(), prefs);
    }
}
