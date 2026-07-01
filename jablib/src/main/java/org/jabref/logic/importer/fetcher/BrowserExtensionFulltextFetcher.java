package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import org.jabref.logic.importer.FulltextFetcher;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Vendor-neutral fulltext fetcher that delegates PDF retrieval to a
/// locally-running browser-extension companion. The companion uses the
/// user's already-authenticated browser session to obtain a PDF that
/// JabRef cannot reach directly (paywall, anti-bot, 418, institutional
/// SSO).
///
/// At fetch time this class:
///   1. reads the entry's DOI (and URL, if any),
///   2. enumerates providers from the well-known discovery directory,
///   3. races all enabled providers in parallel,
///   4. returns the first `200` response's local file path as a `file://`
///      URL, so JabRef's existing attach pipeline performs the
///      move-and-rename step.
///
/// Other responses (404, 503, network errors, timeouts) are treated as a
/// soft miss — there is no retry and no polling. See
/// `docs/requirements/browser-extension-fulltext.md` for the wire spec.
@NullMarked
public class BrowserExtensionFulltextFetcher implements FulltextFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(BrowserExtensionFulltextFetcher.class);
    private static final Gson GSON = new Gson();
    private static final Duration DEFAULT_SOCKET_TIMEOUT = Duration.ofMinutes(5);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);

    private final Supplier<List<BrowserExtensionProvider>> providerSupplier;
    private final Duration socketTimeout;

    public BrowserExtensionFulltextFetcher() {
        this(BrowserExtensionProviderDiscovery::discover, DEFAULT_SOCKET_TIMEOUT);
    }

    /// Test-visible constructor. Allows injecting a provider list (for unit
    /// tests against an embedded HTTP server) and a short socket timeout.
    BrowserExtensionFulltextFetcher(Supplier<List<BrowserExtensionProvider>> providerSupplier,
                                    Duration socketTimeout) {
        this.providerSupplier = providerSupplier;
        this.socketTimeout = socketTimeout;
    }

    @Override
    public Optional<URL> findFullText(BibEntry entry) throws IOException {
        Optional<String> doi = entry.getField(StandardField.DOI).filter(s -> !StringUtil.isBlank(s));
        Optional<String> url = entry.getField(StandardField.URL).filter(s -> !StringUtil.isBlank(s));
        if (doi.isEmpty() && url.isEmpty()) {
            return Optional.empty();
        }

        List<BrowserExtensionProvider> providers = providerSupplier.get();
        if (providers.isEmpty()) {
            return Optional.empty();
        }

        String requestBody = buildRequestBody(doi, url);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            return raceProviders(providers, requestBody, executor);
        }
    }

    @Override
    public TrustLevel getTrustLevel() {
        return TrustLevel.PUBLISHER;
    }

    private Optional<URL> raceProviders(List<BrowserExtensionProvider> providers,
                                        String requestBody,
                                        ExecutorService executor) {
        CompletableFuture<Optional<URL>> winner = new CompletableFuture<>();
        List<Future<?>> tasks = new ArrayList<>(providers.size());

        for (BrowserExtensionProvider provider : providers) {
            tasks.add(executor.submit(() -> {
                if (winner.isDone()) {
                    return;
                }
                tryProvider(provider, requestBody, executor)
                        .ifPresent(url -> winner.complete(Optional.of(url)));
            }));
        }

        // Once a winner is chosen, cancel the losers. Future#cancel(true)
        // interrupts the blocking HttpClient#send in tryProvider, which
        // together with the try-with-resources on HttpClient closes the
        // underlying socket — the cancellation-via-connection-close signal
        // the wire spec expects (req~bxf.cancellation~1).
        winner.thenRun(() -> tasks.forEach(f -> f.cancel(true)));

        // Fallback: if every task finishes without a hit, resolve empty.
        CompletableFuture.runAsync(() -> {
            for (Future<?> task : tasks) {
                try {
                    task.get();
                } catch (CancellationException | ExecutionException ignored) {
                    // Losers get cancelled once winner completes — expected.
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            winner.complete(Optional.empty());
        }, executor);

        try {
            return winner.get(socketTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (TimeoutException e) {
            LOGGER.debug("Browser-extension fulltext race timed out after {}", socketTimeout);
            return Optional.empty();
        } catch (ExecutionException e) {
            LOGGER.debug("Browser-extension fulltext race failed", e);
            return Optional.empty();
        } finally {
            // Belt-and-braces: if we exit via timeout or interrupt without a
            // winner, make sure the in-flight tasks unwind too.
            tasks.forEach(f -> f.cancel(true));
        }
    }

    private Optional<URL> tryProvider(BrowserExtensionProvider provider,
                                      String requestBody,
                                      ExecutorService executor) {
        @Nullable String token = readToken(provider);
        if (token == null) {
            return Optional.empty();
        }

        URI endpoint;
        try {
            endpoint = new URI("http", null, "127.0.0.1", provider.port(), "/v1/fulltext", null, null);
        } catch (URISyntaxException e) {
            LOGGER.debug("Could not build endpoint URI for provider {}", provider.name(), e);
            return Optional.empty();
        }
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                                         .timeout(socketTimeout)
                                         .header("Content-Type", "application/json")
                                         .header("Authorization", "Bearer " + token)
                                         .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                                         .build();

        try (HttpClient client = HttpClient.newBuilder()
                                           .connectTimeout(CONNECT_TIMEOUT)
                                           .executor(executor)
                                           .build()) {
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                LOGGER.debug("Provider {} returned HTTP {}: {}", provider.name(), response.statusCode(), response.body());
                return Optional.empty();
            }
            return parseSuccessResponse(provider, response.body());
        } catch (IOException e) {
            LOGGER.debug("Provider {} unreachable or returned malformed response", provider.name(), e);
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    private Optional<URL> parseSuccessResponse(BrowserExtensionProvider provider, String body) {
        FulltextResponse parsed;
        try {
            parsed = GSON.fromJson(body, FulltextResponse.class);
        } catch (JsonSyntaxException e) {
            LOGGER.debug("Provider {} returned malformed JSON: {}", provider.name(), body, e);
            return Optional.empty();
        }
        if (parsed == null || StringUtil.isBlank(parsed.path())) {
            LOGGER.debug("Provider {} returned 200 without a path field: {}", provider.name(), body);
            return Optional.empty();
        }
        Path filePath = Path.of(parsed.path());
        if (!Files.isReadable(filePath)) {
            LOGGER.debug("Provider {} returned a path that is not readable: {}", provider.name(), parsed.path());
            return Optional.empty();
        }
        try {
            return Optional.of(filePath.toUri().toURL());
        } catch (MalformedURLException e) {
            LOGGER.debug("Provider {} returned an unrepresentable file path: {}", provider.name(), parsed.path(), e);
            return Optional.empty();
        }
    }

    private static @Nullable String readToken(BrowserExtensionProvider provider) {
        try {
            String token = Files.readString(provider.tokenFile(), StandardCharsets.UTF_8).strip();
            if (StringUtil.isBlank(token)) {
                LOGGER.debug("Token file for provider {} is empty: {}", provider.name(), provider.tokenFile());
                return null;
            }
            return token;
        } catch (IOException e) {
            LOGGER.debug("Could not read token file for provider {}: {}", provider.name(), provider.tokenFile(), e);
            return null;
        }
    }

    private static String buildRequestBody(Optional<String> doi, Optional<String> url) {
        Map<String, String> body = new LinkedHashMap<>();
        doi.ifPresent(v -> body.put("doi", v));
        url.ifPresent(v -> body.put("url", v));
        return GSON.toJson(body);
    }

    /// Wire-format success response body. Fields not relevant to the
    /// fetcher (such as `sourceUrl`) are present for forward compatibility
    /// but ignored.
    private record FulltextResponse(
            @Nullable String id,
            @Nullable String path,
            @Nullable String sourceUrl) {
    }
}
