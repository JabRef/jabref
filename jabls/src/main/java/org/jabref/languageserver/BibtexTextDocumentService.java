package org.jabref.languageserver;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.jabref.languageserver.util.LspDiagnosticHandler;
import org.jabref.languageserver.util.LspLinkHandler;
import org.jabref.logic.remote.server.RemoteMessageHandler;

import com.google.gson.JsonArray;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.DocumentLink;
import org.eclipse.lsp4j.DocumentLinkParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BibtexTextDocumentService implements TextDocumentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BibtexTextDocumentService.class);

    private final LspClientHandler clientHandler;
    private final LspDiagnosticHandler diagnosticHandler;
    private final LspLinkHandler linkHandler;
    private final RemoteMessageHandler messageHandler;
    private final Map<String, String> fileUriToLanguageId;
    private final Map<String, String> contentCache;
    private LanguageClient client;

    public BibtexTextDocumentService(RemoteMessageHandler messageHandler, @NonNull LspClientHandler clientHandler, @NonNull LspDiagnosticHandler diagnosticHandler, @NonNull LspLinkHandler linkHandler) {
        this.clientHandler = clientHandler;
        this.diagnosticHandler = diagnosticHandler;
        this.linkHandler = linkHandler;
        this.fileUriToLanguageId = new ConcurrentHashMap<>();
        this.contentCache = new ConcurrentHashMap<>();
        this.messageHandler = messageHandler;
    }

    public void setClient(LanguageClient client) {
        this.client = client;
    }

    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
        LOGGER.debug("didOpen {}", params.getTextDocument().getUri());
        fileUriToLanguageId.putIfAbsent(params.getTextDocument().getUri(), params.getTextDocument().getLanguageId());
        if ("bibtex".equals(params.getTextDocument().getLanguageId())) {
            diagnosticHandler.computeAndPublishDiagnostics(client, params.getTextDocument().getUri(), params.getTextDocument().getText(), params.getTextDocument().getVersion());
        } else {
            contentCache.put(params.getTextDocument().getUri(), params.getTextDocument().getText());
        }
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params) {
        LOGGER.debug("didChange {}", params.getTextDocument().getUri());
        String languageId = fileUriToLanguageId.get(params.getTextDocument().getUri());
        if ("bibtex".equalsIgnoreCase(languageId)) {
            diagnosticHandler.computeAndPublishDiagnostics(client, params.getTextDocument().getUri(), params.getContentChanges().getFirst().getText(), params.getTextDocument().getVersion());
        } else {
            contentCache.put(params.getTextDocument().getUri(), params.getContentChanges().getFirst().getText());
        }
    }

    @Override
    public void didClose(DidCloseTextDocumentParams params) {
        fileUriToLanguageId.remove(params.getTextDocument().getUri());
        contentCache.remove(params.getTextDocument().getUri());
    }

    @Override
    public void didSave(DidSaveTextDocumentParams params) {
    }

    @Override
    public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> definition(DefinitionParams params) {
        if (!clientHandler.isStandalone()) {
            return CompletableFuture.completedFuture(Either.forLeft(List.of()));
        }
        if (fileUriToLanguageId.containsKey(params.getTextDocument().getUri())) {
            String fileUri = params.getTextDocument().getUri();
            return linkHandler.provideDefinition(fileUriToLanguageId.get(fileUri), fileUri, contentCache.get(fileUri), params.getPosition());
        }
        return CompletableFuture.completedFuture(Either.forLeft(List.of()));
    }

    @Override
    public CompletableFuture<List<DocumentLink>> documentLink(DocumentLinkParams params) {
        if (clientHandler.isStandalone()) {
            return CompletableFuture.completedFuture(List.of());
        }
        String fileUri = params.getTextDocument().getUri();
        return linkHandler.provideDocumentLinks(fileUriToLanguageId.get(fileUri), contentCache.get(fileUri));
    }

    @Override
    public CompletableFuture<DocumentLink> documentLinkResolve(DocumentLink params) {
        if (clientHandler.isStandalone()) {
            return CompletableFuture.completedFuture(null);
        }
        if (params.getData() instanceof JsonArray data) {
            messageHandler.handleCommandLineArguments(new String[] {data.asList().getFirst().getAsString(), data.asList().getLast().getAsString()});
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams position) {
        return TextDocumentService.super.completion(position);
    }
}
