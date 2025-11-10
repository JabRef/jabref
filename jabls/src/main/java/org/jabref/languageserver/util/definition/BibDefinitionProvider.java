package org.jabref.languageserver.util.definition;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jabref.languageserver.util.LspParserHandler;
import org.jabref.languageserver.util.LspRangeUtil;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.util.FileFieldParser;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;

import org.eclipse.lsp4j.DocumentLink;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BibDefinitionProvider extends DefinitionProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(BibDefinitionProvider.class);
    private static final Range EMPTY_RANGE = new Range(new Position(0, 0), new Position(0, 0));

    private final CliPreferences preferences;

    public BibDefinitionProvider(CliPreferences preferences, LspParserHandler parserHandler) {
        super(parserHandler);
        this.preferences = preferences;
    }

    @Override
    public List<Location> provideDefinition(String uri, String content, Position position) {
        Optional<ParserResult> parserResult = parserHandler.getParserResultForUri(uri);

        if (parserResult.isEmpty()) {
            return List.of();
        }

        for (Map.Entry<BibEntry, ParserResult.Range> entry : parserResult.get().getArticleRanges().entrySet()) {
            BibEntry bibEntry = entry.getKey();
            ParserResult.Range range = entry.getValue();
            if (bibEntry.getField(StandardField.FILE).isPresent() && LspRangeUtil.isPositionInRange(position, LspRangeUtil.convertToLspRange(range))) {
                Range fileFieldRange = LspRangeUtil.convertToLspRange(parserResult.get().getFieldRange(bibEntry, StandardField.FILE));
                if (!LspRangeUtil.isPositionInRange(position, fileFieldRange)) {
                    return List.of();
                }
                Optional<String> fileString = bibEntry.getFieldOrAlias(StandardField.FILE);
                if (fileString.isEmpty()) {
                    return List.of();
                }

                int offsetStart = LspRangeUtil.toOffset(content, fileFieldRange.getStart());
                int offsetEnd = LspRangeUtil.toOffset(content, fileFieldRange.getEnd());
                String fileField = content.substring(offsetStart, offsetEnd);
                int startIndex = offsetStart + fileField.indexOf(fileString.get());

                Map<LinkedFile, org.jabref.model.util.Range> fileRangeMap = FileFieldParser.parseToPosition(fileString.get());
                for (Map.Entry<LinkedFile, org.jabref.model.util.Range> linkedFileRangeEntry : fileRangeMap.entrySet()) {
                    LinkedFile linkedFile = linkedFileRangeEntry.getKey();
                    org.jabref.model.util.Range rangeInFileString = linkedFileRangeEntry.getValue();
                    int start = startIndex + rangeInFileString.start();
                    int end = start + rangeInFileString.end();
                    Range linkRange = LspRangeUtil.convertToLspRange(content, start, end);
                    if (LspRangeUtil.isPositionInRange(position, linkRange)) {
                        try {
                            Optional<Path> filePath = FileUtil.find(parserResult.get().getDatabaseContext(), linkedFile.getLink(), preferences.getFilePreferences());
                            return List.of(new Location(filePath.get().toUri().toString(), EMPTY_RANGE));
                        } catch (NullPointerException e) {
                            LOGGER.error("Error while getting file path", e);
                        }
                    }
                }
            }
        }
        return List.of();
    }

    @Override
    public List<DocumentLink> provideDocumentLinks(String fileUri, String content) {
        return List.of();
    }
}
