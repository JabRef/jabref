package org.jabref.logic.importer;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.jabref.logic.importer.fetcher.DoiResolution;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.identifier.DOI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for trying to resolve URLs to full-text PDF for articles.
 */
public class FulltextFetchers {
    private static final Logger LOGGER = LoggerFactory.getLogger(FulltextFetchers.class);

    private final List<FulltextFetcher> finders = new ArrayList<>();

    public FulltextFetchers(ImportFormatPreferences importFormatPreferences) {
        this(WebFetchers.getFullTextFetchers(importFormatPreferences));
    }

    FulltextFetchers(List<FulltextFetcher> fetcher) {
        finders.addAll(fetcher);
    }

    public Optional<URL> findFullTextPDF(BibEntry entry) {
        // for accuracy, fetch DOI first but do not modify entry
        BibEntry clonedEntry = (BibEntry) entry.clone();
        Optional<DOI> doi = clonedEntry.getField(FieldName.DOI).flatMap(DOI::parse);

        if (!doi.isPresent()) {
            try {
                WebFetchers.getIdFetcherForIdentifier(DOI.class)
                        .findIdentifier(clonedEntry)
                        .ifPresent(e -> clonedEntry.setField(FieldName.DOI, e.getDOI()));
            } catch (FetcherException e) {
                LOGGER.debug("Failed to find DOI", e);
            }
        }

        ExecutorService executor = Executors.newCachedThreadPool();
        try {
            // First try publisher access
            FulltextFetcher authority = new DoiResolution();

            Optional<URL> pdfUrl = findFirst(executor, Arrays.asList(getCallable(clonedEntry, authority)));
            if (pdfUrl.isPresent()) {
                return pdfUrl;
            }
            // All other available fetchers
            pdfUrl = findFirst(executor, getCallables(clonedEntry, finders));
            if (pdfUrl.isPresent()) {
                return pdfUrl;
            }
        } finally {
            shutdownAndAwaitTermination(executor);
        }
        return Optional.empty();
    }

    private Callable<Optional<URL>> getCallable(BibEntry entry, FulltextFetcher fetcher) {
            return () -> {
                try {
                    Optional<URL> result = fetcher.findFullText(entry);

                    if (result.isPresent() && new URLDownload(result.get().toString()).isPdf()) {
                        return result;
                    }
                } catch (IOException | FetcherException e) {
                    LOGGER.debug("Failed to find fulltext PDF at given URL", e);
                }
                return Optional.empty();
            };
    }

    private List<Callable<Optional<URL>>> getCallables(BibEntry entry, List<FulltextFetcher> fetchers) {
        return fetchers.stream()
                .map(f -> getCallable(entry, f))
                .collect(Collectors.toList());
    }

    private Optional<URL> findFirst(ExecutorService executor, List<Callable<Optional<URL>>> solvers) {
        CompletionService<Optional<URL>> service = new ExecutorCompletionService<>(executor);
        List<Future<Optional<URL>>> futures = solvers.stream()
                .map(callable -> service.submit(callable))
                .collect(Collectors.toList());

        Optional<URL> result = Optional.empty();
        try {
            // check all fetcher results
            for (int i = 0; i < futures.size(); i++) {
                try {
                    Optional<URL> link = service.take().get();

                    if (link.isPresent()) {
                        result = link;
                        break;
                    }
                } catch (InterruptedException ignore) {

                } catch (ExecutionException e) {
                    LOGGER.warn("Error during fetcher execution: " + e.getMessage());
                }
            }
        } finally {
            futures.stream().forEach(f -> f.cancel(true));
        }

        return result;
    }

    private void shutdownAndAwaitTermination(ExecutorService pool) {
        // Disable new tasks from being submitted
        pool.shutdown();
        try {
            // Wait a while for existing tasks to terminate
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                // Cancel currently executing tasks
                pool.shutdownNow();
                // Wait a while for tasks to respond to being cancelled
                if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                    LOGGER.warn("Pool did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }
}
