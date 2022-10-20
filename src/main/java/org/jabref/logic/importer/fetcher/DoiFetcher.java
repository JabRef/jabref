package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.regex.Pattern;

import org.jabref.logic.cleanup.FieldFormatterCleanup;
import org.jabref.logic.formatter.bibtexfields.ClearFormatter;
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
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.model.util.OptionalUtil;

import com.google.common.util.concurrent.RateLimiter;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONException;
import kong.unirest.json.JSONObject;
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
    *  By default, it seems that CrossRef DOI Content Negotiation responses are returned by their API pools, more specifically the public one
    *  (by default). See https://www.crossref.org/documentation/retrieve-metadata/content-negotiation/
    *  Experimentally, the rating applied to this pool is defined by response headers "X-Rate-Limit-Interval" and "X-Rate-Limit-Limit", which seems
    *  to default to 50 request / second. However, because of its dynamic nature, this rate could change between API calls, so we need to update it
    *  atomically when that happens (as multiple threads might access it at the same time)
    * */
    private static final RateLimiter CROSSREF_DCN_RATE_LIMITER = RateLimiter.create(50.0);

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
                if (agency.get().equalsIgnoreCase("datacite")) {
                    waitingTime = DATA_CITE_DCN_RATE_LIMITER.acquire();
                } else if (agency.get().equalsIgnoreCase("crossref")) {
                    waitingTime = CROSSREF_DCN_RATE_LIMITER.acquire();
                } // mEDRA does not explicit an API rating

                LOGGER.trace(String.format("Thread %s, searching for DOI '%s', waited %.2fs because of API rate limiter",
                        Thread.currentThread().getId(), identifier, waitingTime));
            }
        } catch (IOException e) {
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
        Optional<DOI> doi = DOI.parse(identifier);

        try {
            if (doi.isPresent()) {
                Optional<BibEntry> fetchedEntry;

                // mEDRA does not return a parsable bibtex string
                Optional<String> agency;
                if ((agency = getAgency(doi.get())).isPresent() && "medra".equalsIgnoreCase(agency.get())) {
                    return new Medra().performSearchById(identifier);
                }
                URL doiURL = new URL(doi.get().getURIAsASCIIString());

                // BibTeX data
                URLDownload download = getUrlDownload(doiURL);
                download.addHeader("Accept", MediaTypes.APPLICATION_BIBTEX);

                String bibtexString;
                URLConnection openConnection;
                try {
                    openConnection = download.openConnection();
                    bibtexString = URLDownload.asString(openConnection);
                } catch (IOException e) {
                    // an IOException with a nested FetcherException will be thrown when you encounter a 400x or 500x http status code
                    if (e.getCause() instanceof FetcherException fe) {
                        throw fe;
                    }
                    throw e;
                }

                // BibTeX entry
                fetchedEntry = BibtexParser.singleFromString(bibtexString, preferences, new DummyFileUpdateMonitor());
                fetchedEntry.ifPresent(this::doPostCleanup);

                // Crossref has a dynamic API rate limit
                if (agency.isPresent() && agency.get().equalsIgnoreCase("crossref")) {
                    updateCrossrefAPIRate(openConnection);
                }

                // Check if the entry is an APS journal and add the article id as the page count if page field is missing
                if (fetchedEntry.isPresent() && fetchedEntry.get().hasField(StandardField.DOI)) {
                    BibEntry entry = fetchedEntry.get();
                    if (isAPSJournal(entry, entry.getField(StandardField.DOI).get()) && !entry.hasField(StandardField.PAGES)) {
                        setPageCountToArticleId(entry, entry.getField(StandardField.DOI).get());
                    }
                }

                if (openConnection instanceof HttpURLConnection) {
                    ((HttpURLConnection) openConnection).disconnect();
                }
                return fetchedEntry;
            } else {
                throw new FetcherException(Localization.lang("Invalid DOI: '%0'.", identifier));
            }
        } catch (IOException e) {
            throw new FetcherException(Localization.lang("Connection error"), e);
        } catch (ParseException e) {
            throw new FetcherException("Could not parse BibTeX entry", e);
        } catch (JSONException e) {
            throw new FetcherException("Could not retrieve Registration Agency", e);
        }
    }

    private void doPostCleanup(BibEntry entry) {
        new FieldFormatterCleanup(StandardField.PAGES, new NormalizePagesFormatter()).cleanup(entry);
        new FieldFormatterCleanup(StandardField.URL, new ClearFormatter()).cleanup(entry);
    }

    private void updateCrossrefAPIRate(URLConnection existingConnection) {
        try {
            // Assuming this field is given in seconds
            String xRateLimitInterval = existingConnection.getHeaderField("X-Rate-Limit-Interval").replaceAll("[^\\.0123456789]", "");
            String xRateLimit = existingConnection.getHeaderField("X-Rate-Limit-Limit");

            double newRate = Double.parseDouble(xRateLimit) / Double.parseDouble(xRateLimitInterval);
            double oldRate = CROSSREF_DCN_RATE_LIMITER.getRate();

            // In theory, the actual update might rarely happen...
            if (Math.abs(newRate - oldRate) >= 1.0) {
                LOGGER.info(String.format("Updated Crossref API rate limit from %.2f to %.2f", oldRate, newRate));
                CROSSREF_DCN_RATE_LIMITER.setRate(newRate);
            }
        } catch (NullPointerException | IllegalArgumentException e) {
            LOGGER.warn("Could not deduce Crossref API's rate limit from response header. API might have changed");
        }
    }

    @Override
    public List<BibEntry> performSearch(BibEntry entry) throws FetcherException {
        Optional<String> doi = entry.getField(StandardField.DOI);
        if (doi.isPresent()) {
            return OptionalUtil.toList(performSearchById(doi.get()));
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Returns registration agency. Optional.empty() if no agency is found.
     *
     * @param doi the DOI to be searched
     */
    public Optional<String> getAgency(DOI doi) throws IOException {
        Optional<String> agency = Optional.empty();
        try {
            URLDownload download = getUrlDownload(new URL(DOI.AGENCY_RESOLVER + "/" + doi.getDOI()));
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

    private void setPageCountToArticleId(BibEntry entry, String doiAsString) {
        String articleId = doiAsString.substring(doiAsString.lastIndexOf('.') + 1);
        entry.setField(StandardField.PAGES, articleId);
    }

    // checks if the entry is an APS journal by comparing the organization id and the suffix format
    private boolean isAPSJournal(BibEntry entry, String doiAsString) {
        if (!entry.getType().equals(StandardEntryType.Article)) {
            return false;
        }
        String suffix = doiAsString.substring(doiAsString.lastIndexOf('/') + 1);
        String organizationId = doiAsString.substring(doiAsString.indexOf('.') + 1, doiAsString.indexOf('/'));
        return organizationId.equals(APS_JOURNAL_ORG_DOI_ID) && APS_SUFFIX_PATTERN.matcher(suffix).matches();
    }
}
