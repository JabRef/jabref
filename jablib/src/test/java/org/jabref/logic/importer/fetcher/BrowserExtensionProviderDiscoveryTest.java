package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrowserExtensionProviderDiscoveryTest {

    /// Builds a discovery file whose `tokenFile` is absolute on the running OS.
    private static String discoveryJson(Path tokenFile) {
        return discoveryJson("example", 17893, tokenFile);
    }

    private static String discoveryJson(String name, int port, Path tokenFile) {
        return """
                {
                  "name": "%s",
                  "displayName": "Example Provider",
                  "port": %d,
                  "tokenFile": "%s",
                  "protocolVersion": 1
                }
                """.formatted(name, port, tokenFile.toString().replace("\\", "\\\\"));
    }

    @Test
    void discoveryDirectoryEndsWithFulltextProviders() {
        Path directory = BrowserExtensionProviderDiscovery.discoveryDirectory();
        assertTrue(directory.endsWith("fulltext-providers"),
                "Expected discovery directory to end with 'fulltext-providers', was: " + directory);
    }

    @Test
    void discoverInMissingDirectoryReturnsEmptyList(@TempDir Path tempDir) {
        Path missing = tempDir.resolve("does-not-exist");
        assertEquals(List.of(), BrowserExtensionProviderDiscovery.discoverIn(missing));
    }

    @Test
    void discoverInEmptyDirectoryReturnsEmptyList(@TempDir Path tempDir) {
        assertEquals(List.of(), BrowserExtensionProviderDiscovery.discoverIn(tempDir));
    }

    @Test
    void discoverInWithValidFileReturnsProvider(@TempDir Path tempDir) throws IOException {
        Path tokenFile = tempDir.resolve("token");
        Files.writeString(tempDir.resolve("example.json"), discoveryJson(tokenFile));

        BrowserExtensionProvider expected = new BrowserExtensionProvider(
                "example", "Example Provider", 17893, tokenFile, 1, tempDir.resolve("example.json"));

        assertEquals(List.of(expected), BrowserExtensionProviderDiscovery.discoverIn(tempDir));
    }

    @Test
    void discoverInSkipsMalformedJsonAndKeepsValidFiles(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("broken.json"), "not json");
        Files.writeString(tempDir.resolve("good.json"), discoveryJson(tempDir.resolve("token")));

        List<BrowserExtensionProvider> providers = BrowserExtensionProviderDiscovery.discoverIn(tempDir);
        assertEquals(1, providers.size());
        assertEquals("example", providers.getFirst().name());
    }

    @Test
    void discoverInSkipsFilesMissingRequiredFields(@TempDir Path tempDir) throws IOException {
        String missingPort = """
                {
                  "name": "example",
                  "displayName": "Example",
                  "tokenFile": "/etc/example/token",
                  "protocolVersion": 1
                }
                """;
        Files.writeString(tempDir.resolve("missing.json"), missingPort);

        assertEquals(List.of(), BrowserExtensionProviderDiscovery.discoverIn(tempDir));
    }

    @Test
    void discoverInSkipsUnsupportedProtocolVersion(@TempDir Path tempDir) throws IOException {
        String wrongVersion = """
                {
                  "name": "example",
                  "displayName": "Example",
                  "port": 17893,
                  "tokenFile": "/etc/example/token",
                  "protocolVersion": 2
                }
                """;
        Files.writeString(tempDir.resolve("v2.json"), wrongVersion);

        assertEquals(List.of(), BrowserExtensionProviderDiscovery.discoverIn(tempDir));
    }

    @Test
    void discoverInSkipsOutOfRangePort(@TempDir Path tempDir) throws IOException {
        String badPort = """
                {
                  "name": "example",
                  "displayName": "Example",
                  "port": 99999,
                  "tokenFile": "/etc/example/token",
                  "protocolVersion": 1
                }
                """;
        Files.writeString(tempDir.resolve("badport.json"), badPort);

        assertEquals(List.of(), BrowserExtensionProviderDiscovery.discoverIn(tempDir));
    }

    @Test
    void discoverInSkipsNonAbsoluteTokenFile(@TempDir Path tempDir) throws IOException {
        String relativeToken = """
                {
                  "name": "example",
                  "displayName": "Example",
                  "port": 17893,
                  "tokenFile": "relative/token",
                  "protocolVersion": 1
                }
                """;
        Files.writeString(tempDir.resolve("relative.json"), relativeToken);

        assertEquals(List.of(), BrowserExtensionProviderDiscovery.discoverIn(tempDir));
    }

    @Test
    void discoverInSkipsInvalidTokenFilePathAndKeepsValidFiles(@TempDir Path tempDir) throws IOException {
        // A NUL character makes Path.of throw InvalidPathException (unchecked) on every platform;
        // discovery must skip the file rather than abort the whole enumeration.
        String invalidTokenPath = """
                {
                  "name": "broken",
                  "displayName": "Broken",
                  "port": 17893,
                  "tokenFile": "/invalid/\\u0000/token",
                  "protocolVersion": 1
                }
                """;
        Files.writeString(tempDir.resolve("invalid.json"), invalidTokenPath);
        Files.writeString(tempDir.resolve("good.json"), discoveryJson(tempDir.resolve("token")));

        List<BrowserExtensionProvider> providers = BrowserExtensionProviderDiscovery.discoverIn(tempDir);
        assertEquals(1, providers.size());
        assertEquals("example", providers.getFirst().name());
    }

    @Test
    void discoverInIgnoresNonJsonFiles(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("notes.txt"), "ignored");
        Files.writeString(tempDir.resolve("good.json"), discoveryJson(tempDir.resolve("token")));

        assertEquals(1, BrowserExtensionProviderDiscovery.discoverIn(tempDir).size());
    }

    @Test
    void discoverReachableInKeepsLiveProviderAndDropsStaleFile(@TempDir Path tempDir) throws IOException, InterruptedException {
        Path tokenFile = tempDir.resolve("token");
        Files.writeString(tokenFile, "secret");
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse.Builder()
                    .code(200)
                    .body("{\"ok\":true,\"name\":\"live\",\"protocolVersion\":1}")
                    .build());
            server.start();
            Files.writeString(tempDir.resolve("live.json"), discoveryJson("live", server.getPort(), tokenFile));
            Files.writeString(tempDir.resolve("stale.json"), discoveryJson("stale", closedPort, tokenFile));

            List<BrowserExtensionProvider> providers = BrowserExtensionProviderDiscovery.discoverReachableIn(tempDir);

            assertEquals(List.of("live"), providers.stream().map(BrowserExtensionProvider::name).toList());
            RecordedRequest health = server.takeRequest();
            assertEquals("/v1/health", health.getTarget());
            assertEquals("Bearer secret", health.getHeaders().get("Authorization"));
        }
    }

    @Test
    void isReachableIsFalseForNon200Health(@TempDir Path tempDir) throws IOException {
        Path tokenFile = tempDir.resolve("token");
        Files.writeString(tokenFile, "secret");
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse.Builder().code(401).build());
            server.start();
            BrowserExtensionProvider provider = new BrowserExtensionProvider(
                    "p", "P", server.getPort(), tokenFile, 1, tempDir.resolve("p.json"));

            assertFalse(BrowserExtensionProviderDiscovery.isReachable(provider));
        }
    }
}
