package org.jabref.gui.actions;

import java.util.List;
import java.util.Optional;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
import org.jabref.gui.cleanup.CleanupDialog;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.logic.cleanup.CleanupPreset;
import org.jabref.logic.cleanup.CleanupWorker;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.JabRefPreferences;

public class CleanupAction implements BaseAction {

    private final BasePanel panel;
    private final DialogService dialogService;

    /**
     * Global variable to count unsuccessful renames
     */
    private int unsuccessfulRenames;

    private boolean isCanceled;
    private int modifiedEntriesCount;
    private final JabRefPreferences preferences;

    public CleanupAction(BasePanel panel, JabRefPreferences preferences) {
        this.panel = panel;
        this.preferences = preferences;
        this.dialogService = panel.frame().getDialogService();
    }

    @Override
    public void action() {
        init();
        if (isCanceled) {
            return;
        }
        CleanupDialog cleanupDialog = new CleanupDialog(panel.getBibDatabaseContext(), preferences.getCleanupPreset());

        Optional<CleanupPreset> chosenPreset = cleanupDialog.showAndWait();
        chosenPreset.ifPresent(cleanupPreset ->
                BackgroundTask.wrap(() -> cleanup(cleanupPreset))
                              .onSuccess(x -> showResults())
                              .executeWith(Globals.TASK_EXECUTOR));
    }

    public void init() {
        isCanceled = false;
        modifiedEntriesCount = 0;
        if (panel.getSelectedEntries().isEmpty()) { // None selected. Inform the user to select entries first.
            dialogService.showInformationDialogAndWait(Localization.lang("Cleanup entry"), Localization.lang("First select entries to clean up."));
            isCanceled = true;
            return;
        }
        panel.output(Localization.lang("Doing a cleanup for %0 entries...",
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

        unsuccessfulRenames = cleaner.getUnsuccessfulRenames();

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
        if (unsuccessfulRenames > 0) { //Rename failed for at least one entry
            dialogService.showErrorDialogAndWait(
                    Localization.lang("Autogenerate PDF Names"),
                    Localization.lang("File rename failed for %0 entries.", Integer.toString(unsuccessfulRenames)));
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
        panel.output(message);
    }

    private void cleanup(CleanupPreset cleanupPreset) {
        preferences.setCleanupPreset(cleanupPreset);

        if (cleanupPreset.isRenamePDF() && preferences.getBoolean(JabRefPreferences.ASK_AUTO_NAMING_PDFS_AGAIN)) {

            boolean confirmed = DefaultTaskExecutor.runInJavaFXThread(() -> dialogService.showConfirmationDialogWithOptOutAndWait(Localization.lang("Autogenerate PDF Names"),
                    Localization.lang("Auto-generating PDF-Names does not support undo. Continue?"),
                    Localization.lang("Autogenerate PDF Names"),
                    Localization.lang("Cancel"),
                    Localization.lang("Disable this confirmation dialog"),
                    optOut -> Globals.prefs.putBoolean(JabRefPreferences.ASK_AUTO_NAMING_PDFS_AGAIN, !optOut)));

            if (!confirmed) {
                isCanceled = true;
                return;
            }
        }

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
