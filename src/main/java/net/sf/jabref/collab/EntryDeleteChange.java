package net.sf.jabref.collab;

import javax.swing.JComponent;
import javax.swing.JScrollPane;

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.PreviewPanel;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.gui.undo.UndoableRemoveEntry;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.DuplicateCheck;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class EntryDeleteChange extends Change {

    private final BibEntry memEntry;
    private final BibEntry tmpEntry;
    private final JScrollPane sp;

    private static final Log LOGGER = LogFactory.getLog(EntryDeleteChange.class);


    public EntryDeleteChange(BibEntry memEntry, BibEntry tmpEntry) {
        super(Localization.lang("Deleted entry"));
        this.memEntry = memEntry;
        this.tmpEntry = tmpEntry;

        // Compare the deleted entry in memory with the one in the tmpfile. The
        // entry could have been removed in memory.
        double matchWithTmp = DuplicateCheck.compareEntriesStrictly(memEntry, tmpEntry);

        // Check if it has been modified locally, since last tempfile was saved.
        boolean isModifiedLocally = (matchWithTmp <= 1);

        LOGGER.debug("Modified entry: " + memEntry.getCiteKeyOptional().orElse("<no BibTeX key set>")
                + "\n Modified locally: " + isModifiedLocally);

        PreviewPanel pp = new PreviewPanel(null, memEntry, null);
        sp = new JScrollPane(pp);
    }

    @Override
    public boolean makeChange(BasePanel panel, BibDatabase secondary, NamedCompound undoEdit) {
        panel.getDatabase().removeEntry(memEntry);
        undoEdit.addEdit(new UndoableRemoveEntry(panel.getDatabase(), memEntry, panel));
        secondary.removeEntry(tmpEntry);
        return true;
    }

    @Override
    public JComponent description() {
        return sp;
    }
}
