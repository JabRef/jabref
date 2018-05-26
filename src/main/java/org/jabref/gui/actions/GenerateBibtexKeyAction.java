package org.jabref.gui.actions;

import java.util.List;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableKeyChange;
import org.jabref.gui.worker.AbstractWorker;
import org.jabref.logic.bibtexkeypattern.BibtexKeyGenerator;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.JabRefPreferences;

public class GenerateBibtexKeyAction extends AbstractWorker {
    private final DialogService dialogService;
    private BasePanel basePanel;
    private List<BibEntry> entries;
    private boolean canceled;

    public GenerateBibtexKeyAction(BasePanel basePanel, DialogService dialogService) {
        this.basePanel = basePanel;
        this.dialogService = dialogService;
    }

    @Override
    public void init() {
        entries = basePanel.getSelectedEntries();

        if (entries.isEmpty()) {
            dialogService.showWarningDialogAndWait(Localization.lang("Autogenerate BibTeX keys"),
                    Localization.lang("First select the entries you want keys to be generated for."));
            return;
        }
        basePanel.output(formatOutputMessage(Localization.lang("Generating BibTeX key for"), entries.size()));
    }

    @Override
    public void run() {
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

                // The user doesn't want to override cite keys
                if (!overwriteKeysPressed) {
                    canceled = true;
                    return;
                }
            }
        }

        // generate the new cite keys for each entry
        final NamedCompound compound = new NamedCompound(Localization.lang("Autogenerate BibTeX keys"));
        BibtexKeyGenerator keyGenerator = new BibtexKeyGenerator(basePanel.getBibDatabaseContext(), Globals.prefs.getBibtexKeyPatternPreferences());
        for (BibEntry entry : entries) {
            keyGenerator.generateAndSetKey(entry)
                        .ifPresent(fieldChange -> compound.addEdit(new UndoableKeyChange(fieldChange)));
        }
        compound.end();

        // register the undo event only if new cite keys were generated
        if (compound.hasEdits()) {
            basePanel.getUndoManager().addEdit(compound);
        }
    }

    @Override
    public void update() {
        if (canceled) {
            return;
        }
        basePanel.markBaseChanged();
        basePanel.output(formatOutputMessage(Localization.lang("Generated BibTeX key for"), entries.size()));
    }

    private String formatOutputMessage(String start, int count) {
        return String.format("%s %d %s.", start, count,
                (count > 1 ? Localization.lang("entries") : Localization.lang("entry")));
    }
}
