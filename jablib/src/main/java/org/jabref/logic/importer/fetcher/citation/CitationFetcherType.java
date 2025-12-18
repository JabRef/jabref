package org.jabref.logic.importer.fetcher.citation;

import org.jabref.logic.importer.fetcher.citation.crossref.CrossRefCitationFetcher;
import org.jabref.logic.importer.fetcher.citation.semanticscholar.SemanticScholarCitationFetcher;

public enum CitationFetcherType {
    CROSSREF("CrossRef"),
    SEMANTICSCHOLAR("Semantic Scholar");

    private final String name;

    CitationFetcherType(String displayName) {
        this.name = displayName;
    }

    public String getName() {
        return name;
    }

    public String getFetcherName() {
        return switch (this) {
            case CROSSREF ->
                    CrossRefCitationFetcher.FETCHER_NAME;
            case SEMANTICSCHOLAR ->
                    SemanticScholarCitationFetcher.FETCHER_NAME;
            default ->
                    throw new IllegalArgumentException("Unknown CitationFetcherType: " + this);
        };
    }

    @Override
    public String toString() {
        return name;
    }
}
