package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.cleanup.FieldFormatterCleanup;
import org.jabref.logic.formatter.bibtexfields.ClearFormatter;
import org.jabref.logic.formatter.bibtexfields.RemoveEnclosingBracesFormatter;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.EntryBasedFetcher;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.SearchBasedParserFetcher;
import org.jabref.logic.importer.fetcher.transformers.DefaultQueryTransformer;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.importer.util.MediaTypes;
import org.jabref.logic.layout.format.LatexToUnicodeFormatter;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.search.query.BaseQueryNode;

import org.apache.hc.core5.net.URIBuilder;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fetches data from the INSPIRE database.
 * 
 * Enhanced version with:
 * - Retry mechanism for network failures
 * - Better error handling and logging
 * - Validation of fetched data
 * - Optimized request headers
 */
public class INSPIREFetcher implements SearchBasedParserFetcher, EntryBasedFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(INSPIREFetcher.class);
    
    private static final String INSPIRE_HOST = "https://inspirehep.net/api/literature/";
    private static final String INSPIRE_DOI_HOST = "https://inspirehep.net/api/doi/";
    private static final String INSPIRE_ARXIV_HOST = "https://inspirehep.net/api/arxiv/";
    
    // Retry configuration
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000; // 1 second base delay
    
    // Timeout configuration (in milliseconds)
    private static final int CONNECT_TIMEOUT_MS = 10000; // 10 seconds
    private static final int READ_TIMEOUT_MS = 30000;    // 30 seconds
    
    private static final String ERROR_MESSAGE_TEMPLATE = 
        "Failed to fetch from INSPIRE using %s after %d attempts.\n" +
        "Possible causes:\n" +
        "- Network connection issue\n" +
        "- INSPIRE service temporarily unavailable\n" +
        "- Invalid identifier format\n" +
        "Please check your internet connection and try again.";

    private final ImportFormatPreferences importFormatPreferences;

    public INSPIREFetcher(ImportFormatPreferences preferences) {
        this.importFormatPreferences = preferences;
    }

    @Override
    public String getName() {
        return "INSPIRE";
    }

    @Override
    public Optional<HelpFile> getHelpPage() {
        return Optional.of(HelpFile.FETCHER_INSPIRE);
    }

    @Override
    public URL getURLForQuery(BaseQueryNode queryNode) throws URISyntaxException, MalformedURLException {
        URIBuilder uriBuilder = new URIBuilder(INSPIRE_HOST);
        uriBuilder.addParameter("q", new DefaultQueryTransformer().transformSearchQuery(queryNode).orElse(""));
        return uriBuilder.build().toURL();
    }

    @Override
    public URLDownload getUrlDownload(URL url) {
        URLDownload download = new URLDownload(url);
        
        // Set comprehensive headers
        download.addHeader("Accept", MediaTypes.APPLICATION_BIBTEX);
        download.addHeader("User-Agent", "JabRef/" + getClass().getPackage().getImplementationVersion());
        
        // Set timeouts to prevent hanging
        download.setConnectTimeout(CONNECT_TIMEOUT_MS);
        download.setReadTimeout(READ_TIMEOUT_MS);
        
        return download;
    }

    @Override
    public void doPostCleanup(BibEntry entry) {
        // Remove strange "SLACcitation" field
        new FieldFormatterCleanup(new UnknownField("SLACcitation"), new ClearFormatter()).cleanup(entry);

        // Remove braces around content of "title" field
        new FieldFormatterCleanup(StandardField.TITLE, new RemoveEnclosingBracesFormatter()).cleanup(entry);

        new FieldFormatterCleanup(StandardField.TITLE, new LatexToUnicodeFormatter()).cleanup(entry);
        
        // Log the citation key for debugging
        if (LOGGER.isDebugEnabled()) {
            entry.getCitationKey().ifPresent(key -> 
                LOGGER.debug("Post-cleanup citation key: {}", key)
            );
        }
    }

    @Override
    public Parser getParser() {
        return new BibtexParser(importFormatPreferences);
    }

    @Override
    public List<BibEntry> performSearch(@NonNull BibEntry entry) throws FetcherException {
        Optional<String> doi = entry.getField(StandardField.DOI);
        Optional<String> archiveprefix = entry.getFieldOrAlias(StandardField.ARCHIVEPREFIX);
        Optional<String> eprint = entry.getField(StandardField.EPRINT);

        String urlString;
        String identifier;
        
        // Prioritize arXiv (INSPIRE has best support for arXiv identifiers)
        if (archiveprefix.filter("arxiv"::equalsIgnoreCase).isPresent() && eprint.isPresent()) {
            urlString = INSPIRE_ARXIV_HOST + eprint.get();
            identifier = "arXiv:" + eprint.get();
            LOGGER.debug("Using INSPIRE arXiv endpoint for: {}", identifier);
        } else if (doi.isPresent()) {
            urlString = INSPIRE_DOI_HOST + doi.get();
            identifier = "DOI:" + doi.get();
            LOGGER.debug("Using INSPIRE DOI endpoint for: {}", identifier);
        } else {
            LOGGER.debug("No suitable identifier found for INSPIRE search");
            return List.of();
        }

        URL url;
        try {
            url = new URI(urlString).toURL();
        } catch (MalformedURLException | URISyntaxException e) {
            throw new FetcherException("Invalid INSPIRE URL: " + urlString, e);
        }

        // Use retry mechanism for robust fetching
        List<BibEntry> results = performSearchWithRetry(url, identifier);
        
        // Validate and log results
        validateResults(results, identifier);
        
        return results;
    }
    
    /**
     * Performs the search with automatic retry on failure.
     * Implements exponential backoff for retries.
     * 
     * @param url The URL to fetch from
     * @param identifier Human-readable identifier for logging
     * @return List of fetched BibEntry objects
     * @throws FetcherException if all retry attempts fail
     */
    private List<BibEntry> performSearchWithRetry(URL url, String identifier) throws FetcherException {
        int attempt = 0;
        FetcherException lastException = null;
        
        while (attempt < MAX_RETRIES) {
            try {
                LOGGER.info("Fetching from INSPIRE (attempt {}/{}): {} [{}]", 
                           attempt + 1, MAX_RETRIES, url, identifier);
                
                URLDownload download = getUrlDownload(url);
                List<BibEntry> results = getParser().parseEntries(download.asInputStream());
                
                // Log success
                if (results.isEmpty()) {
                    LOGGER.warn("INSPIRE returned empty results for: {} [{}]", url, identifier);
                } else {
                    LOGGER.info("Successfully fetched {} entries from INSPIRE for [{}]", 
                               results.size(), identifier);
                }
                
                // Apply post-processing
                results.forEach(this::doPostCleanup);
                return results;
                
            } catch (ParseException | IOException e) {
                lastException = new FetcherException(url, 
                    "Failed to fetch from INSPIRE (attempt " + (attempt + 1) + "): " + e.getMessage(), e);
                
                LOGGER.warn("Fetch attempt {} failed for [{}]: {}", 
                           attempt + 1, identifier, e.getMessage());
                
                attempt++;
                
                // Implement exponential backoff for retries
                if (attempt < MAX_RETRIES) {
                    long delay = RETRY_DELAY_MS * (long) Math.pow(2, attempt - 1);
                    LOGGER.info("Retrying in {} ms...", delay);
                    
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new FetcherException("Interrupted during retry for [" + identifier + "]", ie);
                    }
                }
            }
        }
        
        // All retries failed
        throw new FetcherException(
            String.format(ERROR_MESSAGE_TEMPLATE, identifier, MAX_RETRIES), 
            lastException
        );
    }
    
    /**
     * Validates the fetched results and logs warnings for potential issues.
     * This helps identify when INSPIRE returns data but without proper texkeys.
     * 
     * @param results The list of fetched entries
     * @param identifier The identifier used for fetching
     */
    private void validateResults(List<BibEntry> results, String identifier) {
        if (results.isEmpty()) {
            return;
        }
        
        for (BibEntry entry : results) {
            // Check for citation key
            if (!entry.hasCitationKey()) {
                LOGGER.warn("Entry from INSPIRE [{}] has no citation key - may need fallback generation", 
                           identifier);
            } else {
                String citationKey = entry.getCitationKey().orElse("");
                
                // Check for problematic citation keys (URLs, DOIs, etc.)
                if (citationKey.startsWith("http") || 
                    citationKey.startsWith("https") ||
                    citationKey.startsWith("doi:") ||
                    citationKey.contains("://")) {
                    
                    LOGGER.warn("Entry has URL-like citation key: '{}' [{}] - cleanup may be needed", 
                               citationKey, identifier);
                } else if (citationKey.length() > 100) {
                    LOGGER.warn("Entry has unusually long citation key ({} chars) [{}] - cleanup may be needed", 
                               citationKey.length(), identifier);
                } else {
                    LOGGER.info("Got valid citation key: '{}' [{}]", citationKey, identifier);
                }
            }
            
            // Check for required fields
            if (entry.getField(StandardField.TITLE).isEmpty()) {
                LOGGER.warn("Entry from INSPIRE [{}] has no title", identifier);
            }
            
            if (entry.getField(StandardField.AUTHOR).isEmpty()) {
                LOGGER.warn("Entry from INSPIRE [{}] has no author", identifier);
            }
            
            // Log whether journal information is present (helps verify we got published version)
            boolean hasJournalInfo = entry.getField(StandardField.JOURNAL).isPresent() ||
                                    entry.getField(StandardField.JOURNALTITLE).isPresent();
            if (hasJournalInfo) {
                LOGGER.debug("Entry [{}] includes journal publication info", identifier);
            } else {
                LOGGER.debug("Entry [{}] has no journal info (may be preprint only)", identifier);
            }
        }
    }
}

