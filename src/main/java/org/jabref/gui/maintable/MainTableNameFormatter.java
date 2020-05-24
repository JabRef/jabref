package org.jabref.gui.maintable;

import org.jabref.model.entry.AuthorList;
import org.jabref.preferences.PreferencesService;

public class MainTableNameFormatter {

    private final PreferencesService preferencesService;

    MainTableNameFormatter(PreferencesService preferences) {
        this.preferencesService = preferences;
    }

    /**
     * Format a name field for the table, according to user preferences.
     *
     * @param nameToFormat The contents of the name field.
     * @return The formatted name field.
     */
    public String formatName(final String nameToFormat) {
        if (nameToFormat == null) {
            return null;
        }

        MainTableNameFormatPreferences nameFormatPreferences = preferencesService.getMainTableNameFormatPreferences();
        MainTableNameFormatPreferences.DisplayStyle displayStyle = nameFormatPreferences.getDisplayStyle();
        MainTableNameFormatPreferences.AbbreviationStyle abbreviationStyle = nameFormatPreferences.getAbbreviationStyle();

        if (((displayStyle == MainTableNameFormatPreferences.DisplayStyle.FIRSTNAME_LASTNAME)
                || (displayStyle == MainTableNameFormatPreferences.DisplayStyle.LASTNAME_FIRSTNAME))
                && abbreviationStyle == MainTableNameFormatPreferences.AbbreviationStyle.LASTNAME_ONLY) {
            return AuthorList.fixAuthorLastNameOnlyCommas(nameToFormat, false);
        }

        return switch (nameFormatPreferences.getDisplayStyle()) {
            case AS_IS -> nameToFormat;
            case NATBIB -> AuthorList.fixAuthorNatbib(nameToFormat);
            case FIRSTNAME_LASTNAME -> AuthorList.fixAuthorFirstNameFirstCommas(
                    nameToFormat,
                    abbreviationStyle == MainTableNameFormatPreferences.AbbreviationStyle.FULL,
                    false);
            case LASTNAME_FIRSTNAME -> AuthorList.fixAuthorLastNameFirstCommas(
                    nameToFormat,
                    abbreviationStyle == MainTableNameFormatPreferences.AbbreviationStyle.FULL,
                    false);
        };
    }
}
