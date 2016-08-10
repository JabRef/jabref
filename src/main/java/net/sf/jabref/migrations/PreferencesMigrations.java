package net.sf.jabref.migrations;

import java.util.HashMap;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefMain;
import net.sf.jabref.model.bibtexkeypattern.AbstractBibtexKeyPattern;
import net.sf.jabref.model.bibtexkeypattern.GlobalBibtexKeyPattern;
import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.preferences.JabRefPreferences;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class PreferencesMigrations {

    private static final Log LOGGER = LogFactory.getLog(PreferencesMigrations.class);

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
                prefs.put(JabRefPreferences.EXPORT_PRIMARY_SORT_FIELD, FieldName.AUTHOR);
                prefs.put(JabRefPreferences.EXPORT_SECONDARY_SORT_FIELD, FieldName.EDITOR);
                prefs.put(JabRefPreferences.EXPORT_TERTIARY_SORT_FIELD, FieldName.YEAR);
                prefs.putBoolean(JabRefPreferences.EXPORT_PRIMARY_SORT_DESCENDING, false);
                prefs.putBoolean(JabRefPreferences.EXPORT_SECONDARY_SORT_DESCENDING, false);
                prefs.putBoolean(JabRefPreferences.EXPORT_TERTIARY_SORT_DESCENDING, false);
            } else if (prefs.getBoolean("exportInTitleOrder", false)) {
                // exportInTitleOrder => title, author, editor
                prefs.putBoolean(JabRefPreferences.EXPORT_IN_SPECIFIED_ORDER, true);
                prefs.put(JabRefPreferences.EXPORT_PRIMARY_SORT_FIELD, FieldName.TITLE);
                prefs.put(JabRefPreferences.EXPORT_SECONDARY_SORT_FIELD, FieldName.AUTHOR);
                prefs.put(JabRefPreferences.EXPORT_TERTIARY_SORT_FIELD, FieldName.EDITOR);
                prefs.putBoolean(JabRefPreferences.EXPORT_PRIMARY_SORT_DESCENDING, false);
                prefs.putBoolean(JabRefPreferences.EXPORT_SECONDARY_SORT_DESCENDING, false);
                prefs.putBoolean(JabRefPreferences.EXPORT_TERTIARY_SORT_DESCENDING, false);
            }
        }
    }

    /**
     * Migrate LabelPattern configuration from version 3.3-3.5 to new BibtexKeyPatterns
     */
    public static void upgradeLabelPatternToBibtexKeyPattern() {

        try {
            Preferences mainPrefsNode = Preferences.userNodeForPackage(JabRefMain.class);

            if (mainPrefsNode.nodeExists("bibtexkeypatterns")) {
                return; //Pref node already exists do not migrate from previous version
            }

            if (mainPrefsNode.nodeExists("logic/labelpattern")) {
                LOGGER.info("Found old Bibtex Key patterns which will be migrated to new version.");
                JabRefPreferences prefs = Globals.prefs;
                GlobalBibtexKeyPattern keyPattern = new GlobalBibtexKeyPattern(AbstractBibtexKeyPattern
                        .split(prefs.get(JabRefPreferences.DEFAULT_BIBTEX_KEY_PATTERN)));
                Preferences oldPatternPrefs = mainPrefsNode.node("logic/labelpattern");
                for (String key : oldPatternPrefs.keys()) {
                    keyPattern.addBibtexKeyPattern(key, oldPatternPrefs.get(key, null));
                }
                prefs.putKeyPattern(keyPattern);
            }
        } catch (BackingStoreException e) {
            LOGGER.error("Migrating old bibtexKeyPatterns failed.", e);
        }
    }

}
