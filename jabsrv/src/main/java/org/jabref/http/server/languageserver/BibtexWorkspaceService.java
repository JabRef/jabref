package org.jabref.http.server.languageserver;

import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.WorkspaceService;

public class BibtexWorkspaceService implements WorkspaceService {

    private LanguageClient client;

    @Override
    public void didChangeConfiguration(DidChangeConfigurationParams didChangeConfigurationParams) {
        System.out.printf("DidChangeConfigurationParams: %s%n", didChangeConfigurationParams);
    }

    @Override
    public void didChangeWatchedFiles(DidChangeWatchedFilesParams didChangeWatchedFilesParams) {
        System.out.printf("DidChangeWatchedFilesParams: %s%n", didChangeWatchedFilesParams);
    }

    public void setClient(LanguageClient client) {
        this.client = client;
    }
}