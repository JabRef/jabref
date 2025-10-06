package org.jabref.languageserver;

import org.jabref.languageserver.util.LspDiagnosticHandler;

import com.google.gson.JsonObject;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BibtexWorkspaceService implements WorkspaceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BibtexWorkspaceService.class);

    private final LspClientHandler clientHandler;
    private final LspDiagnosticHandler diagnosticHandler;

    private LanguageClient client;

    public BibtexWorkspaceService(LspClientHandler clientHandler, LspDiagnosticHandler diagnosticHandler) {
        this.clientHandler = clientHandler;
        this.diagnosticHandler = diagnosticHandler;
    }

    // Todo: handle event
    @Override
    public void didChangeConfiguration(DidChangeConfigurationParams didChangeConfigurationParams) {
        if (didChangeConfigurationParams.getSettings() instanceof JsonObject settings) {
            LOGGER.debug("Received new settings: {}", settings);
            clientHandler.getSettings().copyFromJsonObject(settings);
            LOGGER.info("Updated settings to: {}", clientHandler.getSettings());
            diagnosticHandler.refreshDiagnostics(client);
            LOGGER.debug("Refreshed diagnostics.");
        }
        LOGGER.debug("DidChangeConfigurationParams: {}", didChangeConfigurationParams);
    }

    // Todo: handle event
    @Override
    public void didChangeWatchedFiles(DidChangeWatchedFilesParams didChangeWatchedFilesParams) {
        LOGGER.debug("DidChangeWatchedFilesParams: {}}", didChangeWatchedFilesParams);
    }

    public void setClient(LanguageClient client) {
        this.client = client;
    }
}
