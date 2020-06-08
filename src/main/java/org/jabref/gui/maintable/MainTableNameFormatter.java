package org.jabref.gui.maintable;

import org.jabref.model.entry.AuthorList;
import org.jabref.preferences.PreferencesService;

public class MainTableNameFormatter {
    private final MainTableNameFormatPreferences.DisplayStyle displayStyle;
    private final MainTableNameFormatPreferences.AbbreviationStyle abbreviationStyle;

    MainTableNameFormatter(PreferencesService preferences) {
        MainTableNameFormatPreferences nameFormatPreferences = preferences.getMainTableNameFormatPreferences();
        this.displayStyle = nameFormatPreferences.getDisplayStyle();
        this.abbreviationStyle = nameFormatPreferences.getAbbreviationStyle();
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

        AuthorList authors = AuthorList.parse(nameToFormat);

        if (((displayStyle == MainTableNameFormatPreferences.DisplayStyle.FIRSTNAME_LASTNAME)
                || (displayStyle == MainTableNameFormatPreferences.DisplayStyle.LASTNAME_FIRSTNAME))
                && abbreviationStyle == MainTableNameFormatPreferences.AbbreviationStyle.LASTNAME_ONLY) {
            return authors.getAsLastNamesLatexFree(false);
        }

        switch (displayStyle) {
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

    public boolean needsFormatting() {
        return displayStyle != MainTableNameFormatPreferences.DisplayStyle.AS_IS;
    }
}
