package org.jabref.preferences;

import org.jabref.logic.bibtex.LatexFieldFormatterPreferences;
import org.jabref.logic.exporter.SavePreferences;
import org.jabref.model.bibtexkeypattern.GlobalBibtexKeyPattern;
import org.jabref.model.metadata.SaveOrderConfig;

import java.nio.charset.Charset;

public class SavePreferencesFactory {

    public static SavePreferences loadForExportFromPreferences(JabRefPreferences preferences) {
        Boolean saveInOriginalOrder = preferences.getBoolean(JabRefPreferences.EXPORT_IN_ORIGINAL_ORDER);
        SaveOrderConfig saveOrder = null;
        if (!saveInOriginalOrder) {
            if (preferences.getBoolean(JabRefPreferences.EXPORT_IN_SPECIFIED_ORDER)) {
                saveOrder = preferences.loadExportSaveOrder();
            } else {
                saveOrder = preferences.loadTableSaveOrder();
            }
        }
        Charset encoding = preferences.getDefaultEncoding();
        Boolean makeBackup = preferences.getBoolean(JabRefPreferences.BACKUP);
        SavePreferences.DatabaseSaveType saveType = SavePreferences.DatabaseSaveType.ALL;
        Boolean takeMetadataSaveOrderInAccount = false;
        Boolean reformatFile = preferences.getBoolean(JabRefPreferences.REFORMAT_FILE_ON_SAVE_AND_EXPORT);
        LatexFieldFormatterPreferences latexFieldFormatterPreferences = preferences.getLatexFieldFormatterPreferences();
        GlobalBibtexKeyPattern globalCiteKeyPattern =  preferences.getKeyPattern();
        return new SavePreferences(saveInOriginalOrder, saveOrder, encoding, makeBackup, saveType,
                takeMetadataSaveOrderInAccount, reformatFile, latexFieldFormatterPreferences, globalCiteKeyPattern);
    }

    public static SavePreferences loadForSaveFromPreferences(JabRefPreferences preferences) {
        Boolean saveInOriginalOrder = false;
        SaveOrderConfig saveOrder = null;
        Charset encoding = preferences.getDefaultEncoding();
        Boolean makeBackup = preferences.getBoolean(JabRefPreferences.BACKUP);
        SavePreferences.DatabaseSaveType saveType = SavePreferences.DatabaseSaveType.ALL;
        Boolean takeMetadataSaveOrderInAccount = true;
        Boolean reformatFile = preferences.getBoolean(JabRefPreferences.REFORMAT_FILE_ON_SAVE_AND_EXPORT);
        LatexFieldFormatterPreferences latexFieldFormatterPreferences = preferences.getLatexFieldFormatterPreferences();
        GlobalBibtexKeyPattern globalCiteKeyPattern =  preferences.getKeyPattern();
        return new SavePreferences(saveInOriginalOrder, saveOrder, encoding, makeBackup, saveType,
                takeMetadataSaveOrderInAccount, reformatFile, latexFieldFormatterPreferences, globalCiteKeyPattern);
    }

}
