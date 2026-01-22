package org.jabref.logic.importer.fetcher.citation.semanticscholar;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.fetcher.CustomizableKeyFetcher;
import org.jabref.logic.importer.fetcher.citation.CitationFetcher;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.util.URLUtil;
import org.jabref.model.entry.BibEntry;

import com.google.gson.Gson;
import kong.unirest.core.json.JSONObject;
import org.jooq.lambda.Unchecked;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SemanticScholarCitationFetcher implements CitationFetcher, CustomizableKeyFetcher {
    public static final String FETCHER_NAME = "Semantic Scholar Citations Fetcher";

    private static final Logger LOGGER = LoggerFactory.getLogger(SemanticScholarCitationFetcher.class);

    private static final String SEMANTIC_SCHOLAR_API = "https://api.semanticscholar.org/graph/v1/";

    private static final Gson GSON = new Gson();

    private final ImporterPreferences importerPreferences;

    public SemanticScholarCitationFetcher(ImporterPreferences importerPreferences) {
        this.importerPreferences = importerPreferences;
    }

    public String getAPIUrl(String entryPoint, BibEntry entry) {
        return SEMANTIC_SCHOLAR_API + "paper/" + "DOI:" + entry.getDOI().orElseThrow().asString() + "/" + entryPoint
                + "?fields=" + "title,authors,year,citationCount,referenceCount,externalIds,publicationTypes,abstract,url"
                + "&limit=1000";
    }

    public @NonNull String getUrlForCitationCount(@NonNull BibEntry entry) {
        return SEMANTIC_SCHOLAR_API + "paper/" + "DOI:" + entry.getDOI().orElseThrow().asString()
                + "?fields=" + "citationCount"
                + "&limit=1";
    }

    @Override
    public @NonNull List<@NonNull BibEntry> getCitations(@NonNull BibEntry entry) throws FetcherException {
        if (entry.getDOI().isEmpty()) {
            return List.of();
        }

        URL citationsUrl;
        try {
            citationsUrl = URLUtil.create(getAPIUrl("citations", entry));
            LOGGER.debug("Cited URL {} ", citationsUrl);
        } catch (MalformedURLException e) {
            throw new FetcherException("Malformed URL", e);
        }
        URLDownload urlDownload = new URLDownload(importerPreferences, citationsUrl);

        importerPreferences.getApiKey(getName()).ifPresent(apiKey -> urlDownload.addHeader("x-api-key", apiKey));

        CitationsResponse citationsResponse = GSON
                .fromJson(urlDownload.asString(), CitationsResponse.class);

        return citationsResponse.getData()
                                .stream().filter(citationDataItem -> citationDataItem.getCitingPaper() != null)
                                .map(citationDataItem -> citationDataItem.getCitingPaper().toBibEntry()).toList();
    }

    @Override
    public @NonNull List<@NonNull BibEntry> getReferences(@NonNull BibEntry entry) throws FetcherException {
        if (entry.getDOI().isEmpty()) {
            return List.of();
        }

        URL referencesUrl;
        try {
            referencesUrl = URLUtil.create(getAPIUrl("references", entry));
            LOGGER.debug("Citing URL {} ", referencesUrl);
        } catch (MalformedURLException e) {
            throw new FetcherException("Malformed URL", e);
        }

        URLDownload urlDownload = new URLDownload(referencesUrl);
        importerPreferences.getApiKey(getName()).ifPresent(apiKey -> urlDownload.addHeader("x-api-key", apiKey));
        String response = urlDownload.asString();
        ReferencesResponse referencesResponse = GSON.fromJson(response, ReferencesResponse.class);

        if (referencesResponse.getData() == null) {
            // Get error message from citingPaperInfo.openAccessPdf.disclaimer
            JSONObject responseObject = new JSONObject(response);
            Optional.ofNullable(responseObject.optJSONObject("citingPaperInfo"))
                    .flatMap(citingPaperInfo -> Optional.ofNullable(citingPaperInfo.optJSONObject("openAccessPdf")))
                    .flatMap(openAccessPdf -> Optional.ofNullable(openAccessPdf.optString("disclaimer")))
                    .ifPresent(Unchecked.consumer(disclaimer -> {
                                LOGGER.debug("Received a disclaimer from Semantic Scholar: {}", disclaimer);
                                if (disclaimer.contains("references")) {
                                    throw new FetcherException(Localization.lang("Restricted access to references: %0", disclaimer));
                                }
                            }
                    ));
            return List.of();
        }

        return referencesResponse.getData()
                                 .stream()
                                 .filter(citationDataItem -> citationDataItem.getCitedPaper() != null)
                                 .map(referenceDataItem -> referenceDataItem.getCitedPaper().toBibEntry()).toList();
    }

    @Override
    public Optional<Integer> getCitationCount(BibEntry entry) throws FetcherException {
        if (entry.getDOI().isEmpty()) {
            return Optional.empty();
        }
        URL referencesUrl;
        try {
            referencesUrl = URLUtil.create(getUrlForCitationCount(entry));
        } catch (MalformedURLException e) {
            throw new FetcherException("Malformed URL", e);
        }
        URLDownload urlDownload = new URLDownload(referencesUrl);
        importerPreferences.getApiKey(getName()).ifPresent(apiKey -> urlDownload.addHeader("x-api-key", apiKey));
        String result;
        try {
            result = urlDownload.asString();
        } catch (FetcherException e) {
            e.getHttpResponse().ifPresent(Unchecked.consumer(response -> {
                Optional.ofNullable(response.responseBody())
                        .map(JSONObject::new)
                        .flatMap(json -> Optional.ofNullable(json.getString("error"))
                                                 .map(Unchecked.function(error -> {
                                                     throw new FetcherException(referencesUrl, error, e);
                                                 })));
            }));
            throw e;
        }
        PaperDetails paperDetails = GSON.fromJson(result, PaperDetails.class);

        if (paperDetails == null) {
            return Optional.empty();
        }
        return Optional.of(paperDetails.getCitationCount());
    }

    @Override
    public String getName() {
        return FETCHER_NAME;
    }
}
