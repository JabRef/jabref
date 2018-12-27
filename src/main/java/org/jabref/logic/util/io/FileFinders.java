package org.jabref.logic.util.io;

public class FileFinders {
    /**
     * Creates a preconfigurated file finder based on the given AutoLink preferences.
     */
    public static FileFinder constructFromConfiguration(AutoLinkPreferences autoLinkPreferences) {
        if (autoLinkPreferences.isUseRegularExpression()) {
            return new RegExpBasedFileFinder(autoLinkPreferences.getRegularExpression(), autoLinkPreferences.getKeywordDelimiter());
        } else {
            return new CiteKeyBasedFileFinder(autoLinkPreferences.isOnlyFindByExactCiteKey());
        }
    }
}
