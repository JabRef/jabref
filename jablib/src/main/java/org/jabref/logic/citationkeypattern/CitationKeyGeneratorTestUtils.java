package org.jabref.logic.citationkeypattern;

import javafx.beans.property.SimpleObjectProperty;

import static org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences.DEFAULT_UNWANTED_CHARACTERS;

public class CitationKeyGeneratorTestUtils {

    public static CitationKeyPatternPreferences getInstanceForTesting() {
        return new CitationKeyPatternPreferences(
                true,
                false,
                false,
                false,
                CitationKeyPatternPreferences.KeySuffix.SECOND_WITH_A,
                "",
                "",
                DEFAULT_UNWANTED_CHARACTERS,
                GlobalCitationKeyPatterns.fromPattern("[auth][year]"),
                new SimpleObjectProperty<>(',')
        );
    }
}
