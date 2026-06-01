package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrowserExtensionProviderDiscoveryTest {

    private static final String VALID_DISCOVERY = """
            {
              "name": "example",
              "displayName": "Example Provider",
              "port": 17893,
              "tokenFile": "/etc/example/token",
              "protocolVersion": 1
            }
            """;

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
        Files.writeString(tempDir.resolve("example.json"), VALID_DISCOVERY);

        BrowserExtensionProvider expected = new BrowserExtensionProvider(
                "example", "Example Provider", 17893, Path.of("/etc/example/token"), 1);

        assertEquals(List.of(expected), BrowserExtensionProviderDiscovery.discoverIn(tempDir));
    }

    @Test
    void discoverInSkipsMalformedJsonAndKeepsValidFiles(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("broken.json"), "not json");
        Files.writeString(tempDir.resolve("good.json"), VALID_DISCOVERY);

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
    void discoverInIgnoresNonJsonFiles(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("notes.txt"), "ignored");
        Files.writeString(tempDir.resolve("good.json"), VALID_DISCOVERY);

        assertEquals(1, BrowserExtensionProviderDiscovery.discoverIn(tempDir).size());
    }
}
