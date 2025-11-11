package org.jabref.languageserver.util;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.jabref.languageserver.util.definition.DefinitionProvider;
import org.jabref.languageserver.util.definition.DefinitionProviderFactory;

import org.eclipse.lsp4j.DocumentLink;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LspLinkHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(LspLinkHandler.class);

    private final LspParserHandler parserHandler;

    public LspLinkHandler(LspParserHandler parserHandler) {
        this.parserHandler = parserHandler;
    }

    public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> provideDefinition(String languageId, String uri, String content, Position position) {
        List<Location> locations = List.of();
        Optional<DefinitionProvider> provider = DefinitionProviderFactory.getDefinitionProvider(parserHandler, languageId);
        if (provider.isPresent()) {
            locations = provider.get().provideDefinition(content, position);
        }
        Either<List<? extends Location>, List<? extends LocationLink>> toReturn = Either.forLeft(locations);
        return CompletableFuture.completedFuture(toReturn);
    }

    public CompletableFuture<List<DocumentLink>> provideDocumentLinks(String languageId, String content) {
        List<DocumentLink> documentLinks = List.of();
        Optional<DefinitionProvider> provider = DefinitionProviderFactory.getDefinitionProvider(parserHandler, languageId);
        if (provider.isPresent()) {
            documentLinks = provider.get().provideDocumentLinks(content);
        }
        return CompletableFuture.completedFuture(documentLinks);
    }
}
