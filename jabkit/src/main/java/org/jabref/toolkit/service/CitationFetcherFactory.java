package org.jabref.toolkit.service;

import org.jabref.logic.ai.chatting.ChatModel;
import org.jabref.logic.ai.chatting.util.ChatModelFactory;
import org.jabref.logic.importer.fetcher.citation.CitationFetcher;
import org.jabref.logic.importer.fetcher.citation.CitationFetcherType;
import org.jabref.logic.preferences.CliPreferences;

import org.jspecify.annotations.NonNull;

public class CitationFetcherFactory {

    private final CliPreferences cliPreferences;

    private CitationFetcherFactory(CliPreferences cliPreferences) {
        this.cliPreferences = cliPreferences;
    }

    public static CitationFetcherFactory create(CliPreferences cliPreferences) {
        return new CitationFetcherFactory(cliPreferences);
    }

    public @NonNull CitationFetcher getCitationFetcher(CitationFetcherType citationFetcherType) {
        ChatModel chatModel = ChatModelFactory.create(cliPreferences.getAiPreferences());
        return CitationFetcherType.getCitationFetcher(
                citationFetcherType,
                cliPreferences.getImporterPreferences(),
                cliPreferences.getImportFormatPreferences(),
                cliPreferences.getCitationKeyPatternPreferences(),
                cliPreferences.getGrobidPreferences(),
                cliPreferences.getAiPreferences(),
                chatModel
        );
    }
}
