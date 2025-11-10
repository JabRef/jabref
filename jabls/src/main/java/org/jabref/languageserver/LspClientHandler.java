package org.jabref.languageserver;

import java.util.concurrent.CompletableFuture;

import org.jabref.languageserver.util.LspDiagnosticHandler;
import org.jabref.languageserver.util.LspLinkHandler;
import org.jabref.languageserver.util.LspParserHandler;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.remote.server.RemoteMessageHandler;

import org.eclipse.lsp4j.DocumentLinkOptions;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.TextDocumentSyncOptions;
import org.eclipse.lsp4j.WorkspaceServerCapabilities;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LspClientHandler implements LanguageServer, LanguageClientAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(LspClientHandler.class);

    private final LspDiagnosticHandler diagnosticHandler;
    private final LspParserHandler parserHandler;
    private final LspLinkHandler linkHandler;
    private final BibtexWorkspaceService workspaceService;
    private final BibtexTextDocumentService textDocumentService;
    private final ExtensionSettings settings;
    private final RemoteMessageHandler messageHandler;

    private LanguageClient client;
    private boolean standalone = false;

    public LspClientHandler(RemoteMessageHandler messageHandler, CliPreferences cliPreferences, JournalAbbreviationRepository abbreviationRepository) {
        this.settings = ExtensionSettings.getDefaultSettings();
        this.parserHandler = new LspParserHandler();
        this.diagnosticHandler = new LspDiagnosticHandler(this, parserHandler, cliPreferences, abbreviationRepository);
        this.linkHandler = new LspLinkHandler(this, parserHandler, cliPreferences.getFilePreferences());
        this.workspaceService = new BibtexWorkspaceService(this, diagnosticHandler);
        this.textDocumentService = new BibtexTextDocumentService(messageHandler, this, diagnosticHandler, linkHandler);
        this.messageHandler = messageHandler;
    }

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
        ServerCapabilities capabilities = new ServerCapabilities();

        TextDocumentSyncOptions syncOptions = new TextDocumentSyncOptions();
        syncOptions.setSave(true);
        syncOptions.setChange(TextDocumentSyncKind.Full);
        syncOptions.setOpenClose(true);

        capabilities.setTextDocumentSync(syncOptions);
        capabilities.setWorkspace(new WorkspaceServerCapabilities());
        capabilities.setDefinitionProvider(true);

        DocumentLinkOptions linkOptions = new DocumentLinkOptions();
        linkOptions.setResolveProvider(true);
        capabilities.setDocumentLinkProvider(linkOptions);

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

    public ExtensionSettings getSettings() {
        return settings;
    }

    @Override
    public void connect(LanguageClient client) {
        this.client = client;
        workspaceService.setClient(client);
        textDocumentService.setClient(client);
        client.logMessage(new MessageParams(MessageType.Warning, "BibtexLSPServer connected."));
    }

    public void setStandalone(boolean standalone) {
        this.standalone = standalone;
    }

    public boolean isStandalone() {
        return standalone;
    }
}
