package org.jabref.languageserver.manager;

import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.preferences.CliPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Manages the LanguageServerThread through typical life cycle methods.
public class LanguageServerManager implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(LanguageServerManager.class);

    private LanguageServerThread languageServerThread;

    public synchronized void start(CliPreferences cliPreferences, JournalAbbreviationRepository abbreviationRepository, int port) {
        if (languageServerThread != null) {
            LOGGER.warn("Language server manager already started, cannot start again.");
            return;
        }

        languageServerThread = new LanguageServerThread(cliPreferences, abbreviationRepository, port);
        // This enqueues the thread to run in the background
        // The JVM will take care of running it at some point in time in the future
        // Thus, we cannot check directly if it really runs
        languageServerThread.start();
        LOGGER.debug("Triggered language server start up.");
    }

    public synchronized void stop() {
        LOGGER.debug("Stopping language server manager...");
        if (languageServerThread != null) {
            languageServerThread.interrupt();
            languageServerThread = null;
            LOGGER.debug("Language server stopped successfully.");
        } else {
            LOGGER.debug("Language server is not started, nothing to stop.");
        }
    }

    @Override
    public void close() {
        LOGGER.debug("Closing Language server manager...");
        stop();
    }
}
