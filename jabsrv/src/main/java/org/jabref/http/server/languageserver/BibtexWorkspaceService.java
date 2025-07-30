package org.jabref.http.server.languageserver;

import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BibtexWorkspaceService implements WorkspaceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BibtexWorkspaceService.class);

    private LanguageClient client;

    @Override
    public void didChangeConfiguration(DidChangeConfigurationParams didChangeConfigurationParams) {
        LOGGER.debug("DidChangeConfigurationParams: {}", didChangeConfigurationParams);
    }

    @Override
    public void didChangeWatchedFiles(DidChangeWatchedFilesParams didChangeWatchedFilesParams) {
        LOGGER.debug("DidChangeWatchedFilesParams: {}}", didChangeWatchedFilesParams);
    }

    public void setClient(LanguageClient client) {
        this.client = client;
    }
}
