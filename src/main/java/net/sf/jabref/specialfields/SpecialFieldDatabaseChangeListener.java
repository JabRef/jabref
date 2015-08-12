/**
 * Copyright (C) 2015 JabRef contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.specialfields;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.DatabaseChangeEvent;
import net.sf.jabref.DatabaseChangeListener;
import net.sf.jabref.JabRef;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.logic.l10n.Localization;

public class SpecialFieldDatabaseChangeListener implements
        DatabaseChangeListener {

    private static SpecialFieldDatabaseChangeListener INSTANCE = null;


    @Override
    public void databaseChanged(DatabaseChangeEvent e) {
        if (e.getType() == DatabaseChangeEvent.ChangeType.ADDED_ENTRY &&
                SpecialFieldsUtils.keywordSyncEnabled()) {
            final BibtexEntry entry = e.getEntry();
            // NamedCompount code similar to SpecialFieldUpdateListener
            NamedCompound nc = new NamedCompound(Localization.lang("Synchronized special fields based on keywords"));
            SpecialFieldsUtils.syncSpecialFieldsFromKeywords(entry, nc);
            nc.end();
            JabRef.jrf.basePanel().undoManager.addEdit(nc);
        }
    }

    public static SpecialFieldDatabaseChangeListener getInstance() {
        if (SpecialFieldDatabaseChangeListener.INSTANCE == null) {
            SpecialFieldDatabaseChangeListener.INSTANCE = new SpecialFieldDatabaseChangeListener();
        }
        return SpecialFieldDatabaseChangeListener.INSTANCE;
    }

}
