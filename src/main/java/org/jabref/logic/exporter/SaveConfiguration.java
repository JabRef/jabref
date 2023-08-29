package org.jabref.logic.exporter;

import org.jabref.model.metadata.SaveOrder;

public class SaveConfiguration {

    // Encoding written at the top of the .bib file.
    public static final String ENCODING_PREFIX = "Encoding: ";

    private boolean reformatFile;
    private SaveOrder saveOrder;
    private boolean makeBackup;
    private BibDatabaseWriter.SaveType saveType;
    private boolean useMetadataSaveOrder;

    public SaveConfiguration(SaveOrder saveOrder,
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

    public SaveConfiguration() {
        this(SaveOrder.getDefaultSaveOrder(),
                false,
                BibDatabaseWriter.SaveType.ALL,
                true,
                false);
    }

    public boolean useMetadataSaveOrder() {
        return useMetadataSaveOrder;
    }

    public SaveConfiguration withMetadataSaveOrder(boolean newTakeMetadataSaveOrderInAccount) {
        this.useMetadataSaveOrder = newTakeMetadataSaveOrderInAccount;
        return this;
    }

    public SaveOrder getSaveOrder() {
        return saveOrder;
    }

    public SaveConfiguration withSaveOrder(SaveOrder newSaveOrder) {
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
    public SaveConfiguration withMakeBackup(Boolean newMakeBackup) {
        this.makeBackup = newMakeBackup;
        return this;
    }

    public BibDatabaseWriter.SaveType getSaveType() {
        return saveType;
    }

    public SaveConfiguration withSaveType(BibDatabaseWriter.SaveType newSaveType) {
        this.saveType = newSaveType;
        return this;
    }

    public boolean shouldReformatFile() {
        return reformatFile;
    }

    public SaveConfiguration withReformatOnSave(boolean newReformat) {
        this.reformatFile = newReformat;
        return this;
    }
}
