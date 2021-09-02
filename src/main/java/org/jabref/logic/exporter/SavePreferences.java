package org.jabref.logic.exporter;

import org.jabref.logic.bibtex.FieldWriterPreferences;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.model.metadata.SaveOrderConfig;

public class SavePreferences {

    public enum DatabaseSaveType { ALL, PLAIN_BIBTEX }

    // Encoding written at the top of the .bib file.
    public static final String ENCODING_PREFIX = "Encoding: ";

    private final boolean reformatFile;
    private boolean saveInOriginalOrder;
    private SaveOrderConfig saveOrder;
    private boolean makeBackup;
    private DatabaseSaveType saveType;
    private boolean takeMetadataSaveOrderInAccount;
    private final FieldWriterPreferences fieldWriterPreferences;
    private final CitationKeyPatternPreferences citationKeyPatternPreferences;

    private SavePreferences(Boolean saveInOriginalOrder,
                            SaveOrderConfig saveOrder,
                            Boolean makeBackup,
                            DatabaseSaveType saveType,
                            Boolean takeMetadataSaveOrderInAccount,
                            Boolean reformatFile,
                            FieldWriterPreferences fieldWriterPreferences,
                            CitationKeyPatternPreferences citationKeyPatternPreferences) {

        this.saveInOriginalOrder = saveInOriginalOrder;
        this.saveOrder = saveOrder;
        this.makeBackup = makeBackup;
        this.saveType = saveType;
        this.takeMetadataSaveOrderInAccount = takeMetadataSaveOrderInAccount;
        this.reformatFile = reformatFile;
        this.fieldWriterPreferences = fieldWriterPreferences;
        this.citationKeyPatternPreferences = citationKeyPatternPreferences;
    }

    public SavePreferences(Boolean saveInOriginalOrder,
                           SaveOrderConfig saveOrder,
                           DatabaseSaveType saveType,
                           Boolean takeMetadataSaveOrderInAccount,
                           Boolean reformatFile,
                           FieldWriterPreferences fieldWriterPreferences,
                           CitationKeyPatternPreferences citationKeyPatternPreferences) {

        this(saveInOriginalOrder,
                saveOrder,
                true,
                saveType,
                takeMetadataSaveOrderInAccount,
                reformatFile,
                fieldWriterPreferences,
                citationKeyPatternPreferences);
    }

    public boolean takeMetadataSaveOrderInAccount() {
        return takeMetadataSaveOrderInAccount;
    }

    public SavePreferences withTakeMetadataSaveOrderInAccount(boolean newTakeMetadataSaveOrderInAccount) {
        this.takeMetadataSaveOrderInAccount = newTakeMetadataSaveOrderInAccount;
        return this;
    }

    public SaveOrderConfig getSaveOrder() {
        return saveOrder;
    }

    public SavePreferences withSaveOrder(SaveOrderConfig newSaveOrder) {
        this.saveOrder = newSaveOrder;
        return this;
    }

    public boolean shouldSaveInOriginalOrder() {
        return saveInOriginalOrder;
    }

    public SavePreferences withSaveInOriginalOrder(Boolean newSaveInOriginalOrder) {
        this.saveInOriginalOrder = newSaveInOriginalOrder;
        return this;
    }

    public boolean shouldMakeBackup() {
        return makeBackup;
    }

    /**
     * Required by {@link org.jabref.logic.autosaveandbackup.BackupManager}. Should not be used in other settings
     *
     * @param newMakeBackup whether a backup (.bak file) should be made
     */
    public SavePreferences withMakeBackup(Boolean newMakeBackup) {
        this.makeBackup = newMakeBackup;
        return this;
    }

    public DatabaseSaveType getSaveType() {
        return saveType;
    }

    public SavePreferences withSaveType(DatabaseSaveType newSaveType) {
        this.saveType = newSaveType;
        return this;
    }

    public boolean shouldReformatFile() {
        return reformatFile;
    }

    public FieldWriterPreferences getFieldWriterPreferences() {
        return fieldWriterPreferences;
    }

    public CitationKeyPatternPreferences getCitationKeyPatternPreferences() {
        return citationKeyPatternPreferences;
    }
}
