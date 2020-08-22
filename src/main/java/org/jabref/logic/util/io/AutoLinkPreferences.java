package org.jabref.logic.util.io;

public class AutoLinkPreferences {

    public enum CitationKeyDependency {
        START, // Filenames starting with the citation key
        EXACT, // Filenames exactly matching the citation key
        REGEX // Filenames matching a regular expression pattern
    }

    private final CitationKeyDependency citationKeyDependency;
    private final String regularExpression;
    private final boolean shouldSearchFilesOnOpen;
    private final boolean shouldOpenBrowseOnCreate;
    private final Character keywordDelimiter;

    public AutoLinkPreferences(CitationKeyDependency citationKeyDependency,
                               String regularExpression,
                               boolean shouldSearchFilesOnOpen,
                               boolean shouldOpenBrowseOnCreate,
                               Character keywordDelimiter) {
        this.citationKeyDependency = citationKeyDependency;
        this.regularExpression = regularExpression;
        this.shouldSearchFilesOnOpen = shouldSearchFilesOnOpen;
        this.shouldOpenBrowseOnCreate = shouldOpenBrowseOnCreate;
        this.keywordDelimiter = keywordDelimiter;
    }

    public CitationKeyDependency getCitationKeyDependency() {
        return citationKeyDependency;
    }

    public String getRegularExpression() {
        return regularExpression;
    }

    public boolean shouldSearchFilesOnOpen() {
        return shouldSearchFilesOnOpen;
    }

    public boolean shouldOpenBrowseOnCreate() {
        return shouldOpenBrowseOnCreate;
    }

    public Character getKeywordDelimiter() {
        return keywordDelimiter;
    }
}
