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
 * Converts journal abbreviations back to full name for all selected entries.
 */
public class UnabbreviateAction extends AbstractWorker {

    private final BasePanel panel;
    private String message = "";


    public UnabbreviateAction(BasePanel panel) {
        this.panel = panel;
    }

    @Override
    public void init() {
        panel.output(Localization.lang("Unabbreviating..."));
    }

    @Override
    public void run() {
        List<BibEntry> entries = panel.getSelectedEntries();
        if (entries == null) {
            return;
        }

        UndoableUnabbreviator undoableAbbreviator = new UndoableUnabbreviator(Globals.journalAbbreviationLoader
                .getRepository(Globals.prefs.getJournalAbbreviationPreferences()));

        NamedCompound ce = new NamedCompound(Localization.lang("Unabbreviate journal names"));
        int count = 0;
        for (BibEntry entry : entries) {
            for (String journalField : InternalBibtexFields.getJournalNameFields()) {
                if (undoableAbbreviator.unabbreviate(panel.getDatabase(), entry, journalField, ce)) {
                    count++;
                }
            }
        }
        if (count > 0) {
            ce.end();
            panel.getUndoManager().addEdit(ce);
            panel.markBaseChanged();
            message = Localization.lang("Unabbreviated %0 journal names.", String.valueOf(count));
        } else {
            message = Localization.lang("No journal names could be unabbreviated.");
        }
    }

    @Override
    public void update() {
        panel.output(message);
    }
}
