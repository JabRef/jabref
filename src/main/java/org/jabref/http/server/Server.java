package org.jabref.http.server;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.SSLContext;

import javafx.collections.ObservableList;

import org.jabref.architecture.AllowedToUseStandardStreams;
import org.jabref.logic.util.OS;
import org.jabref.preferences.JabRefPreferences;

import jakarta.ws.rs.SeBootstrap;
import net.harawata.appdirs.AppDirsFactory;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

@AllowedToUseStandardStreams("This is a CLI application. It resides in the package http.server to be close to the other http server related classes.")
public class Server {
    private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);

    private static SeBootstrap.Instance serverInstance;

    /**
     * Starts an http server serving the last files opened in JabRef<br>
     * More files can be provided as args.
     */
    public static void main(final String[] args) throws InterruptedException, URISyntaxException {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        final ObservableList<Path> lastFilesOpened = JabRefPreferences.getInstance().getGuiPreferences().getLastFilesOpened();

        // The server serves the last opened files (see org.jabref.http.server.LibraryResource.getLibraryPath)
        // In a testing environment, this might be difficult to handle
        // This is a quick solution. The architectural fine solution would use some http context or other @Inject_ed variables in org.jabref.http.server.LibraryResource
        if (args.length > 0) {
            LOGGER.debug("Command line parameters passed");
            List<Path> filesToAdd = Arrays.stream(args)
                                          .map(Path::of)
                                          .filter(Files::exists)
                                          .filter(path -> !lastFilesOpened.contains(path))
                                          .toList();

            LOGGER.debug("Adding following files to the list of opened libraries: {}", filesToAdd);

            // add the files in the front of the last opened libraries
            for (Path path : filesToAdd.reversed()) {
                lastFilesOpened.addFirst(path);
            }
        }

        if (lastFilesOpened.isEmpty()) {
            LOGGER.debug("still no library available to serve, serve the demo library");
            // Server.class.getResource("...") is always null here, thus trying relative path
            // Path bibPath = Path.of(Server.class.getResource("http-server-demo.bib").toURI());
            Path bibPath = Path.of("src/main/resources/org/jabref/http/server/http-server-demo.bib").toAbsolutePath();
            LOGGER.debug("Location of demo library: {}", bibPath);
            lastFilesOpened.add(bibPath);
        }

        LOGGER.debug("Libraries served: {}", lastFilesOpened);

        Server.startServer();

        // Keep the http server running until user kills the process (e.g., presses Ctrl+C)
        Thread.currentThread().join();
    }

    private static void startServer() {
        SSLContext sslContext = getSslContext();
        SeBootstrap.Configuration configuration = SeBootstrap.Configuration
                .builder()
                .sslContext(sslContext)
                .protocol("HTTPS")
                .port(6051)
                .build();
        LOGGER.debug("Starting server...");
        SeBootstrap.start(Application.class, configuration).thenAccept(instance -> {
            LOGGER.debug("Server started.");
            instance.stopOnShutdown(stopResult ->
                    System.out.printf("Stop result: %s [Native stop result: %s].%n", stopResult,
                            stopResult.unwrap(Object.class)));
            final URI uri = instance.configuration().baseUri();
            System.out.printf("Instance %s running at %s [Native handle: %s].%n", instance, uri,
                    instance.unwrap(Object.class));
            System.out.println("Send SIGKILL to shutdown.");
            serverInstance = instance;
        });
    }

    private static SSLContext getSslContext() {
        SSLContextConfigurator sslContextConfig = new SSLContextConfigurator();
        Path serverKeyStore = Path.of(AppDirsFactory.getInstance()
                                         .getUserDataDir(
                                                 OS.APP_DIR_APP_NAME,
                                                 "ssl",
                                                 OS.APP_DIR_APP_AUTHOR))
                       .resolve("server.p12");
        if (Files.exists(serverKeyStore)) {
            sslContextConfig.setKeyStoreFile(serverKeyStore.toString());
            sslContextConfig.setKeyStorePass("changeit");
        } else {
            LOGGER.error("Could not find server key store {}.", serverKeyStore);
            LOGGER.error("One create one by following the steps described in [http-server.md](/docs/code-howtos/http-server.md), which is rendered at <https://devdocs.jabref.org/code-howtos/http-server.html>");
        }
        return sslContextConfig.createSSLContext();
    }

    static void stopServer() {
        serverInstance.stop();
    }
}
