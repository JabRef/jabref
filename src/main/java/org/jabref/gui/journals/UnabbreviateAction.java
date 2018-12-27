package org.jabref.gui.journals;

import java.util.List;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.actions.BaseAction;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.InternalBibtexFields;

/**
 * Converts journal abbreviations back to full name for all selected entries.
 */
public class UnabbreviateAction implements BaseAction {

    private final BasePanel panel;

    public UnabbreviateAction(BasePanel panel) {
        this.panel = panel;
    }

    @Override
    public void action() {
        BackgroundTask.wrap(this::unabbreviate)
                      .onSuccess(panel::output)
                      .executeWith(Globals.TASK_EXECUTOR);
    }

    private String unabbreviate() {
        panel.output(Localization.lang("Unabbreviating..."));

        List<BibEntry> entries = panel.getSelectedEntries(); // never null

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
            return Localization.lang("Unabbreviated %0 journal names.", String.valueOf(count));
        } else {
            return Localization.lang("No journal names could be unabbreviated.");
        }
    }
}
