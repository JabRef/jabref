package org.jabref.gui.citationkeypattern;

import java.util.List;

import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableKeyChange;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.JabRefPreferences;

public class GenerateCitationKeyAction extends SimpleCommand {

    private final JabRefFrame frame;
    private final DialogService dialogService;
    private final StateManager stateManager;

    private List<BibEntry> entries;
    private boolean isCanceled;

    public GenerateCitationKeyAction(JabRefFrame frame, DialogService dialogService, StateManager stateManager) {
        this.frame = frame;
        this.dialogService = dialogService;
        this.stateManager = stateManager;

        this.executable.bind(ActionHelper.needsEntriesSelected(stateManager));
    }

    @Override
    public void execute() {
        entries = stateManager.getSelectedEntries();

        if (entries.isEmpty()) {
            dialogService.showWarningDialogAndWait(Localization.lang("Autogenerate citation keys"),
                    Localization.lang("First select the entries you want keys to be generated for."));
            return;
        }
        dialogService.notify(formatOutputMessage(Localization.lang("Generating citation key for"), entries.size()));

        checkOverwriteKeysChosen();

        BackgroundTask.wrap(this::generateKeys)
                      .executeWith(Globals.TASK_EXECUTOR);
    }

    public static boolean confirmOverwriteKeys(DialogService dialogService) {
        if (Globals.prefs.getBoolean(JabRefPreferences.WARN_BEFORE_OVERWRITING_KEY)) {
            return dialogService.showConfirmationDialogWithOptOutAndWait(
                    Localization.lang("Overwrite keys"),
                    Localization.lang("One or more keys will be overwritten. Continue?"),
                    Localization.lang("Overwrite keys"),
                    Localization.lang("Cancel"),
                    Localization.lang("Disable this confirmation dialog"),
                    optOut -> Globals.prefs.putBoolean(JabRefPreferences.WARN_BEFORE_OVERWRITING_KEY, !optOut));
        } else {
            // Always overwrite keys by default
            return true;
        }
    }

    private void checkOverwriteKeysChosen() {
        // We don't want to generate keys for entries which already have one thus remove the entries
        if (Globals.prefs.getBoolean(JabRefPreferences.AVOID_OVERWRITING_KEY)) {
            entries.removeIf(BibEntry::hasCiteKey);
            // if we're going to override some cite keys warn the user about it
        } else if (entries.parallelStream().anyMatch(BibEntry::hasCiteKey)) {
            boolean overwriteKeys = confirmOverwriteKeys(dialogService);

            // The user doesn't want to override cite keys
            if (!overwriteKeys) {
                isCanceled = true;
                return;
            }
        }
    }

    private void generateKeys() {
        if (isCanceled) {
            return;
        }

        stateManager.getActiveDatabase().ifPresent(databaseContext -> {
            // generate the new cite keys for each entry
            final NamedCompound compound = new NamedCompound(Localization.lang("Autogenerate citation keys"));
            CitationKeyGenerator keyGenerator =
                    new CitationKeyGenerator(databaseContext, Globals.prefs.getCitationKeyPatternPreferences());
            for (BibEntry entry : entries) {
                keyGenerator.generateAndSetKey(entry)
                            .ifPresent(fieldChange -> compound.addEdit(new UndoableKeyChange(fieldChange)));
            }
            compound.end();

            // register the undo event only if new cite keys were generated
            if (compound.hasEdits()) {
                frame.getUndoManager().addEdit(compound);
            }

            frame.getCurrentBasePanel().markBaseChanged();
            dialogService.notify(formatOutputMessage(Localization.lang("Generated citation key for"), entries.size()));
        });
    }

    private String formatOutputMessage(String start, int count) {
        return String.format("%s %d %s.", start, count,
                (count > 1 ? Localization.lang("entries") : Localization.lang("entry")));
    }
}
