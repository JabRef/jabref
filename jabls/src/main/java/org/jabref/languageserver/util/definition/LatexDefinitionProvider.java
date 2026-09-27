package org.jabref.languageserver.util.definition;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.languageserver.util.LspParserHandler;
import org.jabref.languageserver.util.LspRangeUtil;
import org.jabref.logic.texparser.DefaultLatexParser;
import org.jabref.logic.texparser.LatexParser;

import org.eclipse.lsp4j.DocumentLink;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

public class LatexDefinitionProvider extends DefinitionProvider {

    private LatexParser latexParser;

    public LatexDefinitionProvider(LspParserHandler parserHandler) {
        super(parserHandler);
        latexParser = new DefaultLatexParser();
    }

    @Override
    public List<Location> provideDefinition(String uri, String content, Position position) {
        List<Location> locations = new ArrayList<>();
        latexParser.parse(content).getCitations().forEach((key, citation) -> {
            Range range = LspRangeUtil.convertToLspRange(citation.line(), citation.colStart(), citation.colEnd());
            if (LspRangeUtil.isPositionInRange(position, range)) {
                parserHandler.searchForEntryByCitationKey(key).forEach((databaseUri, entries) -> entries.forEach(entry -> {
                    locations.add(createLocation(getRangeFromEntry(databaseUri, entry), databaseUri));
                }));
            }
        });
        return locations;
    }

    @Override
    Optional<String> getCitationKeyAtPosition(String content, Position position) {
        return latexParser.parse(content).getCitations().entries().stream()
                          .filter(entry -> LspRangeUtil.isPositionInRange(position, LspRangeUtil.convertToLspRange(entry.getValue().line(), entry.getValue().colStart(), entry.getValue().colEnd())))
                          .map(entry -> entry.getKey().strip())
                          .findFirst();
    }

    @Override
    public List<DocumentLink> provideDocumentLinks(String fileUri, String content) {
        List<DocumentLink> locations = new ArrayList<>();
        latexParser.parse(content).getCitations().forEach((key, citation) -> {
            locations.add(createDocumentLink(LspRangeUtil.convertToLspRange(citation.line(), citation.colStart(), citation.colEnd()), key));
        });
        return locations;
    }
}
