package net.sf.jabref.exporter;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.logic.config.SaveOrderConfig;

import java.nio.charset.Charset;

public class SavePreferences {

    private boolean saveInOriginalOrder;
    private SaveOrderConfig saveOrder;
    private Charset encoding;
    private boolean makeBackup;
    private DatabaseSaveType saveType;
    private boolean takeMetadataSaveOrderInAccount;

    public SavePreferences() {

    }

    public SavePreferences(Boolean saveInOriginalOrder, SaveOrderConfig saveOrder, Charset encoding, Boolean makeBackup,
            DatabaseSaveType saveType, Boolean takeMetadataSaveOrderInAccount) {
        this.saveInOriginalOrder = saveInOriginalOrder;
        this.saveOrder = saveOrder;
        this.encoding = encoding;
        this.makeBackup = makeBackup;
        this.saveType = saveType;
        this.takeMetadataSaveOrderInAccount = takeMetadataSaveOrderInAccount;
    }

    public static SavePreferences loadForExportFromPreferences(JabRefPreferences preferences) {
        Boolean saveInOriginalOrder = Globals.prefs.getBoolean(JabRefPreferences.EXPORT_IN_ORIGINAL_ORDER);
        SaveOrderConfig saveOrder = null;
        if (!saveInOriginalOrder) {
            if (Globals.prefs.getBoolean(JabRefPreferences.EXPORT_IN_SPECIFIED_ORDER)) {
                saveOrder = SaveOrderConfig.loadExportSaveOrderFromPreferences(preferences);
            } else {
                saveOrder = SaveOrderConfig.loadTableSaveOrderFromPreferences(preferences);
            }
        }
        Charset encoding = preferences.getDefaultEncoding();
        Boolean makeBackup = preferences.getBoolean(JabRefPreferences.BACKUP);
        DatabaseSaveType saveType = DatabaseSaveType.ALL;
        Boolean takeMetadataSaveOrderInAccount = false;
        return new SavePreferences(saveInOriginalOrder, saveOrder, encoding, makeBackup, saveType,
                takeMetadataSaveOrderInAccount);
    }

    public static SavePreferences loadForSaveFromPreferences(JabRefPreferences preferences) {
        Boolean saveInOriginalOrder = true;
        SaveOrderConfig saveOrder = null;
        Charset encoding = preferences.getDefaultEncoding();
        Boolean makeBackup = preferences.getBoolean(JabRefPreferences.BACKUP);
        DatabaseSaveType saveType = DatabaseSaveType.ALL;
        Boolean takeMetadataSaveOrderInAccount = true;
        return new SavePreferences(saveInOriginalOrder, saveOrder, encoding, makeBackup, saveType,
                takeMetadataSaveOrderInAccount);
    }

    public Boolean getTakeMetadataSaveOrderInAccount() {
        return takeMetadataSaveOrderInAccount;
    }

    public SaveOrderConfig getSaveOrder() {
        return saveOrder;
    }

    public boolean isSaveInOriginalOrder() {
        return saveInOriginalOrder;
    }

    public void setSaveInOriginalOrder(Boolean saveInOriginalOrder) {
        this.saveInOriginalOrder = saveInOriginalOrder;
    }

    public boolean getMakeBackup() {
        return makeBackup;
    }

    public void setMakeBackup(Boolean makeBackup) {
        this.makeBackup = makeBackup;
    }

    public Charset getEncoding() {
        return encoding;
    }

    public void setEncoding(Charset encoding) {
        this.encoding = encoding;
    }

    public DatabaseSaveType getSaveType() {
        return saveType;
    }

    public void setSaveType(DatabaseSaveType saveType) {
        this.saveType = saveType;
    }

    public enum DatabaseSaveType {
        ALL,
        PLAIN_BIBTEX
    }
}
