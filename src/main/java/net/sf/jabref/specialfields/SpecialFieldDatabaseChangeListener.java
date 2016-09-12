package net.sf.jabref.specialfields;

import net.sf.jabref.Globals;
import net.sf.jabref.logic.specialfields.SpecialFieldsUtils;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.event.EntryAddedEvent;

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
        if (Globals.prefs.isKeywordSyncEnabled()) {
            final BibEntry entry = event.getBibEntry();
            SpecialFieldsUtils.syncSpecialFieldsFromKeywords(entry);
        }
    }

}
