package org.jabref.logic.importer.fetcher.citation;

import org.jabref.logic.ai.AiService;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.fetcher.citation.crossref.CrossRefCitationFetcher;
import org.jabref.logic.importer.fetcher.citation.semanticscholar.SemanticScholarCitationFetcher;
import org.jabref.logic.importer.util.GrobidPreferences;
import org.jabref.logic.bibtex.FieldPreferences;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating CitationFetcher instances based on provider name.
 */
@NullMarked
public class CitationProviderFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(CitationProviderFactory.class);

    /**
     * Available citation providers
     */
    public enum Provider {
        SEMANTIC_SCHOLAR("Semantic Scholar"),
        CROSSREF("Crossref");

        private final String displayName;

        Provider(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        /**
         * Get provider by name (case-insensitive)
         */
        public static Provider fromName(String name) {
            if (name == null) {
                return SEMANTIC_SCHOLAR; // default
            }
            for (Provider provider : values()) {
                if (provider.name().equalsIgnoreCase(name) || 
                    provider.displayName.equalsIgnoreCase(name)) {
                    return provider;
                }
            }
            LOGGER.warn("Unknown citation provider: {}. Using default: Semantic Scholar", name);
            return SEMANTIC_SCHOLAR;
        }
    }

    /**
     * Creates a CitationFetcher instance based on the provider name.
     *
     * @param providerName the name of the citation provider
     * @param importerPreferences importer preferences
     * @param importFormatPreferences import format preferences
     * @param citationKeyPatternPreferences citation key pattern preferences
     * @param grobidPreferences grobid preferences
     * @param aiService AI service
     * @return CitationFetcher instance
     */
    public static CitationFetcher getCitationFetcher(
            String providerName,
            ImporterPreferences importerPreferences,
            ImportFormatPreferences importFormatPreferences,
            CitationKeyPatternPreferences citationKeyPatternPreferences,
            GrobidPreferences grobidPreferences,
            AiService aiService) {
        
        Provider provider = Provider.fromName(providerName);
        
        return switch (provider) {
            case SEMANTIC_SCHOLAR -> new SemanticScholarCitationFetcher(importerPreferences);
            case CROSSREF -> new CrossRefCitationFetcher(
                    importerPreferences,
                    importFormatPreferences,
                    citationKeyPatternPreferences,
                    grobidPreferences,
                    aiService);
        };
    }

    /**
     * Simplified version that only requires ImporterPreferences (for Semantic Scholar).
     * Falls back to Semantic Scholar if provider requires additional dependencies.
     *
     * @param providerName the name of the citation provider
     * @param importerPreferences importer preferences
     * @return CitationFetcher instance
     */
    public static CitationFetcher getCitationFetcher(
            String providerName,
            ImporterPreferences importerPreferences) {
        
        Provider provider = Provider.fromName(providerName);
        
        if (provider == Provider.CROSSREF) {
            LOGGER.warn("CrossRef provider requires additional dependencies. Falling back to Semantic Scholar.");
            return new SemanticScholarCitationFetcher(importerPreferences);
        }
        
        return new SemanticScholarCitationFetcher(importerPreferences);
    }

    /**
     * Get all available provider names
     */
    public static String[] getAvailableProviders() {
        Provider[] providers = Provider.values();
        String[] names = new String[providers.length];
        for (int i = 0; i < providers.length; i++) {
            names[i] = providers[i].getDisplayName();
        }
        return names;
    }

    /**
     * Get the default provider name
     */
    public static String getDefaultProvider() {
        return Provider.SEMANTIC_SCHOLAR.getDisplayName();
    }
}

