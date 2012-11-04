package net.sf.jabref.specialfields;

import javax.swing.SwingUtilities;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.DatabaseChangeEvent;
import net.sf.jabref.DatabaseChangeListener;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRef;
import net.sf.jabref.undo.NamedCompound;

public class SpecialFieldDatabaseChangeListener implements
		DatabaseChangeListener {
	
	private static SpecialFieldDatabaseChangeListener INSTANCE = null;

	public void databaseChanged(DatabaseChangeEvent e) {
		if ((e.getType() == DatabaseChangeEvent.ChangeType.ADDED_ENTRY) &&
		    SpecialFieldsUtils.keywordSyncEnabled()) {
			final BibtexEntry entry = e.getEntry();
			// NamedCompount code similar to SpecialFieldUpdateListener
			NamedCompound nc = new NamedCompound(Globals.lang("Synchronized special fields based on keywords"));
			SpecialFieldsUtils.syncSpecialFieldsFromKeywords(entry, nc);
            nc.end();
			JabRef.jrf.basePanel().undoManager.addEdit(nc);
		}
	}
	
	public static SpecialFieldDatabaseChangeListener getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new SpecialFieldDatabaseChangeListener();
		}
		return INSTANCE;
	}

}
