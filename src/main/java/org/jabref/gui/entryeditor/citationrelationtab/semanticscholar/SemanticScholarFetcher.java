package org.jabref.gui.entryeditor.citationrelationtab.semanticscholar;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.fetcher.CustomizableKeyFetcher;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.util.BuildInfo;
import org.jabref.model.entry.BibEntry;

import com.google.gson.Gson;

public class SemanticScholarFetcher implements CitationFetcher, CustomizableKeyFetcher {
    private static final String SEMANTIC_SCHOLAR_API = "https://api.semanticscholar.org/graph/v1/";

    private static final String API_KEY = new BuildInfo().semanticScholarApiKey;

    private final ImporterPreferences importerPreferences;

    public SemanticScholarFetcher(ImporterPreferences importerPreferences) {
        this.importerPreferences = importerPreferences;
    }

    public String getAPIUrl(String entry_point, BibEntry entry) {
        return SEMANTIC_SCHOLAR_API + "paper/" + "DOI:" + entry.getDOI().orElseThrow().getDOI() + "/" + entry_point
                + "?fields=" + "title,authors,year,citationCount,referenceCount,externalIds,publicationTypes,abstract,url"
                + "&limit=1000";
    }

    @Override
    public List<BibEntry> searchCitedBy(BibEntry entry) throws FetcherException {
        if (!entry.getDOI().isPresent()) {
            return List.of();
        }

        URL citationsUrl;
        try {
            citationsUrl = URI.create(getAPIUrl("citations", entry)).toURL();
        } catch (MalformedURLException e) {
            throw new FetcherException("Malformed URL", e);
        }
        URLDownload urlDownload = new URLDownload(citationsUrl);

        String apiKey = getApiKey();
        if (!apiKey.isEmpty()) {
            urlDownload.addHeader("x-api-key", apiKey);
        }
        CitationsResponse citationsResponse = new Gson()
                .fromJson(urlDownload.asString(), CitationsResponse.class);

        return citationsResponse.getData()
                                .stream().filter(citationDataItem -> citationDataItem.getCitingPaper() != null)
                                .map(citationDataItem -> citationDataItem.getCitingPaper().toBibEntry()).toList();
    }

    @Override
    public List<BibEntry> searchCiting(BibEntry entry) throws FetcherException {
        if (!entry.getDOI().isPresent()) {
            return List.of();
        }

        URL referencesUrl;
        try {
            referencesUrl = URI.create(getAPIUrl("references", entry)).toURL();
        } catch (MalformedURLException e) {
            throw new FetcherException("Malformed URL", e);
        }

        URLDownload urlDownload = new URLDownload(referencesUrl);
        String apiKey = getApiKey();
        if (!apiKey.isEmpty()) {
            urlDownload.addHeader("x-api-key", apiKey);
        }
        ReferencesResponse referencesResponse = new Gson()
                .fromJson(urlDownload.asString(), ReferencesResponse.class);

        return referencesResponse.getData()
                                 .stream()
                                 .filter(citationDataItem -> citationDataItem.getCitedPaper() != null)
                                 .map(referenceDataItem -> referenceDataItem.getCitedPaper().toBibEntry()).toList();
    }

    @Override
    public String getName() {
        return "Semantic Scholar Citations Fetcher";
    }

    private String getApiKey() {
        return importerPreferences.getApiKey(getName()).orElse(API_KEY);
    }
}
