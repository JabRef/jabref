package net.sf.jabref.logic.exporter;

import java.nio.charset.Charset;

import net.sf.jabref.logic.bibtex.LatexFieldFormatterPreferences;
import net.sf.jabref.model.metadata.SaveOrderConfig;
import net.sf.jabref.preferences.JabRefPreferences;

public class SavePreferences {

    // Encoding written at the top of the .bib file.
    public static final String ENCODING_PREFIX = "Encoding: ";

    private final boolean reformatFile;
    private final boolean saveInOriginalOrder;
    private final SaveOrderConfig saveOrder;
    private final Charset encoding;
    private final boolean makeBackup;
    private final DatabaseSaveType saveType;
    private final boolean takeMetadataSaveOrderInAccount;
    private final LatexFieldFormatterPreferences latexFieldFormatterPreferences;

    public SavePreferences() {
        this(true, null, null, false, DatabaseSaveType.ALL, true, false, new LatexFieldFormatterPreferences());
    }

    public SavePreferences(Boolean saveInOriginalOrder, SaveOrderConfig saveOrder, Charset encoding, Boolean makeBackup,
            DatabaseSaveType saveType, Boolean takeMetadataSaveOrderInAccount, Boolean reformatFile,
            LatexFieldFormatterPreferences latexFieldFormatterPreferences) {
        this.saveInOriginalOrder = saveInOriginalOrder;
        this.saveOrder = saveOrder;
        this.encoding = encoding;
        this.makeBackup = makeBackup;
        this.saveType = saveType;
        this.takeMetadataSaveOrderInAccount = takeMetadataSaveOrderInAccount;
        this.reformatFile = reformatFile;
        this.latexFieldFormatterPreferences = latexFieldFormatterPreferences;
    }

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
        DatabaseSaveType saveType = DatabaseSaveType.ALL;
        Boolean takeMetadataSaveOrderInAccount = false;
        Boolean reformatFile = preferences.getBoolean(JabRefPreferences.REFORMAT_FILE_ON_SAVE_AND_EXPORT);
        LatexFieldFormatterPreferences latexFieldFormatterPreferences = preferences.getLatexFieldFormatterPreferences();
        return new SavePreferences(saveInOriginalOrder, saveOrder, encoding, makeBackup, saveType,
                takeMetadataSaveOrderInAccount, reformatFile, latexFieldFormatterPreferences);
    }

    public static SavePreferences loadForSaveFromPreferences(JabRefPreferences preferences) {
        Boolean saveInOriginalOrder = false;
        SaveOrderConfig saveOrder = null;
        Charset encoding = preferences.getDefaultEncoding();
        Boolean makeBackup = preferences.getBoolean(JabRefPreferences.BACKUP);
        DatabaseSaveType saveType = DatabaseSaveType.ALL;
        Boolean takeMetadataSaveOrderInAccount = true;
        Boolean reformatFile = preferences.getBoolean(JabRefPreferences.REFORMAT_FILE_ON_SAVE_AND_EXPORT);
        LatexFieldFormatterPreferences latexFieldFormatterPreferences = preferences.getLatexFieldFormatterPreferences();
        return new SavePreferences(saveInOriginalOrder, saveOrder, encoding, makeBackup, saveType,
                takeMetadataSaveOrderInAccount, reformatFile, latexFieldFormatterPreferences);
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

    public SavePreferences withSaveInOriginalOrder(Boolean newSaveInOriginalOrder) {
        return new SavePreferences(newSaveInOriginalOrder, this.saveOrder, this.encoding, this.makeBackup, this.saveType,
                this.takeMetadataSaveOrderInAccount, this.reformatFile, this.latexFieldFormatterPreferences);
    }

    public boolean getMakeBackup() {
        return makeBackup;
    }

    public SavePreferences withMakeBackup(Boolean newMakeBackup) {
        return new SavePreferences(this.saveInOriginalOrder, this.saveOrder, this.encoding, newMakeBackup, this.saveType,
                this.takeMetadataSaveOrderInAccount, this.reformatFile, this.latexFieldFormatterPreferences);
    }

    public Charset getEncoding() {
        return encoding;
    }

    public SavePreferences withEncoding(Charset newEncoding) {
        return new SavePreferences(this.saveInOriginalOrder, this.saveOrder, newEncoding, this.makeBackup, this.saveType,
                this.takeMetadataSaveOrderInAccount, this.reformatFile, this.latexFieldFormatterPreferences);
    }

    public DatabaseSaveType getSaveType() {
        return saveType;
    }

    public SavePreferences withSaveType(DatabaseSaveType newSaveType) {
        return new SavePreferences(this.saveInOriginalOrder, this.saveOrder, this.encoding, this.makeBackup, newSaveType,
                this.takeMetadataSaveOrderInAccount, this.reformatFile, this.latexFieldFormatterPreferences);
    }

    public Boolean isReformatFile() {
        return reformatFile;
    }

    public SavePreferences withReformatFile(boolean newReformatFile) {
        return new SavePreferences(this.saveInOriginalOrder, this.saveOrder, this.encoding, this.makeBackup,
                this.saveType, this.takeMetadataSaveOrderInAccount, newReformatFile, this.latexFieldFormatterPreferences);
    }

    public Charset getEncodingOrDefault() {
        return encoding == null ? Charset.defaultCharset() : encoding;
    }

    public LatexFieldFormatterPreferences getLatexFieldFormatterPreferences() {
        return latexFieldFormatterPreferences;
    }

    public enum DatabaseSaveType {
        ALL,
        PLAIN_BIBTEX
    }
}
