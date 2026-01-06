package org.jabref.http.server;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.net.ssl.SSLContext;

import org.jabref.http.JabRefSrvStateManager;
import org.jabref.http.SrvStateManager;
import org.jabref.http.dto.GlobalExceptionMapper;
import org.jabref.http.dto.GsonFactory;
import org.jabref.http.server.cayw.CAYWResource;
import org.jabref.http.server.cayw.format.FormatterService;
import org.jabref.http.server.command.CommandResource;
import org.jabref.http.server.resources.LibrariesResource;
import org.jabref.http.server.resources.LibraryResource;
import org.jabref.http.server.resources.MapResource;
import org.jabref.http.server.resources.RootResource;
import org.jabref.http.server.services.FilesToServe;
import org.jabref.logic.os.OS;
import org.jabref.logic.preferences.CliPreferences;

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

public class Server {
    private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);
    private final CliPreferences preferences;

    public Server(CliPreferences preferences) {
        this.preferences = preferences;
    }

    /// Entry point for the CLI
    public HttpServer run(List<Path> files, URI uri) {
        List<Path> filesToServeList;
        if (files == null || files.isEmpty()) {
            LOGGER.debug("No library available to serve, serving the demo library...");
            // Server.class.getResource("...") is always null here, thus trying relative path
            // Path bibPath = Path.of(Server.class.getResource("http-server-demo.bib").toURI());
            Path bibPath = Path.of("src/main/resources/org/jabref/http/server/http-server-demo.bib").toAbsolutePath();
            LOGGER.debug("Location of demo library: {}", bibPath);
            filesToServeList = List.of(bibPath);
        } else {
            filesToServeList = files;
        }

        LOGGER.debug("Libraries to serve: {}", filesToServeList);

        FilesToServe filesToServe = new FilesToServe();
        filesToServe.setFilesToServe(filesToServeList);

        SrvStateManager srvStateManager = new JabRefSrvStateManager();

        ServiceLocator serviceLocator = ServiceLocatorUtilities.createAndPopulateServiceLocator();
        ServiceLocatorUtilities.addOneConstant(serviceLocator, filesToServe);
        ServiceLocatorUtilities.addOneConstant(serviceLocator, srvStateManager, "statemanager", SrvStateManager.class);
        HttpServer httpServer = startServer(serviceLocator, uri);

        // Required for CLI only
        // GUI uses HttpServerManager
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                LOGGER.debug("Shutting down jabsrv...");
                httpServer.shutdownNow();
                LOGGER.debug("Done, exit.");
            } catch (Exception e) {
                LOGGER.error("Could not shut down server", e);
            }
        }));

        return httpServer;
    }

    /// Entry point for the GUI
    public HttpServer run(SrvStateManager srvStateManager, URI uri) {
        FilesToServe filesToServe = new FilesToServe();

        ServiceLocator serviceLocator = ServiceLocatorUtilities.createAndPopulateServiceLocator();
        ServiceLocatorUtilities.addOneConstant(serviceLocator, filesToServe);
        ServiceLocatorUtilities.addOneConstant(serviceLocator, srvStateManager, "statemanager", SrvStateManager.class);

        return startServer(serviceLocator, uri);
    }

    private HttpServer startServer(ServiceLocator serviceLocator, URI uri) {
        ServiceLocatorUtilities.addOneConstant(serviceLocator, new FormatterService());
        ServiceLocatorUtilities.addOneConstant(serviceLocator, preferences, "preferences", CliPreferences.class);
        ServiceLocatorUtilities.addFactoryConstants(serviceLocator, new GsonFactory());

        // see https://stackoverflow.com/a/33794265/873282
        final ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.property("jersey.config.server.wadl.disableWadl", true);
        // TODO: Add SSL

        // RESTish resources
        resourceConfig.register(RootResource.class);
        resourceConfig.register(LibrariesResource.class);
        resourceConfig.register(LibraryResource.class);
        resourceConfig.register(MapResource.class);

        // Other resources
        resourceConfig.register(CommandResource.class);
        resourceConfig.register(CAYWResource.class);

        // Supporting classes
        resourceConfig.register(CORSFilter.class);
        resourceConfig.register(GlobalExceptionMapper.class);

        LOGGER.debug("Starting HTTP server...");

        return GrizzlyHttpServerFactory
                .createHttpServer(uri, resourceConfig, serviceLocator);
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
