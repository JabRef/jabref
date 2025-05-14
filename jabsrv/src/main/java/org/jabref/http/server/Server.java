package org.jabref.http.server;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.net.ssl.SSLContext;

import org.jabref.http.dto.GlobalExceptionMapper;
import org.jabref.http.dto.GsonFactory;
import org.jabref.http.server.services.FilesToServe;
import org.jabref.logic.os.OS;

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

    /// TODO: Use an observable list of BibDatabaseContexts
    public HttpServer run(List<Path> files, URI uri) {
        List filesToServe;
        if (files == null || files.isEmpty()) {
            LOGGER.debug("No library available to serve, serving the demo library...");
            // Server.class.getResource("...") is always null here, thus trying relative path
            // Path bibPath = Path.of(Server.class.getResource("http-server-demo.bib").toURI());
            Path bibPath = Path.of("src/main/resources/org/jabref/http/server/http-server-demo.bib").toAbsolutePath();
            LOGGER.debug("Location of demo library: {}", bibPath);
            filesToServe = List.of(bibPath);
        } else {
            filesToServe = files;
        }

        LOGGER.debug("Libraries to serve: {}", filesToServe);

        FilesToServe filesToServeService = new FilesToServe();
        filesToServeService.setFilesToServe(filesToServe);

        return startServer(filesToServeService, uri);
    }

    private HttpServer startServer(ServiceLocator serviceLocator, URI uri) {
        // see https://stackoverflow.com/a/33794265/873282
        final ResourceConfig resourceConfig = new ResourceConfig();
        // TODO: Add SSL
        resourceConfig.register(RootResource.class);
        resourceConfig.register(LibrariesResource.class);
        resourceConfig.register(LibraryResource.class);
        resourceConfig.register(CORSFilter.class);
        resourceConfig.register(GlobalExceptionMapper.class);

        LOGGER.debug("Starting server...");
        final HttpServer httpServer =
                GrizzlyHttpServerFactory
                        .createHttpServer(uri, resourceConfig, serviceLocator);
        return httpServer;
    }

    private HttpServer startServer(FilesToServe filesToServe, URI uri) {
        ServiceLocator serviceLocator = ServiceLocatorUtilities.createAndPopulateServiceLocator();
        ServiceLocatorUtilities.addFactoryConstants(serviceLocator, new GsonFactory());
        ServiceLocatorUtilities.addFactoryConstants(serviceLocator, new PreferencesFactory());
        ServiceLocatorUtilities.addOneConstant(serviceLocator, filesToServe);

        final HttpServer httpServer = startServer(serviceLocator, uri);

        // TODO: Enable use of GUI StateManager
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                System.out.println("Shutting down jabsrv...");
                httpServer.shutdownNow();
                System.out.println("Done, exit.");
            } catch (Exception e) {
                LOGGER.error("Could not shut down server", e);
            }
        }));

        return httpServer;
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
