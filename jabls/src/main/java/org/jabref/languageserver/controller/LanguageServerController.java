package org.jabref.languageserver.controller;

import org.jabref.languageserver.LspLauncher;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.remote.server.RemoteMessageHandler;
import org.jabref.model.entry.BibEntryTypesManager;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Manages the LspLauncher through typical life cycle methods.
public class LanguageServerController implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(LanguageServerController.class);

    private final CliPreferences cliPreferences;
    private final JournalAbbreviationRepository abbreviationRepository;
    private final BibEntryTypesManager bibEntryTypesManager;

    @Nullable private LspLauncher lspLauncher;

    public LanguageServerController(CliPreferences cliPreferences, JournalAbbreviationRepository abbreviationRepository, BibEntryTypesManager bibEntryTypesManager) {
        this.cliPreferences = cliPreferences;
        this.abbreviationRepository = abbreviationRepository;
        this.bibEntryTypesManager = bibEntryTypesManager;
        LOGGER.debug("LanguageServerController initialized.");
    }

    public synchronized void start(RemoteMessageHandler messageHandler, int port) {
        if (lspLauncher != null) {
            LOGGER.warn("LSP server controller already started, cannot start again.");
            return;
        }

        lspLauncher = new LspLauncher(messageHandler, cliPreferences, abbreviationRepository, bibEntryTypesManager, port);
        // This enqueues the thread to run in the background
        // The JVM will take care of running it at some point in time in the future
        // Thus, we cannot check directly if it really runs
        lspLauncher.start();
        LOGGER.debug("Triggered LSP server start up.");
    }

    public synchronized void stop() {
        LOGGER.debug("Stopping LSP server controller...");
        if (lspLauncher != null) {
            lspLauncher.interrupt();
            lspLauncher = null;
            LOGGER.debug("LSP server stopped successfully.");
        } else {
            LOGGER.debug("LSP server is not started, nothing to stop.");
        }
    }

    @Override
    public void close() {
        LOGGER.debug("Closing LSP server controller...");
        stop();
    }
}
