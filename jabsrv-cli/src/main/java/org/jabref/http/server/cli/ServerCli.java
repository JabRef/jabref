package org.jabref.http.server.cli;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.jabref.architecture.AllowedToUseStandardStreams;
import org.jabref.http.server.Server;
import org.jabref.logic.citedrive.CiteDriveOAuthService;
import org.jabref.logic.citedrive.OAuthSessionRegistry;
import org.jabref.logic.preferences.JabRefCliPreferences;
import org.jabref.logic.remote.RemotePreferences;

import org.glassfish.grizzly.http.server.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import picocli.CommandLine;

@AllowedToUseStandardStreams("This is a CLI application. It resides in the package http.server to be close to the other http server related classes.")
@CommandLine.Command(name = "jabsrv", mixinStandardHelpOptions = true, description = "JabSrv - JabRef HTTP server")
public class ServerCli implements Callable<Integer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerCli.class);

    @CommandLine.Parameters(arity = "0..*", paramLabel = "FILE", description = "the library files (*.bib) to serve")
    List<Path> files;

    @CommandLine.Option(names = {"-H", "--host"}, description = "the host name")
    private String host = "localhost";

    @CommandLine.Option(names = {"-p", "--port"}, description = "the port")
    private Integer port = 23119;

    @CommandLine.Option(names = "--citedrive-login", description = "log in to CiteDrive in the browser, remember the login, and exit")
    private boolean citeDriveLogin;

    /// Starts an http server serving the last files opened in JabRef
    ///
    /// More files can be provided as args.
    ///
    /// @implNote method needs to be public, because JabServLauncher calls it.
    public static void main(final String[] args) throws InterruptedException {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        int exitCode = new CommandLine(new ServerCli()).execute(args);
        // Only reached after --citedrive-login: serving blocks until the process is killed
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws InterruptedException, IOException {
        JabRefCliPreferences preferences = JabRefCliPreferences.getInstance();

        // The server serves the last opened files (see org.jabref.http.server.resources.LibraryResource.getLibraryPath)
        final List<Path> filesToServe = new ArrayList<>(preferences.getLastFilesOpenedPreferences().getLastFilesOpened());

        // Additionally, files can be provided as args
        if (files != null) {
            List<Path> filesToAdd = files.stream()
                                         .filter(Files::exists)
                                         .filter(path -> !filesToServe.contains(path))
                                         .toList();
            LOGGER.info("Adding following files to the list of opened libraries: {}", filesToAdd);
            filesToServe.addAll(0, filesToAdd);
        }

        LOGGER.debug("Libraries to serve: {}", filesToServe);

        String url = "http://" + host + ":" + port + "/";
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException _) {
            LOGGER.error("Invalid URL: {}", url);
            return 1;
        }

        // Only --citedrive-login registers a pending login here; without it, the callback endpoint
        // answers every callback with "login could not be completed"
        OAuthSessionRegistry oAuthSessionRegistry = new OAuthSessionRegistry();
        Server server = new Server(preferences, oAuthSessionRegistry);
        HttpServer httpServer = server.run(filesToServe, uri);

        if (citeDriveLogin) {
            try {
                return logInToCiteDrive(preferences, oAuthSessionRegistry, uri);
            } finally {
                httpServer.shutdownNow();
            }
        }

        // Keep the http server running until user kills the process (e.g., presses Ctrl+C)
        Thread.currentThread().join();

        return 0;
    }

    /// The same flow as in the GUI, with the browser URL printed instead of opened. The refresh token ends up in the keyring, so JabRef itself is logged in afterwards.
    ///
    /// [impl->req~citedrive.login~1]
    ///
    /// @return the process exit code
    private static int logInToCiteDrive(JabRefCliPreferences preferences, OAuthSessionRegistry oAuthSessionRegistry, URI serverUri) throws InterruptedException {
        RemotePreferences remotePreferences = preferences.getRemotePreferences();

        // CiteDrive redirects the browser to the HTTP server address from the preferences: that has to be this server
        int callbackPort = remotePreferences.getHttpServerUri().getPort();
        if (callbackPort != serverUri.getPort()) {
            System.err.printf("CiteDrive redirects the browser to port %d (JabRef's HTTP server port), but jabsrv listens on port %d. Use --port %d.%n", callbackPort, serverUri.getPort(), callbackPort);
            return 1;
        }

        if (!preferences.getCiteDrivePreferences().shouldPersistRefreshToken()) {
            System.err.println("JabRef is set to forget the CiteDrive login between sessions, so a login from jabsrv would be lost on exit. Enable \"Remember the login between sessions\" in JabRef's preferences.");
            return 1;
        }

        CiteDriveOAuthService oAuthService = new CiteDriveOAuthService(
                remotePreferences,
                preferences.getCiteDrivePreferences(),
                oAuthSessionRegistry,
                authUrl -> System.out.println("Open this address in your browser to log in to CiteDrive:\n" + authUrl),
                // This server already receives the callback, whatever the preferences say
                () -> true);

        try {
            if (oAuthService.authorizeInteractive().get().isPresent()) {
                System.out.println("Logged in to CiteDrive.");
                return 0;
            }
            System.err.println("CiteDrive login failed: could not obtain access token.");
        } catch (ExecutionException e) {
            if (e.getCause() instanceof TimeoutException) {
                System.err.println("CiteDrive login failed: login was not completed in time.");
            } else {
                LOGGER.error("CiteDrive login failed", e.getCause());
                System.err.println("CiteDrive login failed: " + e.getCause().getLocalizedMessage());
            }
        }
        return 1;
    }
}
