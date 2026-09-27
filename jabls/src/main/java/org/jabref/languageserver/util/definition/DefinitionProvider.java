package org.jabref.languageserver.util.definition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jabref.languageserver.util.LspParserHandler;
import org.jabref.languageserver.util.LspRangeUtil;
import org.jabref.logic.importer.ParserResult;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import com.google.gson.JsonArray;
import org.eclipse.lsp4j.DocumentLink;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

public abstract class DefinitionProvider {

    protected final LspParserHandler parserHandler;

    protected Pattern citationCommandPattern;
    protected Pattern citationKeyInsidePattern;

    public DefinitionProvider(LspParserHandler parserHandler) {
        this.parserHandler = parserHandler;
    }

    public List<Location> provideDefinition(String uri, String content, Position position) {
        Optional<String> citationKey = getCitationKeyAtPosition(content, position);
        if (citationKey.isPresent()) {
            Map<String, List<BibEntry>> entriesMap = parserHandler.searchForEntryByCitationKey(citationKey.get());
            if (!entriesMap.isEmpty()) {
                return entriesMap.entrySet().stream()
                                 .flatMap(listEntry -> listEntry.getValue().stream()
                                                                .map(entry -> createLocation(getRangeFromEntry(listEntry.getKey(), entry), listEntry.getKey())))
                                 .toList();
            }
        }
        return List.of();
    }

    // [impl->req~jabls.hover.citation-key~1]
    public Optional<Hover> provideHover(String content, Position position) {
        return getCitationKeyAtPosition(content, position).flatMap(citationKey -> {
            String entries = parserHandler.searchForEntryByCitationKey(citationKey).values().stream()
                                          .flatMap(List::stream)
                                          .map(entry -> formatForHover(citationKey, entry))
                                          .collect(Collectors.joining("\n\n---\n\n"));
            if (entries.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new Hover(new MarkupContent(MarkupKind.MARKDOWN, entries)));
        });
    }

    private static String formatForHover(String citationKey, BibEntry entry) {
        StringBuilder result = new StringBuilder("**").append(citationKey).append("**");
        entry.getFieldOrAliasLatexFree(StandardField.AUTHOR).ifPresent(author -> result.append("\n\n").append(author));
        entry.getFieldOrAliasLatexFree(StandardField.TITLE).ifPresent(title -> result.append("\n\n*").append(title).append('*'));
        entry.getFieldOrAliasLatexFree(StandardField.YEAR).ifPresent(year -> result.append(" (").append(year).append(')'));
        return result.toString();
    }

    public List<DocumentLink> provideDocumentLinks(String fileUri, String content) {
        if (content == null || content.isEmpty()) {
            return List.of();
        }
        if (citationCommandPattern == null || citationKeyInsidePattern == null) {
            return List.of();
        }

        List<DocumentLink> links = new ArrayList<>();
        for (KeyBounds kb : findCitationKeys(content)) {
            Range range = LspRangeUtil.convertToLspRange(content, kb.start(), kb.end());
            links.add(createDocumentLink(range, kb.key()));
        }
        return links;
    }

    protected DocumentLink createDocumentLink(Range range, String key) {
        DocumentLink link = new DocumentLink();
        link.setRange(range);
        JsonArray data = new JsonArray();
        data.add("--jumpToKey");
        data.add(key);
        link.setData(data);
        return link;
    }

    protected Location createLocation(Range range, String uri) {
        return new Location(uri, range);
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

    private record KeyBounds(String key, int start, int end) {
    }
}
