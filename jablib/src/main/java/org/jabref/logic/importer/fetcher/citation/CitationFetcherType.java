package org.jabref.logic.importer.fetcher.citation;

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

    @Override
    public String toString() {
        return name;
    }
}
