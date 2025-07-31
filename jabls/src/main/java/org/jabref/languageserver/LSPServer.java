package org.jabref.languageserver;

import java.util.concurrent.CompletableFuture;

import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.preferences.CliPreferences;

import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.TextDocumentSyncOptions;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LSPServer implements LanguageServer, LanguageClientAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(LSPServer.class);

    private LanguageClient client;
    private BibtexWorkspaceService workspaceService;
    private BibtexTextDocumentService textDocumentService;

    public LSPServer(CliPreferences cliPreferences, JournalAbbreviationRepository abbreviationRepository) {
        this.workspaceService = new BibtexWorkspaceService();
        this.textDocumentService = new BibtexTextDocumentService(cliPreferences, abbreviationRepository);
    }

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
        ServerCapabilities capabilities = new ServerCapabilities();

        TextDocumentSyncOptions syncOptions = new TextDocumentSyncOptions();
        syncOptions.setSave(true);
        syncOptions.setChange(TextDocumentSyncKind.Full);
        syncOptions.setOpenClose(true);

        capabilities.setTextDocumentSync(syncOptions);

        return CompletableFuture.completedFuture(new InitializeResult(capabilities));
    }

    @Override
    public CompletableFuture<Object> shutdown() {
        return CompletableFuture.completedFuture(null);
    }

    /// currently not implemented because it comes from the LanguageServer interface and is documented like here
    /// https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/#exit
    /// we have to decide how to implement this so jabls gets stopped only when started before by a lsp client
    @Override
    public void exit() {
    }

    @Override
    public TextDocumentService getTextDocumentService() {
        return this.textDocumentService;
    }

    @Override
    public WorkspaceService getWorkspaceService() {
        return this.workspaceService;
    }

    @Override
    public void connect(LanguageClient client) {
        this.client = client;
        workspaceService.setClient(client);
        textDocumentService.setClient(client);
        client.logMessage(new MessageParams(MessageType.Warning, "BibtexLSPServer connected."));
    }
}
