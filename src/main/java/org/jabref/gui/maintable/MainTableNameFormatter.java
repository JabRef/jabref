package org.jabref.gui.maintable;

import org.jabref.Globals;
import org.jabref.model.entry.AuthorList;
import org.jabref.preferences.JabRefPreferences;

public class MainTableNameFormatter {

    private MainTableNameFormatter() { }

    /**
     * Format a name field for the table, according to user preferences.
     *
     * @param nameToFormat The contents of the name field.
     * @return The formatted name field.
     */
    public static String formatName(final String nameToFormat) {
        if (nameToFormat == null) {
            return null;
        }

        // Read name format options:
        final boolean namesNatbib = Globals.prefs.getBoolean(JabRefPreferences.NAMES_NATBIB); //MK:
        final boolean namesLastOnly = Globals.prefs.getBoolean(JabRefPreferences.NAMES_LAST_ONLY);
        final boolean namesAsIs = Globals.prefs.getBoolean(JabRefPreferences.NAMES_AS_IS);
        final boolean namesFf = Globals.prefs.getBoolean(JabRefPreferences.NAMES_FIRST_LAST);

        final boolean abbrAuthorNames = Globals.prefs.getBoolean(JabRefPreferences.ABBR_AUTHOR_NAMES); //MK:

        if (namesAsIs) {
            return nameToFormat;
        } else if (namesNatbib) {
            return AuthorList.fixAuthorNatbib(nameToFormat);
        } else if (namesLastOnly) {
            return AuthorList.fixAuthorLastNameOnlyCommas(nameToFormat, false);
        } else if (namesFf) {
            return AuthorList.fixAuthorFirstNameFirstCommas(nameToFormat, abbrAuthorNames, false);
        }

        // None of namesAsIs, namesNatbib, namesAsIs, namesFf
        return AuthorList.fixAuthorLastNameFirstCommas(nameToFormat, abbrAuthorNames, false);
    }

}
