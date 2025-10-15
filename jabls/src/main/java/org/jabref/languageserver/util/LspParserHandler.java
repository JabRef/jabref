package org.jabref.languageserver.util;

import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.jabref.logic.JabRefException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.model.entry.BibEntry;

public class LspParserHandler {

    private final Map<String, ParserResult> parserResults;

    public LspParserHandler() {
        this.parserResults = new ConcurrentHashMap<>();
    }

    public ParserResult parserResultFromString(String fileUri, String content, ImportFormatPreferences importFormatPreferences) throws JabRefException, IOException {
        BibtexParser parser = new BibtexParser(importFormatPreferences);
        ParserResult parserResult = parser.parse(Reader.of(content));
        parserResults.put(fileUri, parserResult);
        return parserResult;
    }

    public Optional<ParserResult> getParserResultForUri(String fileUri) {
        return Optional.ofNullable(parserResults.get(fileUri));
    }

    public Map<String, List<BibEntry>> searchForEntryByCitationKey(String citationKey) {
        Map<String, List<BibEntry>> result = new ConcurrentHashMap<>();
        parserResults.forEach((fileUri, parserResult) -> {
            List<BibEntry> entries = parserResult.getDatabase().getEntriesByCitationKey(citationKey);
            if (!entries.isEmpty()) {
                result.put(fileUri, entries);
            }
        });
        return result;
    }

    public boolean citationKeyExists(String citationKey) {
        return parserResults.values().stream()
                .anyMatch(parserResult -> !parserResult.getDatabase().getEntriesByCitationKey(citationKey).isEmpty());
    }
}
