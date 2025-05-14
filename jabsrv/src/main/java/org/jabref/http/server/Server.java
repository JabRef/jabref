package org.jabref.http.server;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import javax.net.ssl.SSLContext;

import org.jabref.architecture.AllowedToUseStandardStreams;
import org.jabref.http.dto.GlobalExceptionMapper;
import org.jabref.http.dto.GsonFactory;
import org.jabref.http.server.services.FilesToServe;
import org.jabref.logic.os.OS;
import org.jabref.logic.preferences.JabRefCliPreferences;

import net.harawata.appdirs.AppDirsFactory;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import picocli.CommandLine;

@AllowedToUseStandardStreams("This is a CLI application. It resides in the package http.server to be close to the other http server related classes.")
@CommandLine.Command(name = "server", mixinStandardHelpOptions = true, description = "JabSrv - JabRef HTTP server")
public class Server implements Callable<Void> {
    private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);

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

        new CommandLine(new Server()).execute(args);
    }

    @Override
    public Void call() throws InterruptedException {
        final List<Path> filesToServe = JabRefCliPreferences.getInstance().getLastFilesOpenedPreferences().getLastFilesOpened().stream().collect(Collectors.toCollection(ArrayList::new));

        // The server serves the last opened files (see org.jabref.http.server.LibraryResource.getLibraryPath)
        // In a testing environment, this might be difficult to handle
        // This is a quick solution. The architectural fine solution would use some http context or other @Inject_ed variables in org.jabref.http.server.LibraryResource
        if (files != null) {
            List<Path> filesToAdd = files.stream()
                                          .filter(Files::exists)
                                          .filter(path -> !filesToServe.contains(path))
                                          .toList();
            LOGGER.debug("Adding following files to the list of opened libraries: {}", filesToAdd);
            filesToServe.addAll(0, filesToAdd);
        }

        if (filesToServe.isEmpty()) {
            LOGGER.debug("No library available to serve, serving the demo library...");
            // Server.class.getResource("...") is always null here, thus trying relative path
            // Path bibPath = Path.of(Server.class.getResource("http-server-demo.bib").toURI());
            Path bibPath = Path.of("src/main/resources/org/jabref/http/server/http-server-demo.bib").toAbsolutePath();
            LOGGER.debug("Location of demo library: {}", bibPath);
            filesToServe.add(bibPath);
        }

        LOGGER.debug("Libraries to serve: {}", filesToServe);

        FilesToServe filesToServeService = new FilesToServe();
        filesToServeService.setFilesToServe(filesToServe);

        startServer(filesToServeService);

        // Keep the http server running until user kills the process (e.g., presses Ctrl+C)
        Thread.currentThread().join();

        return null;
    }

    public HttpServer startServer(ServiceLocator serviceLocator) {
        // see https://stackoverflow.com/a/33794265/873282
        final ResourceConfig resourceConfig = new ResourceConfig();
        // TODO: Add SSL
        resourceConfig.register(RootResource.class);
        resourceConfig.register(LibrariesResource.class);
        resourceConfig.register(LibraryResource.class);
        resourceConfig.register(CORSFilter.class);
        resourceConfig.register(GlobalExceptionMapper.class);

        LOGGER.debug("Starting server...");
        String url = "http://" + host + ":" + port + "/";
        final HttpServer httpServer =
                GrizzlyHttpServerFactory
                        .createHttpServer(URI.create(url), resourceConfig, serviceLocator);
        return httpServer;
    }

    private void startServer(FilesToServe filesToServe) {
        ServiceLocator serviceLocator = ServiceLocatorUtilities.createAndPopulateServiceLocator();
        ServiceLocatorUtilities.addFactoryConstants(serviceLocator, new GsonFactory());
        ServiceLocatorUtilities.addFactoryConstants(serviceLocator, new PreferencesFactory());
        ServiceLocatorUtilities.addOneConstant(serviceLocator, filesToServe);

        try {
            final HttpServer httpServer = startServer(serviceLocator);

            // add jvm shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    System.out.println("Shutting down jabsrv...");
                    httpServer.shutdownNow();
                    System.out.println("Done, exit.");
                } catch (Exception e) {
                    LOGGER.error("Could not shut down server", e);
                }
            }));

            System.out.println("JabSrv started.");
            System.out.println("Stop JabSrv using Ctrl+C");

            Thread.currentThread().join();
        } catch (InterruptedException ex) {
            LOGGER.error("Could not start down server", ex);
        }
    }

    private boolean sslCertExists() {
        Path serverKeyStore = getSslCert();
        return Files.exists(serverKeyStore);
    }

    private SSLContext getSslContext() {
        SSLContextConfigurator sslContextConfig = new SSLContextConfigurator();
        Path serverKeyStore = getSslCert();
        if (Files.exists(serverKeyStore)) {
            sslContextConfig.setKeyStoreFile(serverKeyStore.toString());
            sslContextConfig.setKeyStorePass("changeit");
        } else {
            LOGGER.error("Could not find server key store {}.", serverKeyStore);
            LOGGER.error("One create one by following the steps described in [http-server.md](/docs/code-howtos/http-server.md), which is rendered at <https://devdocs.jabref.org/code-howtos/http-server.html>");
        }
        return sslContextConfig.createSSLContext(false);
    }

    @NonNull
    private Path getSslCert() {
        return Path.of(AppDirsFactory.getInstance()
                                     .getUserDataDir(
                                             OS.APP_DIR_APP_NAME,
                                             "ssl",
                                             OS.APP_DIR_APP_AUTHOR))
                   .resolve("server.p12");
    }

    static void stopServer() {
        // serverInstance.stop();
    }
}
