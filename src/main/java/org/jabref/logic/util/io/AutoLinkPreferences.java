package org.jabref.logic.util.io;

public class AutoLinkPreferences {

    public enum CitationKeyDependency {
        START, // Filenames starting with the citation key
        EXACT, // Filenames exactly matching the citation key
        REGEX // Filenames matching a regular expression pattern
    }

    private final CitationKeyDependency citationKeyDependency;
    private final String regularExpression;
    private boolean askAutoNamingPdfs;
    private final Character keywordDelimiter;

    public AutoLinkPreferences(CitationKeyDependency citationKeyDependency,
                               String regularExpression,
                               boolean askAutoNamingPdfs,
                               Character keywordDelimiter) {
        this.citationKeyDependency = citationKeyDependency;
        this.regularExpression = regularExpression;
        this.askAutoNamingPdfs = askAutoNamingPdfs;
        this.keywordDelimiter = keywordDelimiter;
    }

    public CitationKeyDependency getCitationKeyDependency() {
        return citationKeyDependency;
    }

    public String getRegularExpression() {
        return regularExpression;
    }

    public boolean shouldAskAutoNamingPdfs() {
        return askAutoNamingPdfs;
    }

    public AutoLinkPreferences withAskAutoNamingPdfs(boolean shouldAskAutoNamingPdfs) {
        this.askAutoNamingPdfs = shouldAskAutoNamingPdfs;
        return this;
    }

    public Character getKeywordDelimiter() {
        return keywordDelimiter;
    }
}
