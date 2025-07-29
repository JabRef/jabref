package org.jabref.http.server.languageserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LSPLauncher {

    private static final Logger LOGGER = LoggerFactory.getLogger(LSPLauncher.class);

    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private volatile boolean running;

    public LSPLauncher() {
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
                        LOGGER.info("LSP Client connected!");

                        threadPool.submit(() -> handleClient(socket));
                    } catch (IOException e) {
                        if (running) {
                            LOGGER.error(e.getMessage(), e);
                        }
                    }
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleClient(Socket socket) {
        LSPServer server = new LSPServer();
        try (socket; InputStream in = socket.getInputStream();
             OutputStream out = socket.getOutputStream()) {
            Launcher<LanguageClient> launcher = org.eclipse.lsp4j.launch.LSPLauncher.createServerLauncher(server, in, out, Executors.newCachedThreadPool(), Function.identity());

            server.connect(launcher.getRemoteProxy());

            launcher.startListening().get();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
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
            LOGGER.error(e.getMessage(), e);
        }
        LOGGER.info("LSP Server shutdown.");
    }
}
