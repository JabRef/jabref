package net.sf.jabref.gui.maintable;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.model.entry.AuthorList;

public class MainTableNameFormatter {

    /**
     * Format a name field for the table, according to user preferences.
     *
     * @param nameToFormat The contents of the name field.
     * @return The formatted name field.
     */
    public static String formatName(String nameToFormat) {
        if (nameToFormat == null) {
            return null;
        }

        // Read name format options:
        boolean namesNatbib = Globals.prefs.getBoolean(JabRefPreferences.NAMES_NATBIB); //MK:
        boolean namesLastOnly = Globals.prefs.getBoolean(JabRefPreferences.NAMES_LAST_ONLY);
        boolean namesAsIs = Globals.prefs.getBoolean(JabRefPreferences.NAMES_AS_IS);
        boolean abbr_names = Globals.prefs.getBoolean(JabRefPreferences.ABBR_AUTHOR_NAMES); //MK:
        boolean namesFf = Globals.prefs.getBoolean(JabRefPreferences.NAMES_FIRST_LAST);
        boolean namesLf = !(namesAsIs || namesFf || namesNatbib || namesLastOnly); // None of the above.

        if (namesAsIs) {
            return nameToFormat;
        } else if (namesNatbib) {
            nameToFormat = AuthorList.fixAuthor_Natbib(nameToFormat);
        } else if (namesLastOnly) {
            nameToFormat = AuthorList.fixAuthor_lastNameOnlyCommas(nameToFormat, false);
        } else if (namesFf) {
            nameToFormat = AuthorList.fixAuthor_firstNameFirstCommas(nameToFormat, abbr_names, false);
        } else if (namesLf) {
            nameToFormat = AuthorList.fixAuthor_lastNameFirstCommas(nameToFormat, abbr_names, false);
        }
        return nameToFormat;
    }

}
