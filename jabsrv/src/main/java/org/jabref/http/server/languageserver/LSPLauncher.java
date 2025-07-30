package org.jabref.http.server.languageserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.preferences.CliPreferences;

import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LSPLauncher {

    private static final Logger LOGGER = LoggerFactory.getLogger(LSPLauncher.class);

    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private volatile boolean running;
    private CliPreferences jabRefCliPreferences;
    private JournalAbbreviationRepository abbreviationRepository;

    public LSPLauncher(CliPreferences cliPreferences, JournalAbbreviationRepository abbreviationRepository) {
        this.jabRefCliPreferences = cliPreferences;
        this.abbreviationRepository = abbreviationRepository;
        start(2087);
    }

    public void start(int port) {
        try {
            serverSocket = new ServerSocket(port);
            threadPool = Executors.newCachedThreadPool();
            running = true;

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
        LSPServer server = new LSPServer(jabRefCliPreferences, abbreviationRepository);
        LOGGER.debug("LSP server started.");
        try (socket; // socket should be closed on error
             InputStream in = socket.getInputStream();
             OutputStream out = socket.getOutputStream()) {
            Launcher<LanguageClient> launcher = org.eclipse.lsp4j.launch.LSPLauncher.createServerLauncher(server, in, out, Executors.newCachedThreadPool(), Function.identity());
            LOGGER.debug("LSP server launched.");
            server.connect(launcher.getRemoteProxy());
            LOGGER.debug("LSP server connected.");
            launcher.startListening().get();
        } catch (Throwable e) {
            LOGGER.error("Error in handleClient", e);
        } finally {
            LOGGER.info("LSP Client disconnected.");
        }
    }

    public void shutdown() {
        running = false;
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
        LOGGER.info("LSP Server shutdown.");
    }
}
