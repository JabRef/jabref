package net.sf.jabref.gui.undo;

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.event.EntryEventSource;
import net.sf.jabref.model.strings.StringUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class represents the removal of an entry. The constructor needs
 * references to the database, the entry, and the map of open entry editors.
 * The latter to be able to close the entry's editor if it is opened after
 * an undo, and the removal is then undone.
 */
public class UndoableRemoveEntry extends AbstractUndoableJabRefEdit {

    private final BibDatabase base;
    private final BibEntry entry;
    private final BasePanel panel;

    private static final Log LOGGER = LogFactory.getLog(UndoableRemoveEntry.class);

    public UndoableRemoveEntry(BibDatabase base, BibEntry entry,
                               BasePanel panel) {
        this.base = base;
        this.entry = entry;
        this.panel = panel;
    }

    @Override
    public String getPresentationName() {
        return Localization.lang("remove entry %0",
                StringUtil.boldHTML(entry.getCiteKeyOptional().orElse(Localization.lang("undefined"))));
    }

    @Override
    public void undo() {
        super.undo();
        base.insertEntry(entry, EntryEventSource.UNDO);
    }

    @Override
    public void redo() {
        super.redo();

        // Redo the change.
        try {
            base.removeEntry(entry);
            // If the entry has an editor currently open, we must close it.
            panel.ensureNotShowingBottomPanel(entry);
        } catch (Throwable ex) {
            LOGGER.warn("Problem to redo `remove entry`", ex);
        }
    }

}
