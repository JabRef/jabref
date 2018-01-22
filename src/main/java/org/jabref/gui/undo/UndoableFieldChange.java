package org.jabref.gui.undo;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.strings.StringUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class represents a change in any field value. The relevant
 * information is the BibEntry, the field name, the old and the
 * new value. Old/new values can be null.
 */
public class UndoableFieldChange extends AbstractUndoableJabRefEdit {
    private static final Logger LOGGER = LoggerFactory.getLogger(UndoableFieldChange.class);

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
        return Localization.lang("change field %0 of entry %1 from %2 to %3", StringUtil.boldHTML(field),
                StringUtil.boldHTML(entry.getCiteKeyOptional().orElse(Localization.lang("undefined"))),
                StringUtil.boldHTML(oldValue, Localization.lang("undefined")),
                StringUtil.boldHTML(newValue, Localization.lang("undefined")));
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
