package org.jabref.logic.citationkeypattern;

import static org.jabref.logic.citationkeypattern.CitationKeyGenerator.DEFAULT_UNWANTED_CHARACTERS;

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
                "",
                ','
        );
    }
}
