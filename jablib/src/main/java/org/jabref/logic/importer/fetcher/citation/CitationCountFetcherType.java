package org.jabref.logic.importer.fetcher.citation;

import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.fetcher.OpenAlex;
import org.jabref.logic.importer.fetcher.SciteAiFetcher;
import org.jabref.logic.importer.fetcher.citation.opencitations.OpenCitationsFetcher;
import org.jabref.logic.importer.fetcher.citation.semanticscholar.SemanticScholarCitationFetcher;

public enum CitationCountFetcherType {
    SEMANTIC_SCHOLAR(SemanticScholarCitationFetcher.FETCHER_NAME),
    OPEN_ALEX(OpenAlex.FETCHER_NAME),
    OPEN_CITATIONS(OpenCitationsFetcher.FETCHER_NAME),
    SCITE_AI(SciteAiFetcher.FETCHER_NAME);

    private final String name;

    CitationCountFetcherType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    public static CitationCountFetcher getCitationCountFetcher(CitationCountFetcherType type,
                                                                ImporterPreferences importerPreferences) {
        return switch (type) {
            case SEMANTIC_SCHOLAR ->
                    new SemanticScholarCitationFetcher(importerPreferences);
            case OPEN_ALEX ->
                    new OpenAlex(importerPreferences);
            case OPEN_CITATIONS ->
                    new OpenCitationsFetcher(importerPreferences);
            case SCITE_AI ->
                    new SciteAiFetcher();
        };
    }
}
