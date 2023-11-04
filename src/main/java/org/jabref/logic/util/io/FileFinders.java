package org.jabref.logic.util.io;

public class FileFinders {
    /**
     * Creates a preconfigurated file finder based on the given AutoLink preferences.
     */
    public static FileFinder constructFromConfiguration(AutoLinkPreferences autoLinkPreferences) {
        return switch (autoLinkPreferences.getCitationKeyDependency()) {
            case START ->
                    new CitationKeyBasedFileFinder(false);
            case EXACT ->
                    new CitationKeyBasedFileFinder(true);
            case REGEX ->
                    new RegExpBasedFileFinder(autoLinkPreferences.getRegularExpression(), autoLinkPreferences.getKeywordSeparator());
        };
    }
}
