package org.jabref.gui.cleanup;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.swing.undo.UndoManager;

import javafx.application.Platform;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
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
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

public class CleanupAction extends SimpleCommand {

    private final Supplier<LibraryTab> tabSupplier;
    private final CliPreferences preferences;
    private final DialogService dialogService;
    private final StateManager stateManager;
    private final TaskExecutor taskExecutor;
    private final UndoManager undoManager;
    private final List<JabRefException> failures;

    private boolean isCanceled;
    private int modifiedEntriesCount;

    public CleanupAction(Supplier<LibraryTab> tabSupplier,
                         CliPreferences preferences,
                         DialogService dialogService,
                         StateManager stateManager,
                         TaskExecutor taskExecutor,
                         UndoManager undoManager) {
        this.tabSupplier = tabSupplier;
        this.preferences = preferences;
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.taskExecutor = taskExecutor;
        this.undoManager = undoManager;
        this.failures = new ArrayList<>();

        this.executable.bind(ActionHelper.needsEntriesSelected(stateManager));
    }

    @Override
    public void execute() {
        if (stateManager.getActiveDatabase().isEmpty()) {
            return;
        }

        if (stateManager.getSelectedEntries().isEmpty()) { // None selected. Inform the user to select entries first.
            dialogService.showInformationDialogAndWait(Localization.lang("Cleanup entry"), Localization.lang("First select entries to clean up."));
            return;
        }

        isCanceled = false;
        modifiedEntriesCount = 0;

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

            BackgroundTask.wrap(() -> cleanup(stateManager.getActiveDatabase().get(), preset))
                          .onSuccess(result -> showResults())
                          .onFailure(dialogService::showErrorDialogAndWait)
                          .executeWith(taskExecutor);
        });
    }

    /**
     * Runs the cleanup on the entry and records the change.
     *
     * @return true iff entry was modified
     */
    private boolean doCleanup(BibDatabaseContext databaseContext, CleanupPreferences preset, BibEntry entry, NamedCompoundEdit compoundEdit) {
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

        failures.addAll(cleaner.getFailures());

        return !changes.isEmpty();
    }

    private void showResults() {
        if (isCanceled) {
            return;
        }

        if (modifiedEntriesCount > 0) {
            tabSupplier.get().markBaseChanged();
        }

        if (modifiedEntriesCount == 0) {
            dialogService.notify(Localization.lang("No entry needed a clean up"));
        } else if (modifiedEntriesCount == 1) {
            dialogService.notify(Localization.lang("One entry needed a clean up"));
        } else {
            dialogService.notify(Localization.lang("%0 entries needed a clean up", Integer.toString(modifiedEntriesCount)));
        }
    }

    private void cleanup(BibDatabaseContext databaseContext, CleanupPreferences cleanupPreferences) {
        this.failures.clear();

        // undo granularity is on set of all entries
        NamedCompoundEdit compoundEdit = new NamedCompoundEdit(Localization.lang("Clean up entries"));

        for (BibEntry entry : List.copyOf(stateManager.getSelectedEntries())) {
            if (doCleanup(databaseContext, cleanupPreferences, entry, compoundEdit)) {
                modifiedEntriesCount++;
            }
        }

        compoundEdit.end();

        if (compoundEdit.hasEdits()) {
            undoManager.addEdit(compoundEdit);
        }

        if (!failures.isEmpty()) {
            showFailures(failures);
        }
    }

    private void showFailures(List<JabRefException> failures) {
        String message = failures.stream()
                                 .map(exception -> "- " + exception.getLocalizedMessage())
                                 .collect(Collectors.joining("\n"));

        Platform.runLater(() ->
                dialogService.showErrorDialogAndWait(Localization.lang("File Move Errors"), message)
        );
    }
}
