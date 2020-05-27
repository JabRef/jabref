package org.jabref.gui.maintable;

import org.jabref.model.entry.AuthorList;
import org.jabref.preferences.JabRefPreferences;

public class MainTableNameFormatter {

    private final boolean namesNatbib;
    private final boolean namesLastOnly;
    private final boolean namesAsIs;
    private final boolean namesFf;
    private final boolean abbrAuthorNames;

    MainTableNameFormatter(JabRefPreferences preferences) {
        namesNatbib = preferences.getBoolean(JabRefPreferences.NAMES_NATBIB);
        namesLastOnly = preferences.getBoolean(JabRefPreferences.NAMES_LAST_ONLY);
        namesAsIs = preferences.getBoolean(JabRefPreferences.NAMES_AS_IS);
        namesFf = preferences.getBoolean(JabRefPreferences.NAMES_FIRST_LAST);
        abbrAuthorNames = preferences.getBoolean(JabRefPreferences.ABBR_AUTHOR_NAMES);
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

        if (namesAsIs) {
            return nameToFormat;
        } else if (namesNatbib) {
            return authors.getAsNatbibLatexFree();
        } else if (namesLastOnly) {
            return authors.getAsLastNamesLatexFree(false);
        } else if (namesFf) {
            return authors.getAsFirstLastNamesLatexFree(abbrAuthorNames, false);
        } else {
            return authors.getAsLastFirstNamesLatexFree(abbrAuthorNames, false);
        }
    }
}
