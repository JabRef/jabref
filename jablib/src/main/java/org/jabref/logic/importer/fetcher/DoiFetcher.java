package org.jabref.logic.importer.fetcher;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.regex.Pattern;

import org.jabref.logic.cleanup.FieldFormatterCleanup;
import org.jabref.logic.formatter.bibtexfields.ClearFormatter;
import org.jabref.logic.formatter.bibtexfields.HtmlToLatexFormatter;
import org.jabref.logic.formatter.bibtexfields.NormalizePagesFormatter;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.EntryBasedFetcher;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.importer.util.MediaTypes;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.util.URLUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.util.DummyDirectoryUpdateMonitor;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.model.util.OptionalUtil;

import com.google.common.util.concurrent.RateLimiter;
import kong.unirest.core.json.JSONArray;
import kong.unirest.core.json.JSONException;
import kong.unirest.core.json.JSONObject;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DoiFetcher implements IdBasedFetcher, EntryBasedFetcher {

    public static final String NAME = "DOI";

    private static final String APS_JOURNAL_ORG_DOI_ID = "1103";
    private static final String APS_SUFFIX = "([\\w]+\\.)([\\w]+\\.)([\\w]+)";
    private static final Pattern APS_SUFFIX_PATTERN = Pattern.compile(APS_SUFFIX);

    private static final Logger LOGGER = LoggerFactory.getLogger(DoiFetcher.class);

    // 1000 request per 5 minutes. See https://support.datacite.org/docs/is-there-a-rate-limit-for-making-requests-against-the-datacite-apis
    private static final RateLimiter DATA_CITE_DCN_RATE_LIMITER = RateLimiter.create(3.33);

    /*
     * By default, it seems that CrossRef DOI Content Negotiation responses are returned by their API pools, more specifically the public one
     * (by default). See https://www.crossref.org/documentation/retrieve-metadata/content-negotiation/
     * Experimentally, the rating applied to this pool is defined by response headers "X-Rate-Limit-Interval" and "X-Rate-Limit-Limit", which seems
     * to default to 50 request / second. However, because of its dynamic nature, this rate could change between API calls, so we need to update it
     * atomically when that happens (as multiple threads might access it at the same time)
     */
    private static final RateLimiter CROSSREF_DCN_RATE_LIMITER = RateLimiter.create(50.0);

    private static final FieldFormatterCleanup NORMALIZE_PAGES = new FieldFormatterCleanup(StandardField.PAGES, new NormalizePagesFormatter());
    private static final FieldFormatterCleanup CLEAR_URL = new FieldFormatterCleanup(StandardField.URL, new ClearFormatter());
    private static final FieldFormatterCleanup HTML_TO_LATEX_TITLE = new FieldFormatterCleanup(StandardField.TITLE, new HtmlToLatexFormatter());

    private final ImportFormatPreferences preferences;

    public DoiFetcher(ImportFormatPreferences preferences) {
        this.preferences = preferences;
    }

    @Override
    public String getName() {
        return DoiFetcher.NAME;
    }

    @Override
    public Optional<HelpFile> getHelpPage() {
        return Optional.of(HelpFile.FETCHER_DOI);
    }

    private void doAPILimiting(String identifier) {
        // Without a generic API Rate Limiter implemented on the project, use Guava's RateLimiter for avoiding
        // API throttling when multiple threads are working, specially during DOI Content Negotiations
        Optional<DOI> doi = DOI.parse(identifier);

        try {
            Optional<String> agency;
            if (doi.isPresent() && (agency = getAgency(doi.get())).isPresent()) {
                double waitingTime = 0.0;
                if ("datacite".equalsIgnoreCase(agency.get())) {
                    waitingTime = DATA_CITE_DCN_RATE_LIMITER.acquire();
                } else if ("crossref".equalsIgnoreCase(agency.get())) {
                    waitingTime = CROSSREF_DCN_RATE_LIMITER.acquire();
                } // mEDRA does not explicit an API rating

                LOGGER.trace("Thread {}, searching for DOI '{}', waited {} because of API rate limiter",
                        Thread.currentThread().threadId(), identifier, waitingTime);
            }
        } catch (FetcherException | MalformedURLException e) {
            LOGGER.warn("Could not limit DOI API access rate", e);
        }
    }

    protected CompletableFuture<Optional<BibEntry>> asyncPerformSearchById(String identifier) {
        doAPILimiting(identifier);
        return CompletableFuture.supplyAsync(() -> {
            try {
                return performSearchById(identifier);
            } catch (FetcherException e) {
                throw new CompletionException(e);
            }
        });
    }

    @Override
    public Optional<BibEntry> performSearchById(String identifier) throws FetcherException {
        DOI doi = DOI.parse(identifier)
                     .orElseThrow(() -> new FetcherException(Localization.lang("Invalid DOI: '%0'.", identifier)));

        URL doiURL;
        try {
            doiURL = URLUtil.create(doi.getURIAsASCIIString());
        } catch (MalformedURLException e) {
            throw new FetcherException("Malformed URL", e);
        }

        Optional<BibEntry> fetchedEntry;

        // mEDRA does not return a parsable bibtex string
        Optional<String> agency;
        try {
            agency = getAgency(doi);
        } catch (MalformedURLException e) {
            throw new FetcherException("Invalid URL", e);
        }
        if (agency.isPresent() && "medra".equalsIgnoreCase(agency.get())) {
            return new Medra().performSearchById(identifier);
        }

        URLDownload download = getUrlDownload(doiURL);
        download.addHeader("Accept", MediaTypes.APPLICATION_BIBTEX);
        HttpURLConnection connection = (HttpURLConnection) download.openConnection();
        InputStream inputStream = download.asInputStream(connection);

        BibtexParser bibtexParser = new BibtexParser(preferences, new DummyFileUpdateMonitor(), new DummyDirectoryUpdateMonitor());
        try {
            fetchedEntry = bibtexParser.parseEntries(inputStream).stream().findFirst();
        } catch (ParseException e) {
            throw new FetcherException(doiURL, "Could not parse BibTeX entry", e);
        }
        // Crossref has a dynamic API rate limit
        if (agency.isPresent() && "crossref".equalsIgnoreCase(agency.get())) {
            updateCrossrefAPIRate(connection);
        }
        connection.disconnect();

        fetchedEntry.ifPresent(entry -> {
            doPostCleanup(entry);

            // Output warnings in case of inconsistencies
            entry.getField(StandardField.DOI)
                 .filter(entryDoi -> entryDoi.equals(doi.asString()))
                 .ifPresent(entryDoi -> LOGGER.warn("Fetched entry's DOI {} is different from requested DOI {}", entryDoi, identifier));
            if (entry.getField(StandardField.DOI).isEmpty()) {
                LOGGER.warn("Fetched entry does not contain doi field {}", identifier);
            }

            if (isAPSJournal(entry, doi) && !entry.hasField(StandardField.PAGES)) {
                setPageNumbersBasedOnDoi(entry, doi);
            }
        });

        return fetchedEntry;
    }

    private void doPostCleanup(BibEntry entry) {
        NORMALIZE_PAGES.cleanup(entry);
        CLEAR_URL.cleanup(entry);
        HTML_TO_LATEX_TITLE.cleanup(entry);
        entry.trimLeft();
    }

    private synchronized void updateCrossrefAPIRate(URLConnection existingConnection) {
        try {
            // Assuming this field is given in seconds
            String xRateLimitInterval = existingConnection.getHeaderField("X-Rate-Limit-Interval").replaceAll("[^\\.0123456789]", "");
            String xRateLimit = existingConnection.getHeaderField("X-Rate-Limit-Limit");

            double newRate = Double.parseDouble(xRateLimit) / Double.parseDouble(xRateLimitInterval);
            double oldRate = CROSSREF_DCN_RATE_LIMITER.getRate();

            // In theory, the actual update might rarely happen...
            if (Math.abs(newRate - oldRate) >= 1.0) {
                LOGGER.info("Updated Crossref API rate limit from {} to {}", oldRate, newRate);
                CROSSREF_DCN_RATE_LIMITER.setRate(newRate);
            }
        } catch (NullPointerException | IllegalArgumentException e) {
            LOGGER.warn("Could not deduce Crossref API's rate limit from response header. API might have changed");
        }
    }

    @Override
    public List<BibEntry> performSearch(@NonNull BibEntry entry) throws FetcherException {
        Optional<String> doi = entry.getField(StandardField.DOI);
        if (doi.isPresent()) {
            return OptionalUtil.toList(performSearchById(doi.get()));
        } else {
            return List.of();
        }
    }

    /**
     * Returns registration agency. Optional.empty() if no agency is found.
     *
     * @param doi the DOI to be searched
     */
    public Optional<String> getAgency(DOI doi) throws FetcherException, MalformedURLException {
        Optional<String> agency = Optional.empty();
        try {
            URLDownload download = getUrlDownload(
                    URLUtil.create(DOI.AGENCY_RESOLVER + "/" + URLEncoder.encode(doi.asString(),
                            StandardCharsets.UTF_8)));
            JSONObject response = new JSONArray(download.asString()).getJSONObject(0);
            if (response != null) {
                agency = Optional.ofNullable(response.optString("RA"));
            }
        } catch (JSONException e) {
            LOGGER.error("Cannot parse agency fetcher response to JSON");
            return Optional.empty();
        }

        return agency;
    }

    private void setPageNumbersBasedOnDoi(BibEntry entry, DOI doi) {
        String doiAsString = doi.asString();
        String articleId = doiAsString.substring(doiAsString.lastIndexOf('.') + 1);
        entry.setField(StandardField.PAGES, articleId);
    }

    // checks if the entry is an APS journal by comparing the organization id and the suffix format
    private boolean isAPSJournal(BibEntry entry, DOI doi) {
        if (!entry.getType().equals(StandardEntryType.Article)) {
            return false;
        }
        String doiString = doi.asString();
        String suffix = doiString.substring(doiString.lastIndexOf('/') + 1);
        String organizationId = doiString.substring(doiString.indexOf('.') + 1, doiString.indexOf('/'));
        return APS_JOURNAL_ORG_DOI_ID.equals(organizationId) && APS_SUFFIX_PATTERN.matcher(suffix).matches();
    }
}
