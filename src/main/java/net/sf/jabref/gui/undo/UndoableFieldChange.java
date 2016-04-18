/*  Copyright (C) 2003-2011 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.gui.undo;

import javax.swing.undo.AbstractUndoableEdit;

import net.sf.jabref.logic.FieldChange;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class represents a change in any field value. The relevant
 * information is the BibEntry, the field name, the old and the
 * new value. Old/new values can be null.
 */
public class UndoableFieldChange extends AbstractUndoableEdit {
    private static final Log LOGGER = LogFactory.getLog(UndoableFieldChange.class);

    private final BibEntry entry;
    private final String field;
    private final String oldValue;
    private final String newValue;


    public UndoableFieldChange(BibEntry entry, String field,
            String oldValue, String newValue) {
        this.entry = entry;
        this.field = field;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public UndoableFieldChange(FieldChange change) {
        this(change.getEntry(), change.getField(), change.getOldValue(), change.getNewValue());
    }

    @Override
    public String getPresentationName() {
        return Localization.lang("change field");
    }

    @Override
    public String getUndoPresentationName() {
        return Localization.lang("Undo") + ": " +
                Localization.lang("change field");
    }

    @Override
    public String getRedoPresentationName() {
        return Localization.lang("Redo") + ": " +
                Localization.lang("change field");
    }

    @Override
    public void undo() {
        super.undo();

        // Revert the change.
        try {
            if (oldValue == null) {
                entry.clearField(field);
            } else {
                entry.setField(field, oldValue);
            }

            // this is the only exception explicitly thrown here
        } catch (IllegalArgumentException ex) {
            LOGGER.info("Cannot perform undo", ex);
        }
    }

    @Override
    public void redo() {
        super.redo();

        // Redo the change.
        try {
            if (newValue == null) {
                entry.clearField(field);
            } else {
                entry.setField(field, newValue);
            }

        } catch (IllegalArgumentException ex) {
            LOGGER.info("Cannot perform redo", ex);
        }
    }

}
