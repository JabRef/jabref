package net.sf.jabref.migrations;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;

public class PreferencesMigrations {
    /**
     * This method is called at startup, and makes necessary adaptations to
     * preferences for users from an earlier version of Jabref.
     */
    public static void performCompatibilityUpdate() {
        // Make sure "abstract" is not in General fields, because
        // Jabref 1.55 moves the abstract to its own tab.
        String genFields = Globals.prefs.get(JabRefPreferences.GENERAL_FIELDS);
        // pr(genFields+"\t"+genFields.indexOf("abstract"));
        if (genFields.contains("abstract")) {
            // pr(genFields+"\t"+genFields.indexOf("abstract"));
            String newGen;
            if (genFields.equals("abstract")) {
                newGen = "";
            } else if (genFields.contains(";abstract;")) {
                newGen = genFields.replaceAll(";abstract;", ";");
            } else if (genFields.indexOf("abstract;") == 0) {
                newGen = genFields.replaceAll("abstract;", "");
            } else if (genFields.indexOf(";abstract") == genFields.length() - 9) {
                newGen = genFields.replaceAll(";abstract", "");
            } else {
                newGen = genFields;
            }
            // pr(newGen);
            Globals.prefs.put(JabRefPreferences.GENERAL_FIELDS, newGen);
        }
    }
}
