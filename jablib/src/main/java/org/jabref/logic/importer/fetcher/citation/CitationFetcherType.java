package org.jabref.logic.importer.fetcher.citation;

import org.jabref.logic.importer.fetcher.citation.crossref.CrossRefCitationFetcher;
import org.jabref.logic.importer.fetcher.citation.semanticscholar.SemanticScholarCitationFetcher;

public enum CitationFetcherType {
    CROSSREF("CrossRef"),
    SEMANTIC_SCHOLAR("Semantic Scholar");

    private final String name;

    CitationFetcherType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getFetcherName() {
        return switch (this) {
            case CROSSREF ->
                    CrossRefCitationFetcher.FETCHER_NAME;
            case SEMANTIC_SCHOLAR ->
                    SemanticScholarCitationFetcher.FETCHER_NAME;
        };
    }

    @Override
    public String toString() {
        return name;
    }
}
