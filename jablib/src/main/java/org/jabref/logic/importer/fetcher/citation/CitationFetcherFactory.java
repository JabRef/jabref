package org.jabref.logic.importer.fetcher.citation;

import org.jabref.logic.ai.AiService;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.fetcher.citation.crossref.CrossRefCitationFetcher;
import org.jabref.logic.importer.fetcher.citation.semanticscholar.SemanticScholarCitationFetcher;
import org.jabref.logic.importer.util.GrobidPreferences;

public enum CitationFetcherFactory {
    INSTANCE;

    public CitationFetcher getCitationFetcher(
            String name,
            ImporterPreferences importerPreferences,
            ImportFormatPreferences importFormatPreferences,
            CitationKeyPatternPreferences citationKeyPatternPreferences,
            GrobidPreferences grobidPreferences,
            AiService aiService
    ) {
        CitationFetcher crossRef = new CrossRefCitationFetcher(
                importerPreferences,
                importFormatPreferences,
                citationKeyPatternPreferences,
                grobidPreferences,
                aiService
        );
        if (crossRef.getName().equals(name)) {
            return crossRef;
        }
        CitationFetcher semanticScholar = new SemanticScholarCitationFetcher(
                importerPreferences
        );
        if (semanticScholar.getName().equals(name)) {
            return semanticScholar;
        }
        throw new IllegalArgumentException("Unknown citation fetcher: " + name);
    }
}
