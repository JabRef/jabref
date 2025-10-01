package org.jabref.logic.citationkeypattern;

import com.google.common.annotations.VisibleForTesting;

import static org.jabref.logic.citationkeypattern.CitationKeyGenerator.DEFAULT_UNWANTED_CHARACTERS;

public class CitationKeyGeneratorTestUtils {
    @VisibleForTesting
    public static CitationKeyPatternPreferences getInstanceForTesting() {
        return new CitationKeyPatternPreferences(
                false,
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
