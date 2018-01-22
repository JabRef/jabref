package org.jabref.gui.undo;

import org.jabref.gui.BasePanel;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.strings.StringUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class represents the removal of an entry. The constructor needs
 * references to the database, the entry, and the map of open entry editors.
 * The latter to be able to close the entry's editor if it is opened before
 * the insert is undone.
 */
public class UndoableInsertEntry extends AbstractUndoableJabRefEdit {

    private static final Logger LOGGER = LoggerFactory.getLogger(UndoableInsertEntry.class);
    private final BibDatabase base;
    private final BibEntry entry;

    private final BasePanel panel;

    public UndoableInsertEntry(BibDatabase base, BibEntry entry,
                               BasePanel panel) {
        this.base = base;
        this.entry = entry;
        this.panel = panel;
    }

    @Override
    public String getPresentationName() {
        return Localization.lang("insert entry %0",
                StringUtil.boldHTML(entry.getCiteKeyOptional().orElse(Localization.lang("undefined"))));
    }

    @Override
    public void undo() {
        super.undo();

        // Revert the change.
        try {
            base.removeEntry(entry);
            // If the entry has an editor currently open, we must close it.
            panel.ensureNotShowingBottomPanel(entry);
        } catch (Throwable ex) {
            LOGGER.warn("Problem to undo `insert entry`", ex);
        }
    }

    @Override
    public void redo() {
        super.redo();
        base.insertEntry(entry);
    }

}
