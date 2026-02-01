package org.jabref.logic.importer.fetcher.citation.opencitations;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.fetcher.CrossRef;
import org.jabref.logic.importer.fetcher.citation.CitationFetcher;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.util.URLUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public class OpenCitationsFetcher implements CitationFetcher {
    public static final String FETCHER_NAME = "OpenCitations";

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenCitationsFetcher.class);
    private static final String API_BASE_URL = "https://api.opencitations.net/index/v2";
    private static final Gson GSON = new Gson();

    private final ImporterPreferences importerPreferences;
    private final CrossRef crossRefFetcher = new CrossRef();

    public OpenCitationsFetcher(ImporterPreferences importerPreferences) {
        this.importerPreferences = importerPreferences;
    }

    @Override
    public String getName() {
        return FETCHER_NAME;
    }

    private String getApiUrl(String endpoint, BibEntry entry) throws FetcherException {
        String doi = entry.getDOI()
                          .orElseThrow(() -> new FetcherException("Entry does not have a DOI"))
                          .asString();
        return API_BASE_URL + "/" + endpoint + "/doi:" + doi;
    }

    @Override
    public List<BibEntry> getReferences(BibEntry entry) throws FetcherException {
        return fetchCitationData(entry, "references", CitationItem::citedIdentifiers);
    }

    @Override
    public List<BibEntry> getCitations(BibEntry entry) throws FetcherException {
        return fetchCitationData(entry, "citations", CitationItem::citingIdentifiers);
    }

    private List<BibEntry> fetchCitationData(BibEntry entry, String endpoint, Function<CitationItem, List<CitationItem.IdentifierWithField>> identifierExtractor) throws FetcherException {
        if (entry.getDOI().isEmpty()) {
            return List.of();
        }

        String apiUrl = getApiUrl(endpoint, entry);
        LOGGER.debug("{} URL: {}", endpoint, apiUrl);

        try {
            URL url = URLUtil.create(apiUrl);
            URLDownload urlDownload = new URLDownload(importerPreferences, url);
            importerPreferences.getApiKey(getName())
                               .ifPresent(apiKey -> urlDownload.addHeader("authorization", apiKey));

            String jsonResponse = urlDownload.asString();
            CitationItem[] citationItems = GSON.fromJson(jsonResponse, CitationItem[].class);

            if (citationItems == null || citationItems.length == 0) {
                return List.of();
            }

            List<BibEntry> entries = new ArrayList<>();
            for (CitationItem item : citationItems) {
                List<CitationItem.IdentifierWithField> identifiers = identifierExtractor.apply(item);
                if (!identifiers.isEmpty()) {
                    entries.add(fetchBibEntryFromIdentifiers(identifiers));
                }
            }

            return entries;
        } catch (MalformedURLException e) {
            throw new FetcherException("Malformed URL", e);
        } catch (JsonSyntaxException e) {
            throw new FetcherException("Could not parse JSON response from OpenCitations", e);
        }
    }

    @Override
    public Optional<Integer> getCitationCount(BibEntry entry) throws FetcherException {
        if (entry.getDOI().isEmpty()) {
            return Optional.empty();
        }

        String apiUrl = getApiUrl("citation-count", entry);
        LOGGER.debug("Citation count URL: {}", apiUrl);

        try {
            URL url = URLUtil.create(apiUrl);
            URLDownload urlDownload = new URLDownload(importerPreferences, url);
            importerPreferences.getApiKey(getName())
                               .ifPresent(apiKey -> urlDownload.addHeader("authorization", apiKey));

            String jsonResponse = urlDownload.asString();
            CountResponse countResponse = GSON.fromJson(jsonResponse, CountResponse.class);

            if (countResponse == null) {
                return Optional.empty();
            }

            int count = countResponse.countAsInt();
            return count > 0 ? Optional.of(count) : Optional.empty();
        } catch (MalformedURLException e) {
            throw new FetcherException("Malformed URL", e);
        } catch (JsonSyntaxException e) {
            throw new FetcherException("Could not parse JSON response from OpenCitations", e);
        }
    }

    private BibEntry fetchBibEntryFromIdentifiers(List<CitationItem.IdentifierWithField> identifiers) {
        Optional<CitationItem.IdentifierWithField> doiIdentifier = identifiers.stream()
                .filter(id -> id.field().equals(StandardField.DOI))
                .findFirst();

        if (doiIdentifier.isPresent()) {
            try {
                Optional<BibEntry> fetchedEntry = crossRefFetcher.performSearchById(doiIdentifier.get().value());
                if (fetchedEntry.isPresent()) {
                    BibEntry entry = fetchedEntry.get();
                    for (CitationItem.IdentifierWithField identifier : identifiers) {
                        if (!entry.hasField(identifier.field())) {
                            entry.setField(identifier.field(), identifier.value());
                        }
                    }
                    return entry;
                }
            } catch (FetcherException e) {
                LOGGER.warn("Could not fetch BibEntry for DOI: {}", doiIdentifier.get().value(), e);
            }
        }

        BibEntry bibEntry = new BibEntry();
        for (CitationItem.IdentifierWithField identifier : identifiers) {
            bibEntry.setField(identifier.field(), identifier.value());
        }
        return bibEntry;
    }
}
