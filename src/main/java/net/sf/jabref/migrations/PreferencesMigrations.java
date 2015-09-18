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

    /**
     * Added from Jabref 2.11 beta 4 onwards to fix wrong encoding names
     */
    public static void upgradeFaultyEncodingStrings(){
        JabRefPreferences prefs = Globals.prefs;
        String defaultEncoding = prefs.get(prefs.DEFAULT_ENCODING);

        if(defaultEncoding == null){
            return;
        }

        switch(defaultEncoding){
            case "UTF8":
                prefs.put(prefs.DEFAULT_ENCODING, "UTF-8");
                break;
            case "Cp1250":
                prefs.put(prefs.DEFAULT_ENCODING, "CP1250");
                break;
            case "Cp1251":
                prefs.put(prefs.DEFAULT_ENCODING, "CP1251");
                break;
            case "Cp1252":
                prefs.put(prefs.DEFAULT_ENCODING, "CP1252");
                break;
            case "Cp1253":
                prefs.put(prefs.DEFAULT_ENCODING, "CP1253");
                break;
            case "Cp1254":
                prefs.put(prefs.DEFAULT_ENCODING, "CP1254");
                break;
            case "Cp1257":
                prefs.put(prefs.DEFAULT_ENCODING, "CP1257");
                break;
            case "ISO8859_1":
                prefs.put(prefs.DEFAULT_ENCODING, "ISO8859-1");
                break;
            case "ISO8859_2":
                prefs.put(prefs.DEFAULT_ENCODING, "ISO8859-2");
                break;
            case "ISO8859_3":
                prefs.put(prefs.DEFAULT_ENCODING, "ISO8859-3");
                break;
            case "ISO8859_4":
                prefs.put(prefs.DEFAULT_ENCODING, "ISO8859-4");
                break;
            case "ISO8859_5":
                prefs.put(prefs.DEFAULT_ENCODING, "ISO8859-5");
                break;
            case "ISO8859_6":
                prefs.put(prefs.DEFAULT_ENCODING, "ISO8859-6");
                break;
            case "ISO8859_7":
                prefs.put(prefs.DEFAULT_ENCODING, "ISO8859-7");
                break;
            case "ISO8859_8":
                prefs.put(prefs.DEFAULT_ENCODING, "ISO8859-8");
                break;
            case "ISO8859_9":
                prefs.put(prefs.DEFAULT_ENCODING, "ISO8859-9");
                break;
            case "ISO8859_13":
                prefs.put(prefs.DEFAULT_ENCODING, "ISO8859-13");
                break;
            case "ISO8859_15":
                prefs.put(prefs.DEFAULT_ENCODING, "ISO8859-15");
                break;
            case "KOI8_R":
                prefs.put(prefs.DEFAULT_ENCODING, "KOI8-R");
                break;
            case "Big5_HKSCS":
                prefs.put(prefs.DEFAULT_ENCODING, "Big5-HKSCS");
                break;
            case "EUC_JP":
                prefs.put(prefs.DEFAULT_ENCODING, "EUC-JP");
                break;
        }


    }
}
