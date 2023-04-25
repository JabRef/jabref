package org.jabref.logic.exporter;

import org.jabref.model.metadata.SaveOrder;

public class SavePreferences {

    // Encoding written at the top of the .bib file.
    public static final String ENCODING_PREFIX = "Encoding: ";

    private final boolean reformatFile;
    private SaveOrder saveOrder;
    private boolean makeBackup;
    private BibDatabaseWriter.SaveType saveType;
    private boolean useMetadataSaveOrder;

    public SavePreferences(SaveOrder saveOrder,
                           Boolean makeBackup,
                           BibDatabaseWriter.SaveType saveType,
                           Boolean useMetadataSaveOrder,
                           Boolean reformatFile) {
        this.saveOrder = saveOrder;
        this.makeBackup = makeBackup;
        this.saveType = saveType;
        this.useMetadataSaveOrder = useMetadataSaveOrder;
        this.reformatFile = reformatFile;
    }

    public boolean useMetadataSaveOrder() {
        return useMetadataSaveOrder;
    }

    public SavePreferences withMetadataSaveOrder(boolean newTakeMetadataSaveOrderInAccount) {
        this.useMetadataSaveOrder = newTakeMetadataSaveOrderInAccount;
        return this;
    }

    public SaveOrder getSaveOrder() {
        return saveOrder;
    }

    public SavePreferences withSaveOrder(SaveOrder newSaveOrder) {
        this.saveOrder = newSaveOrder;
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

    public BibDatabaseWriter.SaveType getSaveType() {
        return saveType;
    }

    public SavePreferences withSaveType(BibDatabaseWriter.SaveType newSaveType) {
        this.saveType = newSaveType;
        return this;
    }

    public boolean shouldReformatFile() {
        return reformatFile;
    }
}
