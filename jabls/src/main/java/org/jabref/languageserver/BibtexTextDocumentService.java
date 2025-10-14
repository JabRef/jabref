package org.jabref.languageserver;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.jabref.languageserver.util.LspDiagnosticHandler;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.jspecify.annotations.NonNull;

public class BibtexTextDocumentService implements TextDocumentService {

    private final LspDiagnosticHandler diagnosticHandler;

    private LanguageClient client;

    public BibtexTextDocumentService(@NonNull LspDiagnosticHandler diagnosticHandler) {
        this.diagnosticHandler = diagnosticHandler;
    }

    public void setClient(LanguageClient client) {
        this.client = client;
    }

    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
        diagnosticHandler.computeAndPublishDiagnostics(client, params.getTextDocument().getUri(), params.getTextDocument().getText(), params.getTextDocument().getVersion());
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params) {
        diagnosticHandler.computeAndPublishDiagnostics(client, params.getTextDocument().getUri(), params.getContentChanges().getFirst().getText(), params.getTextDocument().getVersion());
    }

    @Override
    public void didClose(DidCloseTextDocumentParams params) {
    }

    @Override
    public void didSave(DidSaveTextDocumentParams params) {
    }

    @Override
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams position) {
        return TextDocumentService.super.completion(position);
    }
}
