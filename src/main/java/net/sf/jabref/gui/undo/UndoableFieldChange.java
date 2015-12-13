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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.jabref.model.entry.BibtexEntry;

/**
 * This class represents a change in any field value. The relevant
 * information is the BibtexEntry, the field name, the old and the
 * new value. Old/new values can be null.
 */
public class UndoableFieldChange extends AbstractUndoableEdit {
    private static final Log LOGGER = LogFactory.getLog(UndoableFieldChange.class);

    private final BibtexEntry entry;
    private final String field;
    private final String oldValue;
    private final String newValue;


    public UndoableFieldChange(BibtexEntry entry, String field,
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
        // @formatter:off
        return Localization.lang("Undo") + ": " +
                Localization.lang("change field");
        // @formatter:on
    }

    @Override
    public String getRedoPresentationName() {
        // @formatter:off
        return Localization.lang("Redo") + ": " +
                Localization.lang("change field");
        // @formatter:on
    }

    @Override
    public void undo() {
        super.undo();

        // Revert the change.
        try {
            if (oldValue != null) {
                entry.setField(field, oldValue);
            } else {
                entry.clearField(field);
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
            if (newValue != null) {
                entry.setField(field, newValue);
            } else {
                entry.clearField(field);
            }

        } catch (IllegalArgumentException ex) {
            LOGGER.info("Cannot perform redo", ex);
        }
    }

}
