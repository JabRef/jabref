package org.jabref.languageserver.manager;

import org.jabref.languageserver.LSPLauncher;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.preferences.CliPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This thread wrapper is required to be able to interrupt the language server, e.g. when JabRef is closing down the language server should shutdown as well.
 */
public class LanguageServerThread extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(LanguageServerThread.class);

    private final CliPreferences cliPreferences;
    private final JournalAbbreviationRepository abbreviationRepository;
    private LSPLauncher lspLauncher;
    private int port;

    public LanguageServerThread(CliPreferences cliPreferences, JournalAbbreviationRepository abbreviationRepository, int port) {
        this.cliPreferences = cliPreferences;
        this.abbreviationRepository = abbreviationRepository;
        this.lspLauncher = new LSPLauncher();
        this.port = port;
        this.setName("JabLs - JabRef Language Server on :" + port);
    }

    @Override
    public void run() {
        this.lspLauncher.run(cliPreferences, abbreviationRepository, port);
    }

    @Override
    public void interrupt() {
        LOGGER.debug("Interrupting {}", this.getName());
        if (this.lspLauncher == null) {
            LOGGER.warn("LSPLauncher is null, cannot shutdown.");
        } else {
            this.lspLauncher.shutdown();
        }
        super.interrupt();
    }

    public boolean started() {
        return lspLauncher != null && lspLauncher.isRunning();
    }
}
