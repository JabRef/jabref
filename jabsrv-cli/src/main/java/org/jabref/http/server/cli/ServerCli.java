package org.jabref.http.server.cli;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
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

    @CommandLine.Parameters(arity = "0..*", paramLabel = "FILE")
    List<Path> files;

    @CommandLine.Option(names = {"-h", "--host"}, description = "the host name")
    private String host = "localhost";

    @CommandLine.Option(names = {"-p", "--port"}, description = "the port")
    private Integer port = 6050;

    /**
     * Starts an http server serving the last files opened in JabRef<br>
     * More files can be provided as args.
     */
    public static void main(final String[] args) throws InterruptedException {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        new CommandLine(new ServerCli()).execute(args);
    }

    @Override
    public Void call() throws InterruptedException {
        final List<Path> filesToServe = new ArrayList<>(JabRefCliPreferences.getInstance().getLastFilesOpenedPreferences().getLastFilesOpened());

        // The server serves the last opened files (see org.jabref.http.server.LibraryResource.getLibraryPath)
        // In a testing environment, this might be difficult to handle
        // This is a quick solution. The architectural fine solution would use some http context or other @Inject_ed variables in org.jabref.http.server.LibraryResource
        if (files != null) {
            List<Path> filesToAdd = files.stream()
                                          .filter(Files::exists)
                                          .filter(path -> !filesToServe.contains(path))
                                          .toList();
            LOGGER.info("Adding following files to the list of opened libraries: {}", filesToAdd);
            filesToServe.addAll(0, filesToAdd);
        }

        if (filesToServe.isEmpty()) {
            LOGGER.info("No library available to serve, serving the demo library...");
            Path bibPath = null;
            URL resource = Server.class.getResource("http-server-demo.bib");
            if (resource != null) {
                try {
                    bibPath = Path.of(resource.toURI());
                } catch (URISyntaxException e) {
                    LOGGER.error("Error while converting URL to URI", e);
                }
            }
            if (bibPath == null) {
                // Server.class.getResource("...") is null when executing with IntelliJ
                bibPath = Path.of("src/main/resources/org/jabref/http/server/http-server-demo.bib").toAbsolutePath();
                if (Files.notExists(bibPath)) {
                    bibPath = null;
                    LOGGER.debug("http-server-demo.bib not found");
                }
            }

            if (bibPath == null) {
                LOGGER.info("No library to serve. Please provide a library file as argument.");
            } else {
                LOGGER.debug("Location of demo library: {}", bibPath);
                filesToServe.add(bibPath);
            }
        }

        LOGGER.debug("Libraries to serve: {}", filesToServe);

        String url = "http://" + host + ":" + port + "/";
        URI uri = URI.create(url);

        Server server = new Server();
        HttpServer httpServer = server.run(filesToServe, uri);

        // Keep the http server running until user kills the process (e.g., presses Ctrl+C)
        Thread.currentThread().join();

        return null;
    }
}
