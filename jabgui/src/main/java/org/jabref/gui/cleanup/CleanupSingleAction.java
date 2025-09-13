package org.jabref.gui.cleanup;

import java.util.List;
import java.util.Optional;

import javax.swing.undo.UndoManager;

import javafx.application.Platform;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.undo.NamedCompoundEdit;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.logic.JabRefException;
import org.jabref.logic.cleanup.CleanupPreferences;
import org.jabref.logic.cleanup.CleanupWorker;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

public class CleanupSingleAction extends SimpleCommand {

    private final CliPreferences preferences;
    private final DialogService dialogService;
    private final StateManager stateManager;
    private final BibEntry entry;
    private final UndoManager undoManager;

    private boolean isCanceled;
    private int modifiedEntriesCount;

    public CleanupSingleAction(BibEntry entry, CliPreferences preferences, DialogService dialogService, StateManager stateManager, UndoManager undoManager) {
        this.entry = entry;
        this.preferences = preferences;
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.undoManager = undoManager;

        this.executable.bind(ActionHelper.needsEntriesSelected(stateManager));
    }

    @Override
    public void execute() {
        isCanceled = false;

        CleanupDialog cleanupDialog = new CleanupDialog(
                stateManager.getActiveDatabase().get(),
                preferences.getCleanupPreferences(),
                preferences.getFilePreferences()
        );

        Optional<CleanupPreferences> chosenPreset = dialogService.showCustomDialogAndWait(cleanupDialog);

        chosenPreset.ifPresent(preset -> {
            if (preset.isActive(CleanupPreferences.CleanupStep.RENAME_PDF) && preferences.getAutoLinkPreferences().shouldAskAutoNamingPdfs()) {
                boolean confirmed = dialogService.showConfirmationDialogWithOptOutAndWait(Localization.lang("Autogenerate PDF Names"),
                        Localization.lang("Auto-generating PDF-Names does not support undo. Continue?"),
                        Localization.lang("Autogenerate PDF Names"),
                        Localization.lang("Cancel"),
                        Localization.lang("Do not ask again"),
                        optOut -> preferences.getAutoLinkPreferences().setAskAutoNamingPdfs(!optOut));
                if (!confirmed) {
                    isCanceled = true;
                    return;
                }
            }

            preferences.getCleanupPreferences().setActiveJobs(preset.getActiveJobs());
            preferences.getCleanupPreferences().setFieldFormatterCleanups(preset.getFieldFormatterCleanups());

            cleanup(stateManager.getActiveDatabase().get(), preset);
        });
    }

    /**
     * Runs the cleanup on the entry and records the change.
     */
    private void doCleanup(BibDatabaseContext databaseContext, CleanupPreferences preset, BibEntry entry, NamedCompoundEdit compoundEdit) {
        // Create and run cleaner
        CleanupWorker cleaner = new CleanupWorker(
                databaseContext,
                preferences.getFilePreferences(),
                preferences.getTimestampPreferences()
        );

        List<FieldChange> changes = cleaner.cleanup(preset, entry);

        // Register undo action
        for (FieldChange change : changes) {
            compoundEdit.addEdit(new UndoableFieldChange(change));
        }

        if (!cleaner.getFailures().isEmpty()) {
            this.showFailures(cleaner.getFailures());
        }
    }

    private void cleanup(BibDatabaseContext databaseContext, CleanupPreferences cleanupPreferences) {
        // undo granularity is on entry level
        NamedCompoundEdit compoundEdit = new NamedCompoundEdit(Localization.lang("Cleanup entry"));

        doCleanup(databaseContext, cleanupPreferences, entry, compoundEdit);

        compoundEdit.end();
        if (compoundEdit.hasEdits()) {
            undoManager.addEdit(compoundEdit);
        }
    }

    private void showFailures(List<JabRefException> failures) {
        StringBuilder sb = new StringBuilder();
        for (JabRefException exception : failures) {
            sb.append("- ").append(exception.getLocalizedMessage()).append("\n");
        }
        Platform.runLater(() ->
                dialogService.showErrorDialogAndWait(Localization.lang("File Move Errors"), sb.toString())
        );
    }
}
