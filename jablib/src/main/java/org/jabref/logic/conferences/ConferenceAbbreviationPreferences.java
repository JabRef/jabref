package org.jabref.logic.conferences;

import java.util.List;

import org.jabref.logic.journals.AbbreviationPreferences;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class ConferenceAbbreviationPreferences extends AbbreviationPreferences {
    public ConferenceAbbreviationPreferences(List<String> externalLists) {
        super(externalLists);
    }

    public static ConferenceAbbreviationPreferences getDefault() {
        return new ConferenceAbbreviationPreferences(List.of());
    }
}
