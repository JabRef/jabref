package org.jabref.logic.exporter;

import java.nio.charset.Charset;

import org.jabref.logic.bibtex.FieldWriterPreferences;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.model.metadata.SaveOrderConfig;

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
    private final FieldWriterPreferences fieldWriterPreferences;
    private final CitationKeyPatternPreferences citationKeyPatternPreferences;

    private SavePreferences(Boolean saveInOriginalOrder,
                            SaveOrderConfig saveOrder,
                            Charset encoding,
                            Boolean makeBackup,
                            DatabaseSaveType saveType,
                            Boolean takeMetadataSaveOrderInAccount,
                            Boolean reformatFile,
                            FieldWriterPreferences fieldWriterPreferences,
                            CitationKeyPatternPreferences citationKeyPatternPreferences) {

        this.saveInOriginalOrder = saveInOriginalOrder;
        this.saveOrder = saveOrder;
        this.encoding = encoding;
        this.makeBackup = makeBackup;
        this.saveType = saveType;
        this.takeMetadataSaveOrderInAccount = takeMetadataSaveOrderInAccount;
        this.reformatFile = reformatFile;
        this.fieldWriterPreferences = fieldWriterPreferences;
        this.citationKeyPatternPreferences = citationKeyPatternPreferences;
    }

    public SavePreferences(Boolean saveInOriginalOrder,
                           SaveOrderConfig saveOrder,
                           Charset encoding,
                           DatabaseSaveType saveType,
                           Boolean takeMetadataSaveOrderInAccount,
                           Boolean reformatFile,
                           FieldWriterPreferences fieldWriterPreferences,
                           CitationKeyPatternPreferences citationKeyPatternPreferences) {

        this(saveInOriginalOrder,
                saveOrder,
                encoding,
                true,
                saveType,
                takeMetadataSaveOrderInAccount,
                reformatFile,
                fieldWriterPreferences,
                citationKeyPatternPreferences);
    }

    public Boolean takeMetadataSaveOrderInAccount() {
        return takeMetadataSaveOrderInAccount;
    }

    public SaveOrderConfig getSaveOrder() {
        return saveOrder;
    }

    public boolean isSaveInOriginalOrder() {
        return saveInOriginalOrder;
    }

    public SavePreferences withSaveInOriginalOrder(Boolean newSaveInOriginalOrder) {
        return new SavePreferences(
                newSaveInOriginalOrder,
                this.saveOrder,
                this.encoding,
                this.makeBackup,
                this.saveType,
                this.takeMetadataSaveOrderInAccount,
                this.reformatFile,
                this.fieldWriterPreferences,
                this.citationKeyPatternPreferences);
    }

    public boolean makeBackup() {
        return makeBackup;
    }

    /**
     * Required by {@link org.jabref.logic.autosaveandbackup.BackupManager}. Should not be used in other settings
     *
     * @param newMakeBackup whether a backup (.bak file) should be made
     */
    public SavePreferences withMakeBackup(Boolean newMakeBackup) {
        return new SavePreferences(
                this.saveInOriginalOrder,
                this.saveOrder,
                this.encoding,
                newMakeBackup,
                this.saveType,
                this.takeMetadataSaveOrderInAccount,
                this.reformatFile,
                this.fieldWriterPreferences,
                this.citationKeyPatternPreferences);
    }

    public Charset getEncoding() {
        return encoding;
    }

    public SavePreferences withEncoding(Charset newEncoding) {
        return new SavePreferences(
                this.saveInOriginalOrder,
                this.saveOrder,
                newEncoding,
                this.makeBackup,
                this.saveType,
                this.takeMetadataSaveOrderInAccount,
                this.reformatFile,
                this.fieldWriterPreferences,
                this.citationKeyPatternPreferences);
    }

    public DatabaseSaveType getSaveType() {
        return saveType;
    }

    public SavePreferences withSaveType(DatabaseSaveType newSaveType) {
        return new SavePreferences(
                this.saveInOriginalOrder,
                this.saveOrder,
                this.encoding,
                this.makeBackup,
                newSaveType,
                this.takeMetadataSaveOrderInAccount,
                this.reformatFile,
                this.fieldWriterPreferences,
                this.citationKeyPatternPreferences);
    }

    public Boolean isReformatFile() {
        return reformatFile;
    }

    public SavePreferences withReformatFile(boolean newReformatFile) {
        return new SavePreferences(
                this.saveInOriginalOrder,
                this.saveOrder,
                this.encoding,
                this.makeBackup,
                this.saveType,
                this.takeMetadataSaveOrderInAccount,
                newReformatFile,
                this.fieldWriterPreferences,
                this.citationKeyPatternPreferences);
    }

    public FieldWriterPreferences getFieldWriterPreferences() {
        return fieldWriterPreferences;
    }

    public CitationKeyPatternPreferences getCitationKeyPatternPreferences() {
        return citationKeyPatternPreferences;
    }

    public enum DatabaseSaveType {
        ALL,
        PLAIN_BIBTEX
    }
}
