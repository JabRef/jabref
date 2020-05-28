package org.jabref.gui.maintable;

import org.jabref.model.entry.AuthorList;
import org.jabref.preferences.PreferencesService;

public class MainTableNameFormatter {

    private final PreferencesService preferencesService;

    MainTableNameFormatter(PreferencesService preferences) {
        this.preferencesService = preferences;
    }

    /**
     * Format a name field for the table, according to user preferences and with latex expressions translated if
     * possible.
     *
     * @param nameToFormat The contents of the name field.
     * @return The formatted name field.
     */
    public String formatNameLatexFree(final String nameToFormat) {
        if (nameToFormat == null) {
            return null;
        }

        MainTableNameFormatPreferences nameFormatPreferences = preferencesService.getMainTableNameFormatPreferences();
        MainTableNameFormatPreferences.DisplayStyle displayStyle = nameFormatPreferences.getDisplayStyle();
        MainTableNameFormatPreferences.AbbreviationStyle abbreviationStyle = nameFormatPreferences.getAbbreviationStyle();

        AuthorList authors = AuthorList.parse(nameToFormat);

        if (((displayStyle == MainTableNameFormatPreferences.DisplayStyle.FIRSTNAME_LASTNAME)
                || (displayStyle == MainTableNameFormatPreferences.DisplayStyle.LASTNAME_FIRSTNAME))
                && abbreviationStyle == MainTableNameFormatPreferences.AbbreviationStyle.LASTNAME_ONLY) {
            return authors.getAsLastNamesLatexFree(false);
        }

        switch (nameFormatPreferences.getDisplayStyle()) {
            case AS_IS:
                return nameToFormat;
            case NATBIB:
                return authors.getAsNatbibLatexFree();
            case FIRSTNAME_LASTNAME:
                return authors.getAsFirstLastNamesLatexFree(
                        abbreviationStyle == MainTableNameFormatPreferences.AbbreviationStyle.FULL,
                        false);
            default:
            case LASTNAME_FIRSTNAME:
                return authors.getAsLastFirstNamesLatexFree(
                        abbreviationStyle == MainTableNameFormatPreferences.AbbreviationStyle.FULL,
                        false);
        }
    }
}
