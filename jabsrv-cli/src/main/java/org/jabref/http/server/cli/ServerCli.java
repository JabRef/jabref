package org.jabref.http.server.cli;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.jabref.architecture.AllowedToUseStandardStreams;
import org.jabref.http.server.Server;
import org.jabref.logic.preferences.JabRefCliPreferences;

import org.glassfish.grizzly.http.server.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import picocli.CommandLine;

@AllowedToUseStandardStreams("This is a CLI application. It resides in the package http.server to be close to the other http server related classes.")
@CommandLine.Command(name = "server", mixinStandardHelpOptions = true, description = "JabSrv - JabRef HTTP server")
public class ServerCli implements Callable<Void> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerCli.class);

    @CommandLine.Parameters(arity = "0..*", paramLabel = "FILE", description = "the library files (*.bib) to serve")
    List<Path> files;

    @CommandLine.Option(names = {"-h", "--host"}, description = "the host name")
    private String host = "localhost";

    @CommandLine.Option(names = {"-p", "--port"}, description = "the port")
    private Integer port = 23119;

    /**
     * Starts an http server serving the last files opened in JabRef<br>
     * More files can be provided as args.
     *
     * @implNote method needs to be public, because JabServLauncher calls it.
     */
    public static void main(final String[] args) throws InterruptedException {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        new CommandLine(new ServerCli()).execute(args);
    }

    @Override
    public Void call() throws InterruptedException {
        // The server serves the last opened files (see org.jabref.http.server.resources.LibraryResource.getLibraryPath)
        final List<Path> filesToServe = new ArrayList<>(JabRefCliPreferences.getInstance().getLastFilesOpenedPreferences().getLastFilesOpened());

        // Additionally, files can be provided as args
        if (files != null) {
            List<Path> filesToAdd = files.stream()
                                         .filter(Files::exists)
                                         .filter(path -> !filesToServe.contains(path))
                                         .toList();
            LOGGER.info("Adding following files to the list of opened libraries: {}", filesToAdd);
            filesToServe.addAll(0, filesToAdd);
        }

        // If we are on Windows and checked-out JabRef at the location given in the workspace setup guideline, we can serve Chocolate.bib, too
        // Required by rest-api.http
        Path exampleChocolateBib = Path.of("C:\\git-repositories\\JabRef\\jablib\\src\\main\\resources\\Chocolate.bib");
        if (Files.exists(exampleChocolateBib)) {
            filesToServe.add(exampleChocolateBib);
        }

        LOGGER.debug("Libraries to serve: {}", filesToServe);

        String url = "http://" + host + ":" + port + "/";
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            LOGGER.error("Invalid URL: {}", url);
            return null;
        }

        Server server = new Server();
        HttpServer httpServer = server.run(filesToServe, uri);

        // Keep the http server running until user kills the process (e.g., presses Ctrl+C)
        Thread.currentThread().join();

        return null;
    }
}
