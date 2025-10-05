package org.jabref.languageserver.util.definition;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jabref.languageserver.util.LspParserHandler;
import org.jabref.languageserver.util.LspRangeUtil;
import org.jabref.logic.importer.ParserResult;
import org.jabref.model.entry.BibEntry;

import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

public class MarkdownDefinitionProvider implements DefinitionProvider {

    private final LspParserHandler parserHandler;

    public MarkdownDefinitionProvider(LspParserHandler parserHandler) {
        this.parserHandler = parserHandler;
    }

    @Override
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

    private Range getRangeFromEntry(String fileUri, BibEntry entry) {
        ParserResult parserResult = parserHandler.getParserResultForUri(fileUri).get(); // always present if we have an entry from provideDefinition
        return LspRangeUtil.convertToLspRange(parserResult.getArticleRanges().get(entry));
    }

    private Optional<String> getCitationKeyAtPosition(String content, Position position) {
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
        return (c >= 'A' && c <= 'Z')
                || (c >= 'a' && c <= 'z')
                || (c >= '0' && c <= '9')
                || c == '_' || c == '-' || c == ':' || c == '.' || c == '+';
    }

    private boolean isValidCitationKeyCharBefore(String content, int idBeforeAt) {
        if (idBeforeAt < 0) {
            return true;
        }
        char p = content.charAt(idBeforeAt);

        if (Character.isLetterOrDigit(p) || p == '_' || p == '.') {
            return false;
        }

        return Character.isWhitespace(p)
                || p == '[' || p == '(' || p == '{'
                || p == ';' || p == ',' || p == ':'
                || p == '-' || p == '—' || p == '–' || p == '«' || p == '“' || p == '"'
                || p == '\'';
    }
}
