package org.jabref.languageserver.util;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.jabref.languageserver.LspClientHandler;
import org.jabref.languageserver.util.definition.DefinitionProvider;
import org.jabref.languageserver.util.definition.DefinitionProviderFactory;
import org.jabref.logic.preferences.CliPreferences;

import org.eclipse.lsp4j.DocumentLink;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LspLinkHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(LspLinkHandler.class);

    private final LspClientHandler clientHandler;
    private final LspParserHandler parserHandler;
    private final CliPreferences preferences;

    public LspLinkHandler(LspClientHandler clientHandler, LspParserHandler parserHandler, CliPreferences preferences) {
        this.clientHandler = clientHandler;
        this.parserHandler = parserHandler;
        this.preferences = preferences;
    }

    public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> provideDefinition(String languageId, String uri, String content, Position position) {
        if (!clientHandler.isStandalone() && !"bibtex".equals(languageId)) {
            return CompletableFuture.completedFuture(Either.forLeft(List.of()));
        }

        List<Location> locations = List.of();
        Optional<DefinitionProvider> provider = DefinitionProviderFactory.getDefinitionProvider(preferences, parserHandler, languageId);
        if (provider.isPresent()) {
            locations = provider.get().provideDefinition(uri, content, position);
        }
        Either<List<? extends Location>, List<? extends LocationLink>> toReturn = Either.forLeft(locations);
        return CompletableFuture.completedFuture(toReturn);
    }

    public CompletableFuture<List<DocumentLink>> provideDocumentLinks(String fileUri, String languageId, String content) {
        if (clientHandler.isStandalone()) {
            return CompletableFuture.completedFuture(List.of());
        }
        List<DocumentLink> documentLinks = List.of();
        Optional<DefinitionProvider> provider = DefinitionProviderFactory.getDefinitionProvider(preferences, parserHandler, languageId);
        if (provider.isPresent()) {
            documentLinks = provider.get().provideDocumentLinks(fileUri, content);
        }
        return CompletableFuture.completedFuture(documentLinks);
    }
}
