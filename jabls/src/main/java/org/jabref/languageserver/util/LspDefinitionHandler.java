package org.jabref.languageserver.util;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.jabref.languageserver.util.definition.MarkdownDefinitionProvider;

import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LspDefinitionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(LspDiagnosticHandler.class);

    private final LspParserHandler parserHandler;

    public LspDefinitionHandler(LspParserHandler parserHandler) {
        this.parserHandler = parserHandler;
    }

    public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> provideDefinition(String languageId, String uri, String content, Position position) {
        List<Location> locations = List.of();
        switch (languageId.toLowerCase()) {
            case "markdown" -> {
                locations = new MarkdownDefinitionProvider(parserHandler).provideDefinition(content, position);
            }
            default -> LOGGER.warn("Definition request for unsupported language: {}", languageId);
        }
        return CompletableFuture.completedFuture(Either.forLeft(locations));
    }
}
