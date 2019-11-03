package org.jabref.gui.journals;

import java.util.List;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.actions.BaseAction;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.FieldFactory;

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
        panel.output(Localization.lang("Unabbreviating..."));
        BackgroundTask.wrap(this::unabbreviate)
                      .onSuccess(panel::output)
                      .executeWith(Globals.TASK_EXECUTOR);
    }

    private String unabbreviate() {
        List<BibEntry> entries = panel.getSelectedEntries(); // Never null

        UndoableUnabbreviator undoableAbbreviator = new UndoableUnabbreviator(Globals.journalAbbreviationLoader
                .getRepository(Globals.prefs.getJournalAbbreviationPreferences()));

        NamedCompound ce = new NamedCompound(Localization.lang("Unabbreviate journal names"));
        int count = entries.stream().mapToInt(entry ->
                (int) FieldFactory.getJournalNameFields().stream().filter(journalField ->
                        undoableAbbreviator.unabbreviate(panel.getDatabase(), entry, journalField, ce)).count()).sum();
        if (count > 0) {
            ce.end();
            panel.getUndoManager().addEdit(ce);
            panel.markBaseChanged();
            return Localization.lang("Unabbreviated %0 journal names.", String.valueOf(count));
        }
        return Localization.lang("No journal names could be unabbreviated.");
    }
}
