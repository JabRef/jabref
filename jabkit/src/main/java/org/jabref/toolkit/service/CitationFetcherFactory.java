package org.jabref.toolkit.service;

import org.jabref.logic.ai.AiServiceFactory;
import org.jabref.logic.importer.fetcher.citation.CitationFetcher;
import org.jabref.logic.importer.fetcher.citation.CitationFetcherType;
import org.jabref.logic.preferences.CliPreferences;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class CitationFetcherFactory {

    private final CliPreferences cliPreferences;

    private CitationFetcherFactory(CliPreferences cliPreferences) {
        this.cliPreferences = cliPreferences;
    }

    public static CitationFetcherFactory create(CliPreferences cliPreferences) {
        return new CitationFetcherFactory(cliPreferences);
    }

    public CitationFetcher getCitationFetcher(CitationFetcherType citationFetcherType) {
        return CitationFetcherType.getCitationFetcher(
                citationFetcherType,
                cliPreferences.getImporterPreferences(),
                cliPreferences.getImportFormatPreferences(),
                cliPreferences.getCitationKeyPatternPreferences(),
                cliPreferences.getGrobidPreferences(),
                AiServiceFactory.createNoOpService()
        );
    }
}
