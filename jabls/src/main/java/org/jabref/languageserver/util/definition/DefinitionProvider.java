package org.jabref.languageserver.util.definition;

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

    private static final Pattern CITATION_KEY_PATTERN = Pattern.compile("@(?<citationkey>[a-z0-9_.+:-]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern VALID_BEFORE_AT = Pattern.compile("[\\s\\[({;,:\\-—–«“\"'’]?");
    private static final Pattern CITATION_KEY_CHAR_PATTERN = Pattern.compile("[a-z0-9_\\-:.+]", Pattern.CASE_INSENSITIVE);

    protected final LspParserHandler parserHandler;

    public DefinitionProvider(LspParserHandler parserHandler) {
        this.parserHandler = parserHandler;
    }

    public List<Location> provideDefinition(String content, Position position) {
        Optional<String> citationKey = getCitationKeyAtPosition(content, position);
        if (citationKey.isPresent()) {
            Map<String, List<BibEntry>> entriesMap = parserHandler.searchForEntryByCitationKey(citationKey.get());
            if (!entriesMap.isEmpty()) {
                return entriesMap.entrySet().stream()
                                 .flatMap(listEntry -> listEntry.getValue().stream().map(entry -> new Location(listEntry.getKey(), getRangeFromEntry(listEntry.getKey(), entry))))
                                 .toList();
            }
        }
        return List.of();
    }

    public List<DocumentLink> provideDocumentLinks(String content) {
        Matcher matcher = CITATION_KEY_PATTERN.matcher(content);
        return matcher.results()
                      .map(matchResult -> {
                          String citationKey = matchResult.group("citationkey");
                          Range range = LspRangeUtil.convertToLspRange(content, matchResult.start(), matchResult.end());
                          DocumentLink documentLink = new DocumentLink();
                          documentLink.setRange(range);
                          JsonArray data = new JsonArray();
                          data.add("--jumpToKey");
                          data.add(citationKey);
                          documentLink.setData(data);
                          return documentLink;
                      })
                      .toList();
    }

    Range getRangeFromEntry(String fileUri, BibEntry entry) {
        ParserResult parserResult = parserHandler.getParserResultForUri(fileUri).get(); // always present if we have an entry from provideDefinition
        return LspRangeUtil.convertToLspRange(parserResult.getArticleRanges().get(entry));
    }

    Optional<String> getCitationKeyAtPosition(String content, Position position) {
        if (content == null || content.isEmpty() || position == null) {
            return Optional.empty();
        }

        int around = LspRangeUtil.toOffset(content, position);
        if (around < 0 || around > content.length()) {
            return Optional.empty();
        }

        int start = lookaheadForCitationKeyStart(content, position);
        if (start < 0) {
            return Optional.empty();
        }

        int end = lookaheadForCitationKeyEnd(content, position);
        if (end < 0 || end <= start) {
            return Optional.empty();
        }

        return Optional.of(content.substring(start, end));
    }

    private int lookaheadForCitationKeyEnd(String content, Position position) {
        int i = lookaheadForCitationKeyStart(content, position);
        while (i < content.length() && isCitationKeyCharacter(content.charAt(i))) {
            i++;
        }
        return i;
    }

    private int lookaheadForCitationKeyStart(String content, Position position) {
        int caretOffset = clamp(LspRangeUtil.toOffset(content, position), 0, content.length());
        int scanIndex = Math.min(Math.max(0, caretOffset - 1), Math.max(0, content.length() - 1));
        while (scanIndex >= 0 && isCitationKeyCharacter(content.charAt(scanIndex))) {
            scanIndex--;
        }

        if (scanIndex >= 0 && content.charAt(scanIndex) == '@') {
            if (isValidCitationKeyCharBefore(content, scanIndex - 1)) {
                return scanIndex + 1;
            }
            return -1;
        }
        if (caretOffset < content.length() && content.charAt(caretOffset) == '@' && isValidCitationKeyCharBefore(content, caretOffset - 1)) {
            return caretOffset + 1;
        }
        return -1;
    }

    private int clamp(int i, int min, int max) {
        return Math.max(min, Math.min(i, max));
    }

    private boolean isCitationKeyCharacter(char c) {
        return CITATION_KEY_CHAR_PATTERN.matcher(String.valueOf(c)).matches();
    }

    private boolean isValidCitationKeyCharBefore(String content, int idBeforeAt) {
        if (idBeforeAt < 0) {
            return true;
        }
        String before = content.substring(idBeforeAt, idBeforeAt + 1);
        return VALID_BEFORE_AT.matcher(before).matches();
    }
}
