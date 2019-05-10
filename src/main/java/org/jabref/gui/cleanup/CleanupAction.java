package org.jabref.gui.cleanup;

import java.util.List;
import java.util.Optional;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
import org.jabref.gui.actions.BaseAction;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.cleanup.CleanupPreset;
import org.jabref.logic.cleanup.CleanupWorker;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.JabRefPreferences;

public class CleanupAction implements BaseAction {

    private final BasePanel panel;
    private final DialogService dialogService;
    private final TaskExecutor taskExecutor;

    private boolean isCanceled;
    private int modifiedEntriesCount;
    private final JabRefPreferences preferences;

    public CleanupAction(BasePanel panel, JabRefPreferences preferences, TaskExecutor taskExecutor) {
        this.panel = panel;
        this.preferences = preferences;
        this.dialogService = panel.frame().getDialogService();
        this.taskExecutor = taskExecutor;
    }

    @Override
    public void action() {
        init();
        if (isCanceled) {
            return;
        }
        CleanupDialog cleanupDialog = new CleanupDialog(panel.getBibDatabaseContext(), preferences.getCleanupPreset(), preferences.getFilePreferences());

        Optional<CleanupPreset> chosenPreset = cleanupDialog.showAndWait();

        if (chosenPreset.isPresent()) {
            if (chosenPreset.get().isRenamePDFActive() && preferences.getBoolean(JabRefPreferences.ASK_AUTO_NAMING_PDFS_AGAIN)) {
                boolean confirmed = dialogService.showConfirmationDialogWithOptOutAndWait(Localization.lang("Autogenerate PDF Names"),
                        Localization.lang("Auto-generating PDF-Names does not support undo. Continue?"),
                        Localization.lang("Autogenerate PDF Names"),
                        Localization.lang("Cancel"),
                        Localization.lang("Disable this confirmation dialog"),
                        optOut -> Globals.prefs.putBoolean(JabRefPreferences.ASK_AUTO_NAMING_PDFS_AGAIN, !optOut));

                if (!confirmed) {
                    isCanceled = true;
                    return;
                }
            }

            preferences.setCleanupPreset(chosenPreset.get());

            BackgroundTask.wrap(() -> cleanup(chosenPreset.get()))
                          .onSuccess(result -> showResults())
                          .executeWith(taskExecutor);
        }
    }

    public void init() {
        isCanceled = false;
        modifiedEntriesCount = 0;
        if (panel.getSelectedEntries().isEmpty()) { // None selected. Inform the user to select entries first.
            dialogService.showInformationDialogAndWait(Localization.lang("Cleanup entry"), Localization.lang("First select entries to clean up."));
            isCanceled = true;
            return;
        }
        dialogService.notify(Localization.lang("Doing a cleanup for %0 entries...",
                Integer.toString(panel.getSelectedEntries().size())));
    }

    /**
     * Runs the cleanup on the entry and records the change.
     */
    private void doCleanup(CleanupPreset preset, BibEntry entry, NamedCompound ce) {
        // Create and run cleaner
        CleanupWorker cleaner = new CleanupWorker(panel.getBibDatabaseContext(), preferences.getCleanupPreferences(
                Globals.journalAbbreviationLoader));
        List<FieldChange> changes = cleaner.cleanup(preset, entry);

        if (changes.isEmpty()) {
            return;
        }

        // Register undo action
        for (FieldChange change : changes) {
            ce.addEdit(new UndoableFieldChange(change));
        }
    }

    private void showResults() {
        if (isCanceled) {
            return;
        }

        if (modifiedEntriesCount > 0) {
            panel.updateEntryEditorIfShowing();
            panel.markBaseChanged();
        }
        String message;
        switch (modifiedEntriesCount) {
            case 0:
                message = Localization.lang("No entry needed a clean up");
                break;
            case 1:
                message = Localization.lang("One entry needed a clean up");
                break;
            default:
                message = Localization.lang("%0 entries needed a clean up", Integer.toString(modifiedEntriesCount));
                break;
        }
        dialogService.notify(message);
    }

    private void cleanup(CleanupPreset cleanupPreset) {
        preferences.setCleanupPreset(cleanupPreset);

        for (BibEntry entry : panel.getSelectedEntries()) {
            // undo granularity is on entry level
            NamedCompound ce = new NamedCompound(Localization.lang("Cleanup entry"));

            doCleanup(cleanupPreset, entry, ce);

            ce.end();
            if (ce.hasEdits()) {
                modifiedEntriesCount++;
                panel.getUndoManager().addEdit(ce);
            }
        }
    }
}
