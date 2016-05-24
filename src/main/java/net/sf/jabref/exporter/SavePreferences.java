package net.sf.jabref.exporter;

import java.nio.charset.Charset;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.logic.config.SaveOrderConfig;

public class SavePreferences {

    private final boolean reformatFile;
    private final boolean saveInOriginalOrder;
    private final SaveOrderConfig saveOrder;
    private final Charset encoding;
    private final boolean makeBackup;
    private final DatabaseSaveType saveType;
    private final boolean takeMetadataSaveOrderInAccount;

    public SavePreferences() {
        this(true, null, null, false, DatabaseSaveType.ALL, true, false);
    }

    public SavePreferences(Boolean saveInOriginalOrder, SaveOrderConfig saveOrder, Charset encoding, Boolean makeBackup,
            DatabaseSaveType saveType, Boolean takeMetadataSaveOrderInAccount, Boolean reformatFile) {
        this.saveInOriginalOrder = saveInOriginalOrder;
        this.saveOrder = saveOrder;
        this.encoding = encoding;
        this.makeBackup = makeBackup;
        this.saveType = saveType;
        this.takeMetadataSaveOrderInAccount = takeMetadataSaveOrderInAccount;
        this.reformatFile = reformatFile;
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
        Boolean reformatFile = preferences.getBoolean(JabRefPreferences.REFORMAT_FILE_ON_SAVE_AND_EXPORT);
        return new SavePreferences(saveInOriginalOrder, saveOrder, encoding, makeBackup, saveType,
                takeMetadataSaveOrderInAccount, reformatFile);
    }

    public static SavePreferences loadForSaveFromPreferences(JabRefPreferences preferences) {
        Boolean saveInOriginalOrder = false;
        SaveOrderConfig saveOrder = null;
        Charset encoding = preferences.getDefaultEncoding();
        Boolean makeBackup = preferences.getBoolean(JabRefPreferences.BACKUP);
        DatabaseSaveType saveType = DatabaseSaveType.ALL;
        Boolean takeMetadataSaveOrderInAccount = true;
        Boolean reformatFile = preferences.getBoolean(JabRefPreferences.REFORMAT_FILE_ON_SAVE_AND_EXPORT);
        return new SavePreferences(saveInOriginalOrder, saveOrder, encoding, makeBackup, saveType,
                takeMetadataSaveOrderInAccount, reformatFile);
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

    public SavePreferences withSaveInOriginalOrder(Boolean saveInOriginalOrder) {
        return new SavePreferences(saveInOriginalOrder, this.saveOrder, this.encoding, this.makeBackup, this.saveType,
                this.takeMetadataSaveOrderInAccount, this.reformatFile);
    }

    public boolean getMakeBackup() {
        return makeBackup;
    }

    public SavePreferences withMakeBackup(Boolean makeBackup) {
        return new SavePreferences(this.saveInOriginalOrder, this.saveOrder, this.encoding, makeBackup, this.saveType,
                this.takeMetadataSaveOrderInAccount, this.reformatFile);
    }

    public Charset getEncoding() {
        return encoding;
    }

    public SavePreferences withEncoding(Charset encoding) {
        return new SavePreferences(this.saveInOriginalOrder, this.saveOrder, encoding, this.makeBackup, this.saveType,
                this.takeMetadataSaveOrderInAccount, this.reformatFile);
    }

    public DatabaseSaveType getSaveType() {
        return saveType;
    }

    public SavePreferences withSaveType(DatabaseSaveType saveType) {
        return new SavePreferences(this.saveInOriginalOrder, this.saveOrder, this.encoding, this.makeBackup, saveType,
                this.takeMetadataSaveOrderInAccount, this.reformatFile);
    }

    public Boolean isReformatFile() {
        return reformatFile;
    }

    public SavePreferences withReformatFile(boolean reformatFile) {
        return new SavePreferences(this.saveInOriginalOrder, this.saveOrder, this.encoding, this.makeBackup,
                this.saveType, this.takeMetadataSaveOrderInAccount, reformatFile);
    }

    public enum DatabaseSaveType {
        ALL,
        PLAIN_BIBTEX
    }
}
