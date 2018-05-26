package org.jabref.gui.specialfields;

import org.jabref.model.database.event.EntryAddedEvent;

import com.google.common.eventbus.Subscribe;

public enum SpecialFieldDatabaseChangeListener {

    INSTANCE;

    @Subscribe
    public void listen(EntryAddedEvent event) {
        // TODO
    }
    /*
    if (!Globals.prefs.isKeywordSyncEnabled()) {
        return;
    }

    final BibEntry entry = event.getBibEntry();
    // NamedCompount code similar to SpecialFieldUpdateListener
    NamedCompound nc = new NamedCompound(Localization.lang("Synchronized special fields based on keywords"));
    List<FieldChange> changes = SpecialFieldsUtils.syncSpecialFieldsFromKeywords(entry, Globals.prefs.getKeywordDelimiter());
    for (FieldChange change: changes) {
        nc.addEdit(new UndoableFieldChange(change));
    }
    // Don't insert the compound into the undoManager,
    // it would be added before the component which undoes the insertion of the entry and creates heavy problems
    // (which prohibits the undo the deleting multiple entries)
    */
}
