package org.jabref.logic.importer.fetcher.citation;

import org.jabref.logic.ai.AiService;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.fetcher.citation.crossref.CrossRefCitationFetcher;
import org.jabref.logic.importer.fetcher.citation.semanticscholar.SemanticScholarCitationFetcher;
import org.jabref.logic.importer.util.GrobidPreferences;

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

    public static CitationFetcher getCitationFetcher(CitationFetcherType citationFetcherName,
                                                     ImporterPreferences importerPreferences,
                                                     ImportFormatPreferences importFormatPreferences,
                                                     CitationKeyPatternPreferences citationKeyPatternPreferences,
                                                     GrobidPreferences grobidPreferences,
                                                     AiService aiService) {
        return switch (citationFetcherName) {
            case CROSSREF ->
                    new CrossRefCitationFetcher(importerPreferences, importFormatPreferences,
                            citationKeyPatternPreferences, grobidPreferences, aiService);
            case SEMANTIC_SCHOLAR ->
                    new SemanticScholarCitationFetcher(importerPreferences);
        };
    }
}
