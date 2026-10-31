package org.jabref.logic.conferences;

import java.nio.file.Path;

import org.jabref.logic.journals.AbbreviationLoader;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class ConferenceAbbreviationLoader extends AbbreviationLoader<ConferenceAbbreviationRepository> {
    // [impl->req~abbreviations.conference-store~1]
    public ConferenceAbbreviationLoader() {
        super("conferences", "conference-list.mv");
    }

    public static ConferenceAbbreviationRepository loadRepository(ConferenceAbbreviationPreferences preferences) {
        return new ConferenceAbbreviationLoader().loadRepositorySynchronously(preferences);
    }

    public static ConferenceAbbreviationRepository loadBuiltInRepository() {
        return loadRepository(ConferenceAbbreviationPreferences.getDefault());
    }

    @Override
    protected ConferenceAbbreviationRepository createRepository(Path file) {
        return new ConferenceAbbreviationRepository(file);
    }

    @Override
    protected ConferenceAbbreviationRepository createFallbackRepository() {
        return new ConferenceAbbreviationRepository();
    }
}
