package net.sf.jabref.gui.journals;

import java.util.List;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.gui.worker.AbstractWorker;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.InternalBibtexFields;

/**
 * Converts journal full names to either iso or medline abbreviations for all selected entries.
 */
public class AbbreviateAction extends AbstractWorker {

    private final BasePanel panel;
    private String message = "";
    private final boolean iso;


    public AbbreviateAction(BasePanel panel, boolean iso) {
        this.panel = panel;
        this.iso = iso;
    }

    @Override
    public void init() {
        panel.output(Localization.lang("Abbreviating..."));
    }

    @Override
    public void run() {
        List<BibEntry> entries = panel.getSelectedEntries();
        if (entries == null) {
            return;
        }

        UndoableAbbreviator undoableAbbreviator = new UndoableAbbreviator(Globals.journalAbbreviationLoader
                .getRepository(Globals.prefs.getJournalAbbreviationPreferences()), iso);

        NamedCompound ce = new NamedCompound(Localization.lang("Abbreviate journal names"));
        int count = 0;
        for (BibEntry entry : entries) {
            for (String journalField : InternalBibtexFields.getJournalNameFields()) {
                if (undoableAbbreviator.abbreviate(panel.getDatabase(), entry, journalField, ce)) {
                    count++;
                }
            }
        }

        if (count > 0) {
            ce.end();
            panel.getUndoManager().addEdit(ce);
            panel.markBaseChanged();
            message = Localization.lang("Abbreviated %0 journal names.", String.valueOf(count));
        } else {
            message = Localization.lang("No journal names could be abbreviated.");
        }
    }

    @Override
    public void update() {
        panel.output(message);
    }
}
