package org.jabref.logic.importer.citationrelation;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.gui.entryeditor.citationrelationtab.RelatedEntriesFetcher;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.fetcher.CustomizableKeyFetcher;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.util.BuildInfo;
import org.jabref.model.entry.BibEntry;

import com.google.gson.Gson;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SemanticScholarFetcher implements CitationFetcher, CustomizableKeyFetcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(SemanticScholarFetcher.class);
    private static final SemanticScholarFetcher DEFAULT = new SemanticScholarFetcher();
    private static final String SEMANTIC_SCHOLAR_API = "https://api.semanticscholar.org/graph/v1/";

    private static final String API_KEY = new BuildInfo().semanticScholarApiKey;

    private final ImporterPreferences importerPreferences;

    public SemanticScholarFetcher(@Nullable ImporterPreferences importerPreferences) {
        this.importerPreferences = importerPreferences;
    }

    public SemanticScholarFetcher() {
        this(null);
    }

    @Override
    public List<BibEntry> searchCitedBy(BibEntry entry) {
        if (entry.getDOI().isEmpty()) {
            return Collections.emptyList();
        }

        StringBuilder urlBuilder = new StringBuilder(SEMANTIC_SCHOLAR_API)
                .append("paper/")
                .append("DOI:").append(entry.getDOI().get().getDOI())
                .append("/citations")
                .append("?fields=").append("title,authors,year,citationCount,referenceCount")
                .append("&limit=1000");
        try {
            URL citationsUrl = URI.create(urlBuilder.toString()).toURL();
            URLDownload urlDownload = new URLDownload(citationsUrl);

            Optional<String> apiKeyOpt = getApiKey();
            apiKeyOpt.ifPresent(s -> urlDownload.addHeader("x-api-key", s));

            CitationsResponse citationsResponse = new Gson()
                    .fromJson(urlDownload.asString(), CitationsResponse.class);

            return citationsResponse.getData()
                                    .stream().filter(citationDataItem -> citationDataItem.getCitingPaper() != null)
                                    .map(citationDataItem -> citationDataItem.getCitingPaper().toBibEntry()).toList();
        } catch (IOException e) {
            LOGGER.warn("Failed to fetch entries cited by entry: {}", entry.getDOI().get().getDOI(), e);
        }

        return Collections.emptyList();
    }

    @Override
    public List<BibEntry> searchCiting(BibEntry entry) {
        if (entry.getDOI().isEmpty()) {
            return Collections.emptyList();
        }

        StringBuilder urlBuilder = new StringBuilder(SEMANTIC_SCHOLAR_API)
                .append("paper/")
                .append("DOI:").append(entry.getDOI().get().getDOI())
                .append("/references")
                .append("?fields=")
                .append("title,authors,year,citationCount,referenceCount")
                .append("&limit=1000");
        try {
            URL referencesUrl = URI.create(urlBuilder.toString()).toURL();
            URLDownload urlDownload = new URLDownload(referencesUrl);

            Optional<String> apiKeyOpt = getApiKey();
            apiKeyOpt.ifPresent(s -> urlDownload.addHeader("x-api-key", s));

            ReferencesResponse referencesResponse = new Gson()
                    .fromJson(urlDownload.asString(), ReferencesResponse.class);

            return referencesResponse.getData()
                                     .stream()
                                     .filter(citationDataItem -> citationDataItem.getCitedPaper() != null)
                                     .map(referenceDataItem -> referenceDataItem.getCitedPaper().toBibEntry()).toList();
        } catch (IOException e) {
            LOGGER.warn("Failed to fetch entries citing entry: {}", entry.getDOI().get().getDOI(), e);
        }

        return Collections.emptyList();
    }

    // TODO: Move. Don't belong here
    public static RelatedEntriesFetcher buildCitationsFetcher() {
        return DEFAULT::searchCitedBy;
    }

    public static RelatedEntriesFetcher buildReferencesFetcher() {
        return DEFAULT::searchCiting;
    }

    @Override
    public String getName() {
        return "Semantic Scholar Citations Fetcher";
    }

    private Optional<String> getApiKey() {
        if (importerPreferences == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(importerPreferences.getApiKey(getName()).orElse(API_KEY));
    }
}
