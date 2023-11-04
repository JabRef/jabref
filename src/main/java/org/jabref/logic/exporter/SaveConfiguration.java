package org.jabref.logic.exporter;

import org.jabref.gui.autosaveandbackup.BackupManager;
import org.jabref.model.metadata.SaveOrder;

public class SaveConfiguration {

    // Encoding written at the top of the .bib file.
    public static final String ENCODING_PREFIX = "Encoding: ";

    private boolean reformatFile;
    private SaveOrder saveOrder;
    private boolean makeBackup;
    private BibDatabaseWriter.SaveType saveType;

    public SaveConfiguration(SaveOrder saveOrder,
                             Boolean makeBackup,
                             BibDatabaseWriter.SaveType saveType,
                             Boolean reformatFile) {
        this.saveOrder = saveOrder;
        this.makeBackup = makeBackup;
        this.saveType = saveType;
        this.reformatFile = reformatFile;
    }

    public SaveConfiguration() {
        this(SaveOrder.getDefaultSaveOrder(),
                false,
                BibDatabaseWriter.SaveType.WITH_JABREF_META_DATA,
                false);
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
     * Required by {@link BackupManager}. Should not be used in other settings
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
