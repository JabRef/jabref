package net.sf.jabref.specialfields;

import net.sf.jabref.JabRefGUI;
import net.sf.jabref.event.AddEntryEvent;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;

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
    public void listen(AddEntryEvent addEntryEvent) {
        if (SpecialFieldsUtils.keywordSyncEnabled()) {
            final BibEntry entry = addEntryEvent.getBibEntry();
            // NamedCompount code similar to SpecialFieldUpdateListener
            NamedCompound nc = new NamedCompound(Localization.lang("Synchronized special fields based on keywords"));
            SpecialFieldsUtils.syncSpecialFieldsFromKeywords(entry, nc);
            nc.end();
            JabRefGUI.getMainFrame().getCurrentBasePanel().undoManager.addEdit(nc);
        }
    }

}
