package org.jabref.languageserver.util.definition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.languageserver.util.LspParserHandler;
import org.jabref.languageserver.util.LspRangeUtil;
import org.jabref.logic.importer.ParserResult;
import org.jabref.model.entry.BibEntry;

import com.google.gson.JsonArray;
import org.eclipse.lsp4j.DocumentLink;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

public abstract class DefinitionProvider {

    protected final LspParserHandler parserHandler;

    protected Pattern citationCommandPattern;
    protected Pattern citationKeyInsidePattern;

    public DefinitionProvider(LspParserHandler parserHandler) {
        this.parserHandler = parserHandler;
    }

    public List<Location> provideDefinition(String content, Position position) {
        Optional<String> citationKey = getCitationKeyAtPosition(content, position);
        if (citationKey.isPresent()) {
            Map<String, List<BibEntry>> entriesMap = parserHandler.searchForEntryByCitationKey(citationKey.get());
            if (!entriesMap.isEmpty()) {
                return entriesMap.entrySet().stream()
                                 .flatMap(listEntry -> listEntry.getValue().stream()
                                                                .map(entry -> new Location(listEntry.getKey(), getRangeFromEntry(listEntry.getKey(), entry))))
                                 .toList();
            }
        }
        return List.of();
    }

    public List<DocumentLink> provideDocumentLinks(String content) {
        if (content == null || content.isEmpty()) {
            return List.of();
        }
        if (citationCommandPattern == null || citationKeyInsidePattern == null) {
            return List.of();
        }

        List<DocumentLink> links = new ArrayList<>();
        for (KeyBounds kb : findCitationKeys(content)) {
            Range range = LspRangeUtil.convertToLspRange(content, kb.start(), kb.end());

            DocumentLink link = new DocumentLink();
            link.setRange(range);
            JsonArray data = new JsonArray();
            data.add("--jumpToKey");
            data.add(kb.key());
            link.setData(data);
            links.add(link);
        }
        return links;
    }

    Range getRangeFromEntry(String fileUri, BibEntry entry) {
        ParserResult parserResult = parserHandler.getParserResultForUri(fileUri).get(); // always present if we have an entry from provideDefinition
        return LspRangeUtil.convertToLspRange(parserResult.getArticleRanges().get(entry));
    }

    Optional<String> getCitationKeyAtPosition(String content, Position position) {
        if (content == null || position == null) {
            return Optional.empty();
        }
        if (citationCommandPattern == null || citationKeyInsidePattern == null) {
            return Optional.empty();
        }

        int caret = LspRangeUtil.toOffset(content, position);
        if (caret < 0 || caret > content.length()) {
            return Optional.empty();
        }

        for (KeyBounds kb : findCitationKeys(content)) {
            if (caret >= kb.start() && caret <= kb.end()) {
                return Optional.of(kb.key());
            }
        }
        return Optional.empty();
    }

    protected static Optional<String> safeGroup(Matcher matcher, String group) {
        return matcher.group(group).describeConstable();
    }

    private List<KeyBounds> findCitationKeys(String content) {
        if (content == null || content.isEmpty()) {
            return List.of();
        }
        if (citationCommandPattern == null || citationKeyInsidePattern == null) {
            return List.of();
        }

        List<KeyBounds> result = new ArrayList<>();
        Matcher cmdMatcher = citationCommandPattern.matcher(content);
        while (cmdMatcher.find()) {
            Optional<String> keysString = safeGroup(cmdMatcher, "keys");
            if (keysString.isEmpty() || keysString.get().isBlank()) {
                continue;
            }
            int keysStartInContent = cmdMatcher.start("keys");

            Matcher keyMatcher = citationKeyInsidePattern.matcher(keysString.get());
            while (keyMatcher.find()) {
                Optional<String> key = safeGroup(keyMatcher, "citationkey");
                if (key.isEmpty() || key.get().isBlank()) {
                    continue;
                }
                int start = keysStartInContent + keyMatcher.start("citationkey");
                int end = keysStartInContent + keyMatcher.end("citationkey");
                result.add(new KeyBounds(key.get(), start, end));
            }
        }
        return result;
    }

    private record KeyBounds(String key, int start, int end) { }
}
