package org.jabref.logic.browserext;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

import org.jabref.logic.os.OS;
import org.jabref.logic.util.strings.StringUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/// Talks to the loopback HTTP bridge the JabRef Browser Extension
/// (experimental) starts on demand (`bridge/JabExtBridge.java`, see
/// `docs/requirements/mathscinet.md`, `req~mathscinet.sync.transport~1`).
///
/// The bridge is discovered, not configured: it writes a small JSON file
/// naming its ephemeral port and a token file once it is running. If that
/// file is absent (extension not installed, or not started), every method
/// here fails silently by returning an empty [Optional] — the bridge is an
/// optional enhancement, never a hard dependency for identifier editing.
// [impl->req~mathscinet.sync.unavailable~1]
public class BrowserExtensionBridgeClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(BrowserExtensionBridgeClient.class);

    // Same provider name/discovery file as the fulltext-fetch bridge — this command is served by the same
    // process, not a second bridge.
    // [impl->req~mathscinet.sync.transport~1]
    private static final String PROVIDER_NAME = "jabext-experimental";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
                                                    .connectTimeout(REQUEST_TIMEOUT)
                                                    .build();
    private final Path discoveryDirectory;

    /// Result of a successful `/v1/mathscinet/open` call.
    ///
    /// @param action either `"opened"` (a new tab was created) or `"focused"` (an existing, extension-owned tab was reused)
    /// @param tabId  the browser's id for the tab, for diagnostics only
    public record MathSciNetOpenResult(String action, int tabId) {
    }

    private record Discovery(int port, String token) {
    }

    public BrowserExtensionBridgeClient() {
        this(defaultDiscoveryDirectory());
    }

    /// Visible for tests, to point at a temporary discovery directory instead of the real OS-specific one.
    BrowserExtensionBridgeClient(Path discoveryDirectory) {
        this.discoveryDirectory = discoveryDirectory;
    }

    /// Asks the browser extension to open (or focus and navigate) the one tab it
    /// keeps for this feature to the MathSciNet page for `mrNumber`. Returns
    /// empty if the bridge isn't running or the request fails for any reason.
    // [impl->req~mathscinet.sync.open-or-focus~1]
    public Optional<MathSciNetOpenResult> openMathSciNet(String mrNumber) {
        Optional<Discovery> discovery = readDiscovery();
        if (discovery.isEmpty()) {
            LOGGER.debug("No browser-extension bridge discovery file found; skipping MathSciNet browser sync");
            return Optional.empty();
        }
        Discovery bridge = discovery.get();

        String requestBody = MAPPER.createObjectNode().put("mrNumber", mrNumber).toString();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                                             .uri(new URI("http://127.0.0.1:%d/v1/mathscinet/open".formatted(bridge.port())))
                                             .header("Authorization", "Bearer " + bridge.token())
                                             .header("Content-Type", "application/json")
                                             .timeout(REQUEST_TIMEOUT)
                                             .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                                             .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                LOGGER.warn("MathSciNet browser sync request failed with HTTP {}: {}", response.statusCode(), response.body());
                return Optional.empty();
            }
            JsonNode json = MAPPER.readTree(response.body());
            JsonNode action = json.get("action");
            JsonNode tabId = json.get("tabId");
            if (action == null || tabId == null) {
                LOGGER.warn("MathSciNet browser sync response missing action/tabId: {}", response.body());
                return Optional.empty();
            }
            return Optional.of(new MathSciNetOpenResult(action.asString(), tabId.asInt()));
        } catch (IOException | JacksonException | URISyntaxException e) {
            LOGGER.warn("Could not reach browser-extension bridge for MathSciNet browser sync", e);
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    private Optional<Discovery> readDiscovery() {
        Path discoveryFile = discoveryDirectory.resolve(PROVIDER_NAME + ".json");
        if (!Files.isReadable(discoveryFile)) {
            return Optional.empty();
        }
        try {
            JsonNode node = MAPPER.readTree(discoveryFile.toFile());
            JsonNode port = node.get("port");
            JsonNode tokenFile = node.get("tokenFile");
            if (port == null || tokenFile == null) {
                LOGGER.warn("Browser-extension bridge discovery file {} missing port/tokenFile", discoveryFile);
                return Optional.empty();
            }
            String token = Files.readString(Path.of(tokenFile.asString()), StandardCharsets.UTF_8).strip();
            return Optional.of(new Discovery(port.asInt(), token));
        } catch (IOException | JacksonException e) {
            LOGGER.debug("Could not read browser-extension bridge discovery file {}", discoveryFile, e);
            return Optional.empty();
        }
    }

    /// Mirrors the path resolution in the JabRef Browser Extension's
    /// `bridge/JabExtBridge.java` (there: `JabRefPaths.jabrefConfigBase()`).
    /// The two repos share no code, so this **must** be kept in sync by hand
    /// with that method if either side changes.
    private static Path defaultDiscoveryDirectory() {
        return configBase().resolve("fulltext-providers");
    }

    private static Path configBase() {
        if (OS.WINDOWS) {
            String appData = System.getenv("APPDATA");
            Path base = StringUtil.isBlank(appData)
                        ? Path.of(System.getProperty("user.home"), "AppData", "Roaming")
                        : Path.of(appData);
            return base.resolve("JabRef");
        }
        if (OS.OS_X) {
            return Path.of(System.getProperty("user.home"), "Library", "Application Support", "JabRef");
        }
        String xdgConfigHome = System.getenv("XDG_CONFIG_HOME");
        Path base = StringUtil.isBlank(xdgConfigHome)
                    ? Path.of(System.getProperty("user.home"), ".config")
                    : Path.of(xdgConfigHome);
        return base.resolve("jabref");
    }
}
