package org.jabref.languageserver;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.jabref.languageserver.util.LspDefinitionHandler;
import org.jabref.languageserver.util.LspDiagnosticHandler;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.jspecify.annotations.NonNull;

public class BibtexTextDocumentService implements TextDocumentService {

    private final LspDiagnosticHandler diagnosticHandler;
    private final LspDefinitionHandler definitionHandler;
    private final Map<String, String> fileUriToLanguageId;
    private final Map<String, String> contentCache;
    private LanguageClient client;

    public BibtexTextDocumentService(@NonNull LspDiagnosticHandler diagnosticHandler, @NonNull LspDefinitionHandler definitionHandler) {
        this.diagnosticHandler = diagnosticHandler;
        this.definitionHandler = definitionHandler;
        this.fileUriToLanguageId = new ConcurrentHashMap<>();
        this.contentCache = new ConcurrentHashMap<>();
    }

    public void setClient(LanguageClient client) {
        this.client = client;
    }

    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
        fileUriToLanguageId.putIfAbsent(params.getTextDocument().getUri(), params.getTextDocument().getLanguageId());
        if ("bibtex".equals(params.getTextDocument().getLanguageId())) {
            diagnosticHandler.computeAndPublishDiagnostics(client, params.getTextDocument().getUri(), params.getTextDocument().getText(), params.getTextDocument().getVersion());
        } else {
            contentCache.put(params.getTextDocument().getUri(), params.getTextDocument().getText());
        }
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params) {
        String languageId = fileUriToLanguageId.get(params.getTextDocument().getUri());
        if ("bibtex".equals(languageId)) {
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
        if (fileUriToLanguageId.containsKey(params.getTextDocument().getUri())) {
            String fileUri = params.getTextDocument().getUri();
            return definitionHandler.provideDefinition(fileUriToLanguageId.get(fileUri), fileUri, contentCache.get(fileUri), params.getPosition());
        }
        return CompletableFuture.completedFuture(Either.forLeft(List.of()));
    }

    @Override
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams position) {
        return TextDocumentService.super.completion(position);
    }
}
