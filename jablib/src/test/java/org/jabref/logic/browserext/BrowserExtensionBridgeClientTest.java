package org.jabref.logic.browserext;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrowserExtensionBridgeClientTest {

    @Test
    void openMathSciNetReturnsEmptyWhenNoDiscoveryFilePresent(@TempDir Path discoveryDir) {
        BrowserExtensionBridgeClient client = new BrowserExtensionBridgeClient(discoveryDir);

        assertEquals(Optional.empty(), client.openMathSciNet("619693"));
    }

    @Test
    void openMathSciNetReturnsEmptyWhenDiscoveryFileIsMalformed(@TempDir Path discoveryDir) throws IOException {
        Files.writeString(discoveryDir.resolve("jabext-experimental.json"), "{\"name\":\"jabext-experimental\"}");

        BrowserExtensionBridgeClient client = new BrowserExtensionBridgeClient(discoveryDir);

        assertEquals(Optional.empty(), client.openMathSciNet("619693"));
    }

    @Test
    void openMathSciNetSendsBearerTokenAndParsesResponse(@TempDir Path discoveryDir) throws IOException {
        Path tokenFile = discoveryDir.resolve("jabext-experimental.token");
        Files.writeString(tokenFile, "s3cr3t-token");

        AtomicReference<String> receivedAuthHeader = new AtomicReference<>();
        AtomicReference<String> receivedBody = new AtomicReference<>();

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/mathscinet/open", exchange -> {
            try (exchange) {
                receivedAuthHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
                receivedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                byte[] responseBody = "{\"action\":\"opened\",\"tabId\":42}".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, responseBody.length);
                try (OutputStream body = exchange.getResponseBody()) {
                    body.write(responseBody);
                }
            }
        });
        server.start();
        try {
            int port = server.getAddress().getPort();
            Files.writeString(discoveryDir.resolve("jabext-experimental.json"), """
                    {"name":"jabext-experimental","displayName":"JabRef Browser Extension (experimental)","port":%d,"tokenFile":"%s","protocolVersion":1}
                    """.formatted(port, tokenFile.toString().replace("\\", "\\\\")));

            BrowserExtensionBridgeClient client = new BrowserExtensionBridgeClient(discoveryDir);
            Optional<BrowserExtensionBridgeClient.MathSciNetOpenResult> result = client.openMathSciNet("619693");

            assertEquals(Optional.of(new BrowserExtensionBridgeClient.MathSciNetOpenResult("opened", 42)), result);
            assertEquals("Bearer s3cr3t-token", receivedAuthHeader.get());
            assertTrue(receivedBody.get().contains("619693"));
        } finally {
            server.stop(0);
        }
    }
}
