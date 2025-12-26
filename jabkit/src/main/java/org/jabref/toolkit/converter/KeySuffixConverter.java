package org.jabref.toolkit.converter;

import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;

public class KeySuffixConverter
        extends CaseInsensitiveEnumConverter<CitationKeyPatternPreferences.KeySuffix> {

    public KeySuffixConverter() {
        super(CitationKeyPatternPreferences.KeySuffix.class);
    }
}
