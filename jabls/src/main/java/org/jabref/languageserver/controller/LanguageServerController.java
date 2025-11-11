package org.jabref.languageserver.controller;

import org.jabref.languageserver.LspLauncher;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.remote.server.RemoteMessageHandler;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Manages the LspLauncher through typical life cycle methods.
public class LanguageServerController implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(LanguageServerController.class);

    private final CliPreferences cliPreferences;
    private final JournalAbbreviationRepository abbreviationRepository;

    @Nullable private LspLauncher lspLauncher;

    public LanguageServerController(CliPreferences cliPreferences, JournalAbbreviationRepository abbreviationRepository) {
        this.cliPreferences = cliPreferences;
        this.abbreviationRepository = abbreviationRepository;
        LOGGER.debug("LanguageServerController initialized.");
    }

    public synchronized void start(RemoteMessageHandler messageHandler, int port) {
        if (lspLauncher != null) {
            LOGGER.warn("Language server controller already started, cannot start again.");
            return;
        }

        lspLauncher = new LspLauncher(messageHandler, cliPreferences, abbreviationRepository, port);
        // This enqueues the thread to run in the background
        // The JVM will take care of running it at some point in time in the future
        // Thus, we cannot check directly if it really runs
        lspLauncher.start();
        LOGGER.debug("Triggered language server start up.");
    }

    public synchronized void stop() {
        LOGGER.debug("Stopping language server controller...");
        if (lspLauncher != null) {
            lspLauncher.interrupt();
            lspLauncher = null;
            LOGGER.debug("Language server stopped successfully.");
        } else {
            LOGGER.debug("Language server is not started, nothing to stop.");
        }
    }

    @Override
    public void close() {
        LOGGER.debug("Closing Language server controller...");
        stop();
    }
}
