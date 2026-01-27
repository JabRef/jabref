package org.jabref.logic.citationkeypattern;

import java.util.Map;

import org.jabref.model.entry.types.EntryType;

public class GlobalCitationKeyPatterns extends AbstractCitationKeyPatterns {

    public GlobalCitationKeyPatterns(CitationKeyPattern defaultPattern) {
        this.defaultPattern = defaultPattern;
    }

    public static GlobalCitationKeyPatterns fromPattern(String pattern) {
        return new GlobalCitationKeyPatterns(new CitationKeyPattern(pattern));
    }

    public GlobalCitationKeyPatterns copy() {
        GlobalCitationKeyPatterns copy = new GlobalCitationKeyPatterns(this.defaultPattern);
        for (Map.Entry<EntryType, CitationKeyPattern> entry : this.data.entrySet()) {
            copy.data.put(entry.getKey(), entry.getValue());
        }
        return copy;
    }

    public void setDefaultPattern(String pattern) {
        this.defaultPattern = new CitationKeyPattern(pattern);
    }

    @Override
    public CitationKeyPattern getLastLevelCitationKeyPattern(EntryType entryType) {
        return defaultPattern;
    }
}
