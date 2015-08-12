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
package net.sf.jabref.gui.journals;

import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.logic.journals.Abbreviation;
import net.sf.jabref.logic.journals.JournalAbbreviationRepository;
import net.sf.jabref.gui.undo.UndoableFieldChange;

import javax.swing.undo.CompoundEdit;

public class UndoableUnabbreviator {

    private final JournalAbbreviationRepository journalAbbreviationRepository;

    public UndoableUnabbreviator(JournalAbbreviationRepository journalAbbreviationRepository) {
        this.journalAbbreviationRepository = journalAbbreviationRepository;
    }

    /**
     * Unabbreviate the journal name of the given entry.
     *
     * @param entry     The entry to be treated.
     * @param fieldName The field name (e.g. "journal")
     * @param ce        If the entry is changed, add an edit to this compound.
     * @return true if the entry was changed, false otherwise.
     */
    public boolean unabbreviate(BibtexDatabase database, BibtexEntry entry,
                                String fieldName, CompoundEdit ce) {
        String text = entry.getField(fieldName);
        if (text == null) {
            return false;
        }
        String origText = text;
        if (database != null) {
            text = database.resolveForStrings(text);
        }

        if (!journalAbbreviationRepository.isKnownName(text)) {
            return false; // cannot do anything if it is not known
        }

        if (!journalAbbreviationRepository.isAbbreviatedName(text)) {
            return false; // cannot unabbreviate unabbreviated name.
        }

        Abbreviation abbreviation = journalAbbreviationRepository.getAbbreviation(text).get(); // must be here
        String newText = abbreviation.getName();
        entry.setField(fieldName, newText);
        ce.addEdit(new UndoableFieldChange(entry, fieldName, origText, newText));
        return true;
    }

}
