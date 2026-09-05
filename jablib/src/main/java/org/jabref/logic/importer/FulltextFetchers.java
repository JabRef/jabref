package org.jabref.logic.importer;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;

import org.jabref.logic.net.URLDownload;
import org.jabref.logic.util.HeadlessExecutorService;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Utility class for trying to resolve URLs to full-text PDF for articles.
///
/// Combines multiple [FulltextFetcher]s together. Each fetcher is invoked, the "best" result (sorted by the fetcher trust level) is returned.
public class FulltextFetchers {
    private static final Logger LOGGER = LoggerFactory.getLogger(FulltextFetchers.class);

    // Timeout in seconds for the regular (direct HTTP) fetchers. Set generously so
    // fetchers that bounce through an institutional SSO chain or a slow publisher CDN
    // have a chance to complete.
    private static final int FETCHER_TIMEOUT = 120;

    // Timeout for the fallback fetchers. The browser-extension companion opens a tab,
    // navigates through SSO, and downloads the PDF; it holds the connection for its own
    // socket timeout (5 minutes). The outer race must not cancel it earlier, otherwise a
    // slow authenticated browser flow always returns as a miss.
    private static final int FALLBACK_FETCHER_TIMEOUT = 330;

    // How long the direct (non-fallback) fetchers run before the fallback fetcher is also
    // launched. Short, so a source only the browser extension can serve (e.g. IEEE) does not
    // first wait out the slowest direct fetcher's full timeout. See ADR-0072.
    private static final Duration DEFAULT_HEAD_START = Duration.ofSeconds(4);

    private final Set<FulltextFetcher> fetchers;
    private final ImporterPreferences importerPreferences;
    private final Duration headStart;

    private final BiPredicate<String, Map<String, String>> isPDF = (url, headers) -> {
        // Local file:// URLs (returned e.g. by a browser-extension companion
        // fetcher that already wrote the PDF to disk) cannot go through
        // URLDownload, which is HTTP-oriented. Verify the file is actually a
        // PDF by reading the magic bytes.
        if (url.startsWith("file:")) {
            try {
                Path path = Path.of(new java.net.URI(url));
                if (!Files.isReadable(path)) {
                    return false;
                }
                try (java.io.InputStream in = Files.newInputStream(path)) {
                    byte[] magic = in.readNBytes(5);
                    return magic.length == 5
                            && magic[0] == '%'
                            && magic[1] == 'P'
                            && magic[2] == 'D'
                            && magic[3] == 'F'
                            && magic[4] == '-';
                }
            } catch (java.net.URISyntaxException | IllegalArgumentException | IOException e) {
                LOGGER.debug("Could not verify PDF magic bytes for {}", url, e);
                return false;
            }
        }
        try {
            URLDownload download = new URLDownload(url);
            headers.forEach(download::addHeader);
            return download.isPdf();
        } catch (MalformedURLException e) {
            LOGGER.warn("URL returned by fulltext fetcher is invalid");
        }
        return false;
    };

    public FulltextFetchers(ImportFormatPreferences importFormatPreferences, ImporterPreferences importerPreferences) {
        this(WebFetchers.getFullTextFetchers(importFormatPreferences, importerPreferences), importerPreferences, DEFAULT_HEAD_START);
    }

    @VisibleForTesting
    FulltextFetchers(Set<FulltextFetcher> fetchers) {
        this(fetchers, ImporterPreferences.getDefault(), DEFAULT_HEAD_START);
    }

    @VisibleForTesting
    FulltextFetchers(Set<FulltextFetcher> fetchers, Duration headStart) {
        this(fetchers, ImporterPreferences.getDefault(), headStart);
    }

    private FulltextFetchers(Set<FulltextFetcher> fetchers, ImporterPreferences importerPreferences, Duration headStart) {
        this.fetchers = new HashSet<>(fetchers);
        this.importerPreferences = importerPreferences;
        this.headStart = headStart;
    }

    public Optional<FetcherResult> findFullTextPDF(BibEntry entry) {
        // for accuracy, fetch DOI first but do not modify entry
        BibEntry clonedEntry = new BibEntry(entry);
        Optional<DOI> doi = clonedEntry.getField(StandardField.DOI).flatMap(DOI::parse);

        if (doi.isEmpty()) {
            findDoiForEntry(clonedEntry);
        }

        // Split direct fetchers from fallback fetchers. Direct (HTTP) fetchers are cheap;
        // fallback fetchers (e.g. the browser-extension companion, which opens a browser tab)
        // are consulted only after a head start, so the browser session is reserved for PDFs
        // JabRef cannot download directly. See ADR-0072.
        Set<FulltextFetcher> primaryFetchers = new HashSet<>();
        Set<FulltextFetcher> fallbackFetchers = new HashSet<>();
        for (FulltextFetcher fetcher : fetchers) {
            if (fetcher instanceof FallbackFulltextFetcher) {
                fallbackFetchers.add(fetcher);
            } else {
                primaryFetchers.add(fetcher);
            }
        }

        if (fallbackFetchers.isEmpty()) {
            // Common case (no browser-extension provider registered): race the direct fetchers.
            return race(clonedEntry, primaryFetchers, FETCHER_TIMEOUT);
        }
        return raceWithHeadStart(clonedEntry, primaryFetchers, fallbackFetchers);
    }

    /// Runs the given fetchers in parallel and returns the result of the most trusted fetcher, if any.
    private Optional<FetcherResult> race(BibEntry entry, Set<FulltextFetcher> fetchersToRace, int timeoutSeconds) {
        if (fetchersToRace.isEmpty()) {
            return Optional.empty();
        }
        List<Future<Optional<FetcherResult>>> result = HeadlessExecutorService.INSTANCE.executeAll(getCallables(entry, fetchersToRace), timeoutSeconds, TimeUnit.SECONDS);

        return best(result.stream()
                          .map(FulltextFetchers::getResults)
                          .filter(Optional::isPresent)
                          .map(Optional::get)
                          .toList());
    }

    /// Races the direct fetchers and, only after a head start with no result, the fallback fetchers too.
    ///
    /// The direct fetchers get [#headStart] to answer. If one does, its result is returned and the
    /// fallback fetchers (which open a browser tab) never run. Otherwise the fallback fetchers are
    /// launched alongside the still-running direct fetchers and the first usable result wins; the
    /// losers are cancelled — closing the connection aborts the extension's tab (`req~bxf.cancellation~1`).
    private Optional<FetcherResult> raceWithHeadStart(BibEntry entry, Set<FulltextFetcher> primary, Set<FulltextFetcher> fallback) {
        BlockingQueue<Optional<FetcherResult>> completed = new LinkedBlockingQueue<>();
        List<Future<Optional<FetcherResult>>> futures = new ArrayList<>();
        List<FetcherResult> results = new ArrayList<>();
        try {
            int outstanding = submitInto(entry, primary, completed, futures);

            // Phase 1: head start for the direct fetchers; collect whatever answers in that window.
            long headStartEnd = System.nanoTime() + headStart.toNanos();
            while (outstanding > 0) {
                long waitNanos = headStartEnd - System.nanoTime();
                if (waitNanos <= 0) {
                    break;
                }
                Optional<FetcherResult> reported = completed.poll(waitNanos, TimeUnit.NANOSECONDS);
                if (reported == null) {
                    break;
                }
                outstanding--;
                reported.ifPresent(results::add);
            }
            if (!results.isEmpty()) {
                // A direct fetcher answered within the head start; the fallback never runs.
                return best(results);
            }

            // Phase 2: no direct result yet. Launch the fallback fetchers and race everything;
            // the first usable result wins.
            outstanding += submitInto(entry, fallback, completed, futures);
            long overallEnd = System.nanoTime() + TimeUnit.SECONDS.toNanos(FALLBACK_FETCHER_TIMEOUT);
            while (outstanding > 0 && results.isEmpty()) {
                long waitNanos = overallEnd - System.nanoTime();
                if (waitNanos <= 0) {
                    break;
                }
                Optional<FetcherResult> reported = completed.poll(waitNanos, TimeUnit.NANOSECONDS);
                if (reported == null) {
                    break;
                }
                outstanding--;
                reported.ifPresent(results::add);
            }
            // Prefer the most-trusted among any results that completed together.
            Optional<FetcherResult> more;
            while ((more = completed.poll()) != null) {
                more.ifPresent(results::add);
            }
            return best(results);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } finally {
            futures.forEach(future -> future.cancel(true));
        }
    }

    /// Submits each fetcher as a task that places its result on `completed`; returns the count submitted.
    private int submitInto(BibEntry entry, Set<FulltextFetcher> group,
                           BlockingQueue<Optional<FetcherResult>> completed,
                           List<Future<Optional<FetcherResult>>> futures) {
        for (FulltextFetcher fetcher : group) {
            Callable<Optional<FetcherResult>> task = getCallable(entry, fetcher);
            futures.add(HeadlessExecutorService.INSTANCE.execute(() -> {
                Optional<FetcherResult> result;
                try {
                    result = task.call();
                } catch (Exception e) {
                    LOGGER.debug("Fulltext fetcher failed", e);
                    result = Optional.empty();
                }
                completed.add(result);
                return result;
            }));
        }
        return group.size();
    }

    private static Optional<FetcherResult> best(Collection<FetcherResult> results) {
        return results.stream()
                      .filter(res -> res.source() != null)
                      .max(Comparator.comparingInt((FetcherResult res) -> res.trust().getTrustScore()));
    }

    private void findDoiForEntry(BibEntry clonedEntry) {
        try {
            WebFetchers.getIdFetcherForIdentifier(DOI.class, importerPreferences)
                       .findIdentifier(clonedEntry)
                       .ifPresent(e -> clonedEntry.setField(StandardField.DOI, e.asString()));
        } catch (FetcherException e) {
            LOGGER.debug("Failed to find DOI", e);
        }
    }

    private static Optional<FetcherResult> getResults(Future<Optional<FetcherResult>> future) {
        try {
            return future.get();
        } catch (InterruptedException ignore) {
            // ignore thread interruptions
        } catch (ExecutionException | CancellationException e) {
            LOGGER.debug("Fetcher execution failed or was cancelled");
        }
        return Optional.empty();
    }

    private Callable<Optional<FetcherResult>> getCallable(BibEntry entry, FulltextFetcher fetcher) {
        return () -> {
            try {
                Map<String, String> headers = fetcher.getDownloadHeaders();
                return fetcher.findFullText(entry)
                              .filter(url -> isAllowedScheme(fetcher, url))
                              .filter(url -> isPDF.test(url.toString(), headers))
                              .map(url -> new FetcherResult(fetcher.getTrustLevel(), url, headers));
            } catch (IOException | FetcherException e) {
                LOGGER.debug("Failed to find fulltext PDF at given URL", e);
            }
            return Optional.empty();
        };
    }

    /// Rejects local `file:` URLs unless the fetcher is explicitly trusted via [FileSchemeFulltextFetcher].
    /// A `file:` result triggers a local file read and the GUI move/attach pipeline, so an untrusted
    /// fetcher (e.g. one parsing remote HTML) must not be able to point JabRef at an arbitrary file.
    private static boolean isAllowedScheme(FulltextFetcher fetcher, URL url) {
        if ("file".equalsIgnoreCase(url.getProtocol()) && !(fetcher instanceof FileSchemeFulltextFetcher)) {
            LOGGER.warn("Rejecting file: URL from fetcher {} that is not trusted for local files", fetcher.getClass().getSimpleName());
            return false;
        }
        return true;
    }

    private List<Callable<Optional<FetcherResult>>> getCallables(BibEntry entry, Set<FulltextFetcher> fetchers) {
        return fetchers.stream()
                       .map(f -> getCallable(entry, f))
                       .toList();
    }
}
