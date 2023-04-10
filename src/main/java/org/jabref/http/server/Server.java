package org.jabref.http.server;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;

import javax.net.ssl.SSLContext;

import org.jabref.logic.util.OS;

import jakarta.ws.rs.SeBootstrap;
import net.harawata.appdirs.AppDirsFactory;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Server {
    private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);

    private static SeBootstrap.Instance serverInstance;

    static void startServer(CountDownLatch latch) {
        SSLContext sslContext = getSslContext();
        SeBootstrap.Configuration configuration = SeBootstrap.Configuration
                .builder()
                .sslContext(sslContext)
                .protocol("HTTPS")
                .port(6051)
                .build();
        SeBootstrap.start(Application.class, configuration).thenAccept(instance -> {
            instance.stopOnShutdown(stopResult ->
                    System.out.printf("Stop result: %s [Native stop result: %s].%n", stopResult,
                            stopResult.unwrap(Object.class)));
            final URI uri = instance.configuration().baseUri();
            System.out.printf("Instance %s running at %s [Native handle: %s].%n", instance, uri,
                    instance.unwrap(Object.class));
            System.out.println("Send SIGKILL to shutdown.");
            serverInstance = instance;
            latch.countDown();
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
