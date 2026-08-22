package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.os.OS;
import org.jabref.logic.util.strings.StringUtil;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Enumerates browser-extension fulltext providers from JabRef's well-known
/// discovery directory.
///
/// See `docs/requirements/browser-extension-fulltext.md`.
@NullMarked
public final class BrowserExtensionProviderDiscovery {

    private static final Logger LOGGER = LoggerFactory.getLogger(BrowserExtensionProviderDiscovery.class);
    private static final Gson GSON = new Gson();
    private static final int SUPPORTED_PROTOCOL_VERSION = 1;

    /// Wire-format mirror of the discovery JSON, deserialised directly by Gson.
    /// Fields are boxed so missing entries surface as `null` for validation.
    private record DiscoveryFile(
            @Nullable String name,
            @Nullable String displayName,
            @Nullable Integer port,
            @Nullable String tokenFile,
            @Nullable Integer protocolVersion) {
    }

    private BrowserExtensionProviderDiscovery() {
    }

    /// Returns the absolute path of the discovery directory for the current platform.
    public static Path discoveryDirectory() {
        if (OS.WINDOWS) {
            String appData = System.getenv("APPDATA");
            Path baseDir = StringUtil.isBlank(appData)
                           ? Path.of(System.getProperty("user.home"), "AppData", "Roaming")
                           : Path.of(appData);
            return baseDir.resolve("JabRef").resolve("fulltext-providers");
        }
        if (OS.OS_X) {
            return Path.of(System.getProperty("user.home"),
                    "Library", "Application Support", "JabRef", "fulltext-providers");
        }
        // Linux + other Unixy: honour $XDG_CONFIG_HOME, fall back to ~/.config.
        String xdg = System.getenv("XDG_CONFIG_HOME");
        Path baseDir = StringUtil.isBlank(xdg)
                       ? Path.of(System.getProperty("user.home"), ".config")
                       : Path.of(xdg);
        return baseDir.resolve("jabref").resolve("fulltext-providers");
    }

    /// Enumerates and parses every `*.json` file in the platform-default
    /// discovery directory. Malformed files are skipped with a warning;
    /// the enumeration never aborts.
    public static List<BrowserExtensionProvider> discover() {
        return discoverIn(discoveryDirectory());
    }

    /// Enumerates and parses every `*.json` file under the given directory.
    /// Exposed for tests that point at a temporary directory.
    public static List<BrowserExtensionProvider> discoverIn(Path directory) {
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        List<BrowserExtensionProvider> providers = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*.json")) {
            for (Path file : stream) {
                parseProvider(file).ifPresent(providers::add);
            }
        } catch (IOException e) {
            LOGGER.warn("Could not enumerate fulltext-provider discovery directory {}", directory, e);
            return List.of();
        }
        return List.copyOf(providers);
    }

    private static Optional<BrowserExtensionProvider> parseProvider(Path file) {
        DiscoveryFile parsed;
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            parsed = GSON.fromJson(reader, DiscoveryFile.class);
        } catch (IOException | JsonSyntaxException e) {
            LOGGER.warn("Could not parse fulltext-provider discovery file {}", file, e);
            return Optional.empty();
        }
        if (parsed == null) {
            LOGGER.warn("Empty fulltext-provider discovery file: {}", file);
            return Optional.empty();
        }

        if (StringUtil.isBlank(parsed.name())
                || StringUtil.isBlank(parsed.displayName())
                || parsed.port() == null
                || StringUtil.isBlank(parsed.tokenFile())
                || parsed.protocolVersion() == null) {
            LOGGER.warn("Skipping malformed fulltext-provider discovery file: {}", file);
            return Optional.empty();
        }
        if (parsed.protocolVersion() != SUPPORTED_PROTOCOL_VERSION) {
            LOGGER.warn("Skipping fulltext-provider {} declaring unsupported protocolVersion {}",
                    parsed.name(), parsed.protocolVersion());
            return Optional.empty();
        }
        int port = parsed.port();
        if (port <= 0 || port > 65_535) {
            LOGGER.warn("Skipping fulltext-provider {} declaring out-of-range port {}", parsed.name(), port);
            return Optional.empty();
        }
        Path tokenFile = Path.of(parsed.tokenFile());
        if (!tokenFile.isAbsolute()) {
            LOGGER.warn("Skipping fulltext-provider {} declaring non-absolute tokenFile {}", parsed.name(), parsed.tokenFile());
            return Optional.empty();
        }
        return Optional.of(new BrowserExtensionProvider(
                parsed.name(),
                parsed.displayName(),
                port,
                tokenFile,
                parsed.protocolVersion()));
    }
}
