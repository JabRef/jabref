package org.jabref.gui.actions;

import java.util.List;
import java.util.Optional;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableKeyChange;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.logic.bibtexkeypattern.BibtexKeyGenerator;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.JabRefPreferences;

public class GenerateKeysAction implements BaseAction {

    private final BasePanel panel;
    private final DialogService dialogService;
    private List<BibEntry> entries;
    private int numSelected;
    private boolean canceled;

    public GenerateKeysAction(BasePanel panel, DialogService dialogService) {
        this.panel = panel;
        this.dialogService = dialogService;
    }

    public void init() {
        entries = panel.getSelectedEntries();
        numSelected = entries.size();

        if (entries.isEmpty()) { // None selected. Inform the user to select entries first.
            dialogService.showWarningDialogAndWait(Localization.lang("Autogenerate BibTeX keys"),
                                                   Localization.lang("First select the entries you want keys to be generated for."));
            return;
        }
        panel.output(panel.formatOutputMessage(Localization.lang("Generating BibTeX key for"), numSelected));
    }

    private void generateKeys() {
        // We don't want to generate keys for entries which already have one thus remove the entries
        if (Globals.prefs.getBoolean(JabRefPreferences.AVOID_OVERWRITING_KEY)) {
            entries.removeIf(BibEntry::hasCiteKey);

            // if we're going to override some cite keys warn the user about it
        } else if (Globals.prefs.getBoolean(JabRefPreferences.WARN_BEFORE_OVERWRITING_KEY)) {
            if (entries.parallelStream().anyMatch(BibEntry::hasCiteKey)) {

                boolean overwriteKeysPressed = dialogService.showConfirmationDialogWithOptOutAndWait(
                                                                                                     Localization.lang("Overwrite keys"),
                                                                                                     Localization.lang("One or more keys will be overwritten. Continue?"),
                                                                                                     Localization.lang("Overwrite keys"),
                                                                                                     Localization.lang("Cancel"),
                                                                                                     Localization.lang("Disable this confirmation dialog"),
                                                                                                     optOut -> Globals.prefs.putBoolean(JabRefPreferences.WARN_BEFORE_OVERWRITING_KEY, !optOut));

                // The user doesn't want to overide cite keys
                if (!overwriteKeysPressed) {
                    canceled = true;
                    return;
                }
            }
        }

        // generate the new cite keys for each entry
        final NamedCompound ce = new NamedCompound(Localization.lang("Autogenerate BibTeX keys"));
        BibtexKeyGenerator keyGenerator = new BibtexKeyGenerator(panel.getBibDatabaseContext(), Globals.prefs.getBibtexKeyPatternPreferences());
        for (BibEntry entry : entries) {
            Optional<FieldChange> change = keyGenerator.generateAndSetKey(entry);
            change.ifPresent(fieldChange -> ce.addEdit(new UndoableKeyChange(fieldChange)));
        }
        ce.end();

        // register the undo event only if new cite keys were generated
        if (ce.hasEdits()) {
            panel.getUndoManager().addEdit(ce);
        }

        if (canceled) {
            return;
        }
        panel.markBaseChanged();
        numSelected = entries.size();
        panel.output(panel.formatOutputMessage(Localization.lang("Generated BibTeX key for"), numSelected));
    }

    @Override
    public void action() {
        init();
        BackgroundTask.wrap(this::generateKeys)
                      .executeWith(Globals.TASK_EXECUTOR);
    }
}
