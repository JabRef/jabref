package org.jabref.logic.util.io;

public class AutoLinkPreferences {

    public enum CitationKeyDependency {
        START, // Filenames starting with the citation key
        EXACT, // Filenames exactly matching the citation key
        REGEX // Filenames matching a regular expression pattern
    }

    private final CitationKeyDependency citationKeyDependency;
    private final String regularExpression;

    private final Character keywordDelimiter;

    public AutoLinkPreferences(CitationKeyDependency citationKeyDependency,
                               String regularExpression,

                               Character keywordDelimiter) {
        this.citationKeyDependency = citationKeyDependency;
        this.regularExpression = regularExpression;
        this.keywordDelimiter = keywordDelimiter;
    }

    public CitationKeyDependency getCitationKeyDependency() {
        return citationKeyDependency;
    }

    public String getRegularExpression() {
        return regularExpression;
    }

    public Character getKeywordDelimiter() {
        return keywordDelimiter;
    }
}
