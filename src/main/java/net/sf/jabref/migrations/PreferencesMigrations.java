package net.sf.jabref.migrations;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;

import java.util.HashMap;
import java.util.Map;

public class PreferencesMigrations {

    /**
     * This method is called at startup, and makes necessary adaptations to
     * preferences for users from an earlier version of Jabref.
     */
    public static void replaceAbstractField() {
        // Make sure "abstract" is not in General fields, because
        // Jabref 1.55 moves the abstract to its own tab.
        String genFields = Globals.prefs.get(JabRefPreferences.GENERAL_FIELDS);

        if (genFields.contains("abstract")) {

            String newGen;
            if ("abstract".equals(genFields)) {
                newGen = "";
            } else if (genFields.contains(";abstract;")) {
                newGen = genFields.replace(";abstract;", ";");
            } else if (genFields.indexOf("abstract;") == 0) {
                newGen = genFields.replace("abstract;", "");
            } else if (genFields.indexOf(";abstract") == (genFields.length() - 9)) {
                newGen = genFields.replace(";abstract", "");
            } else {
                newGen = genFields;
            }

            Globals.prefs.put(JabRefPreferences.GENERAL_FIELDS, newGen);
        }
    }

    /**
     * Added from Jabref 2.11 beta 4 onwards to fix wrong encoding names
     */
    public static void upgradeFaultyEncodingStrings() {
        JabRefPreferences prefs = Globals.prefs;
        String defaultEncoding = prefs.get(JabRefPreferences.DEFAULT_ENCODING);
        if (defaultEncoding == null) {
            return;
        }

        Map<String, String> encodingMap = new HashMap<>();
        encodingMap.put("UTF8", "UTF-8");
        encodingMap.put("Cp1250", "CP1250");
        encodingMap.put("Cp1251", "CP1251");
        encodingMap.put("Cp1252", "CP1252");
        encodingMap.put("Cp1253", "CP1253");
        encodingMap.put("Cp1254", "CP1254");
        encodingMap.put("Cp1257", "CP1257");
        encodingMap.put("ISO8859_1", "ISO8859-1");
        encodingMap.put("ISO8859_2", "ISO8859-2");
        encodingMap.put("ISO8859_3", "ISO8859-3");
        encodingMap.put("ISO8859_4", "ISO8859-4");
        encodingMap.put("ISO8859_5", "ISO8859-5");
        encodingMap.put("ISO8859_6", "ISO8859-6");
        encodingMap.put("ISO8859_7", "ISO8859-7");
        encodingMap.put("ISO8859_8", "ISO8859-8");
        encodingMap.put("ISO8859_9", "ISO8859-9");
        encodingMap.put("ISO8859_13", "ISO8859-13");
        encodingMap.put("ISO8859_15", "ISO8859-15");
        encodingMap.put("KOI8_R", "KOI8-R");
        encodingMap.put("Big5_HKSCS", "Big5-HKSCS");
        encodingMap.put("EUC_JP", "EUC-JP");

        if (encodingMap.containsKey(defaultEncoding)) {
            prefs.put(JabRefPreferences.DEFAULT_ENCODING, encodingMap.get(defaultEncoding));
        }
    }

    /**
     * Upgrade the sort order preferences for the current version
     * The old preference is kept in case an old version of JabRef is used with
     * these preferences, but it is only used when the new preference does not
     * exist
     */
    public static void upgradeSortOrder() {
        JabRefPreferences prefs = Globals.prefs;

        if (prefs.get(JabRefPreferences.EXPORT_IN_SPECIFIED_ORDER, null) == null) {
            if (prefs.getBoolean("exportInStandardOrder", false)) {
                prefs.putBoolean(JabRefPreferences.EXPORT_IN_SPECIFIED_ORDER, true);
                prefs.put(JabRefPreferences.EXPORT_PRIMARY_SORT_FIELD, "author");
                prefs.put(JabRefPreferences.EXPORT_SECONDARY_SORT_FIELD, "editor");
                prefs.put(JabRefPreferences.EXPORT_TERTIARY_SORT_FIELD, "year");
                prefs.putBoolean(JabRefPreferences.EXPORT_PRIMARY_SORT_DESCENDING, false);
                prefs.putBoolean(JabRefPreferences.EXPORT_SECONDARY_SORT_DESCENDING, false);
                prefs.putBoolean(JabRefPreferences.EXPORT_TERTIARY_SORT_DESCENDING, false);
            } else if (prefs.getBoolean("exportInTitleOrder", false)) {
                // exportInTitleOrder => title, author, editor
                prefs.putBoolean(JabRefPreferences.EXPORT_IN_SPECIFIED_ORDER, true);
                prefs.put(JabRefPreferences.EXPORT_PRIMARY_SORT_FIELD, "title");
                prefs.put(JabRefPreferences.EXPORT_SECONDARY_SORT_FIELD, "author");
                prefs.put(JabRefPreferences.EXPORT_TERTIARY_SORT_FIELD, "editor");
                prefs.putBoolean(JabRefPreferences.EXPORT_PRIMARY_SORT_DESCENDING, false);
                prefs.putBoolean(JabRefPreferences.EXPORT_SECONDARY_SORT_DESCENDING, false);
                prefs.putBoolean(JabRefPreferences.EXPORT_TERTIARY_SORT_DESCENDING, false);
            }
        }
    }

}
