package org.jabref.languageserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.preferences.JabRefCliPreferences;
import org.jabref.logic.remote.server.RemoteMessageHandler;
import org.jabref.model.entry.BibEntryTypesManager;

import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LspLauncher extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(LspLauncher.class);

    private final CliPreferences cliPreferences;
    private final JournalAbbreviationRepository abbreviationRepository;
    private final ExecutorService threadPool;
    private final RemoteMessageHandler messageHandler;
    private final BibEntryTypesManager bibEntryTypesManager;

    private final int port;
    private boolean standalone = false;
    private volatile boolean running;
    private ServerSocket serverSocket;

    public LspLauncher(RemoteMessageHandler messageHandler, CliPreferences cliPreferences, JournalAbbreviationRepository abbreviationRepository, BibEntryTypesManager bibEntryTypesManager, int port) {
        this.cliPreferences = cliPreferences;
        this.abbreviationRepository = abbreviationRepository;
        this.threadPool = Executors.newCachedThreadPool();
        this.bibEntryTypesManager = bibEntryTypesManager;
        this.port = port;
        this.setName("JabLs - JabRef Language Server on: " + port);
        this.messageHandler = messageHandler;
    }

    public LspLauncher(RemoteMessageHandler messageHandler, CliPreferences cliPreferences, int port) {
        this(messageHandler, cliPreferences, JournalAbbreviationLoader.loadRepository(cliPreferences.getJournalAbbreviationPreferences()), cliPreferences.getCustomEntryTypesRepository(), port);
    }

    public LspLauncher(JabRefCliPreferences instance, Integer port) {
        this(_ -> LOGGER.warn("LSP cannot handle UICommands in standalone mode."), instance, port);
        this.standalone = true;
    }

    @Override
    public void run() {
        running = true;
        try {
            serverSocket = new ServerSocket(port);
            threadPool.execute(() -> {
                LOGGER.info("LSP Server listening on port {}...", port);
                while (running) {
                    try {
                        Socket socket = serverSocket.accept();
                        LOGGER.info("LSP Client connected.");
                        threadPool.submit(() -> handleClient(socket));
                    } catch (IOException e) {
                        if (running) {
                            LOGGER.error("Error during LSP run", e);
                        } else {
                            LOGGER.debug("Error while not running", e);
                        }
                    }
                }
            });
        } catch (IOException e) {
            LOGGER.error("Error during LSP run", e);
        }
    }

    private void handleClient(Socket socket) {
        LspClientHandler clientHandler = new LspClientHandler(messageHandler, cliPreferences, abbreviationRepository, bibEntryTypesManager);
        clientHandler.setStandalone(standalone);
        LOGGER.debug("LSP clientHandler started.");
        try (socket; // socket should be closed on error
             InputStream in = socket.getInputStream();
             OutputStream out = socket.getOutputStream()) {
            Launcher<LanguageClient> launcher = LSPLauncher.createServerLauncher(clientHandler, in, out, Executors.newCachedThreadPool(), Function.identity());
            LOGGER.debug("LSP clientHandler launched.");
            clientHandler.connect(launcher.getRemoteProxy());
            LOGGER.debug("LSP clientHandler connected.");
            launcher.startListening().get();
        } catch (Throwable e) {
            LOGGER.error("Error in handleClient", e);
        } finally {
            LOGGER.info("LSP Client disconnected.");
        }
    }

    @Override
    public void interrupt() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            if (threadPool != null && !threadPool.isShutdown()) {
                threadPool.shutdownNow();
            }
        } catch (IOException e) {
            LOGGER.error("Error during LSP shutdown", e);
        }
        running = false;
        LOGGER.info("LSP Server shutdown.");
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isStandalone() {
        return standalone;
    }
}
