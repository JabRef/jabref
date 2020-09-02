package org.jabref.logic.util.io;

public class FileFinders {
    /**
     * Creates a preconfigurated file finder based on the given AutoLink preferences.
     */
    public static FileFinder constructFromConfiguration(AutoLinkPreferences autoLinkPreferences) {
        switch (autoLinkPreferences.getCitationKeyDependency()) {
            default:
            case START:
                return new CitationKeyBasedFileFinder(false);
            case EXACT:
                return new CitationKeyBasedFileFinder(true);
            case REGEX:
                return new RegExpBasedFileFinder(autoLinkPreferences.getRegularExpression(), autoLinkPreferences.getKeywordDelimiter());
        }
    }
}
