package org.jabref.gui.specialfields;

import java.util.List;

import org.jabref.Globals;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.specialfields.SpecialFieldsUtils;
import org.jabref.model.FieldChange;
import org.jabref.model.database.event.EntryAddedEvent;
import org.jabref.model.entry.BibEntry;

import com.google.common.eventbus.Subscribe;

public class SpecialFieldDatabaseChangeListener {

    private static SpecialFieldDatabaseChangeListener INSTANCE;

    public static SpecialFieldDatabaseChangeListener getInstance() {
        if (SpecialFieldDatabaseChangeListener.INSTANCE == null) {
            SpecialFieldDatabaseChangeListener.INSTANCE = new SpecialFieldDatabaseChangeListener();
        }
        return SpecialFieldDatabaseChangeListener.INSTANCE;
    }

    @Subscribe
    public void listen(EntryAddedEvent event) {
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
    }
}
