package org.jabref.logic.importer.fetcher.citation.semanticscholar;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.fetcher.CustomizableKeyFetcher;
import org.jabref.logic.importer.fetcher.citation.CitationFetcher;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.util.URLUtil;
import org.jabref.model.entry.BibEntry;

import com.google.gson.Gson;
import org.jspecify.annotations.NonNull;

public class SemanticScholarCitationFetcher implements CitationFetcher, CustomizableKeyFetcher {
    public static final String FETCHER_NAME = "Semantic Scholar Citations Fetcher";
    private static final String SEMANTIC_SCHOLAR_API = "https://api.semanticscholar.org/graph/v1/";

    private static final Gson GSON = new Gson();

    private final ImporterPreferences importerPreferences;

    public SemanticScholarCitationFetcher(ImporterPreferences importerPreferences) {
        this.importerPreferences = importerPreferences;
    }

    public String getAPIUrl(String entry_point, BibEntry entry) {
        return SEMANTIC_SCHOLAR_API + "paper/" + "DOI:" + entry.getDOI().orElseThrow().asString() + "/" + entry_point
                + "?fields=" + "title,authors,year,citationCount,referenceCount,externalIds,publicationTypes,abstract,url"
                + "&limit=1000";
    }

    @Override
    public List<BibEntry> searchCitedBy(BibEntry entry) throws FetcherException {
        if (entry.getDOI().isEmpty()) {
            return List.of();
        }

        URL citationsUrl;
        try {
            citationsUrl = URLUtil.create(getAPIUrl("citations", entry));
        } catch (MalformedURLException e) {
            throw new FetcherException("Malformed URL", e);
        }
        URLDownload urlDownload = new URLDownload(citationsUrl);

        importerPreferences.getApiKey(getName()).ifPresent(apiKey -> urlDownload.addHeader("x-api-key", apiKey));

        CitationsResponse citationsResponse = GSON
                .fromJson(urlDownload.asString(), CitationsResponse.class);

        return citationsResponse.getData()
                                .stream().filter(citationDataItem -> citationDataItem.getCitingPaper() != null)
                                .map(citationDataItem -> citationDataItem.getCitingPaper().toBibEntry()).toList();
    }

    @Override
    public @NonNull List<BibEntry> searchCiting(@NonNull BibEntry entry) throws FetcherException {
        if (entry.getDOI().isEmpty()) {
            return List.of();
        }

        URL referencesUrl;
        try {
            referencesUrl = URLUtil.create(getAPIUrl("references", entry));
        } catch (MalformedURLException e) {
            throw new FetcherException("Malformed URL", e);
        }

        URLDownload urlDownload = new URLDownload(referencesUrl);
        importerPreferences.getApiKey(getName()).ifPresent(apiKey -> urlDownload.addHeader("x-api-key", apiKey));
        ReferencesResponse referencesResponse = GSON.fromJson(urlDownload.asString(), ReferencesResponse.class);

        if (referencesResponse.getData() == null) {
            return List.of();
        }

        return referencesResponse.getData()
                                 .stream()
                                 .filter(citationDataItem -> citationDataItem.getCitedPaper() != null)
                                 .map(referenceDataItem -> referenceDataItem.getCitedPaper().toBibEntry()).toList();
    }

    @Override
    public String getName() {
        return FETCHER_NAME;
    }
}
