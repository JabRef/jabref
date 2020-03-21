package org.jabref.gui.specialfields;

import org.jabref.model.database.event.EntriesAddedEvent;

import com.google.common.eventbus.Subscribe;

public enum SpecialFieldDatabaseChangeListener {

    INSTANCE;

    @Subscribe
    public void listen(EntriesAddedEvent event) {
        // TODO
        /*
        if (!Globals.prefs.isKeywordSyncEnabled()) {
            return;
        }

        final List<BibEntry> entries = event.getBibEntries();
        // NamedCompound code similar to SpecialFieldUpdateListener
        NamedCompound nc = new NamedCompound(Localization.lang("Synchronized special fields based on keywords"));
        for (BibEntry entry : entries) {
            List<FieldChange> changes = SpecialFieldsUtils.syncSpecialFieldsFromKeywords(entry, Globals.prefs.getKeywordDelimiter());
            for (FieldChange change : changes) {
                nc.addEdit(new UndoableFieldChange(change));
            }
        }
        // Don't insert the compound into the undoManager,
        // it would be added before the component which undoes the insertion of the entry and creates heavy problems
        // (which prohibits the undo the deleting multiple entries)

        // See if the above is still true after EntryAddedEvent changed to EntriesAddedEvent
        */
    }
}
