package org.jabref.gui.cleanup;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.swing.undo.UndoManager;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.undo.NamedCompoundEdit;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.logic.JabRefException;
import org.jabref.logic.cleanup.CleanupPreferences;
import org.jabref.logic.cleanup.CleanupTabSelection;
import org.jabref.logic.cleanup.CleanupWorker;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NonNull;

public class CleanupDialogViewModel extends AbstractViewModel {

    private final BibDatabaseContext databaseContext;
    private final CliPreferences preferences;
    private final DialogService dialogService;
    private final StateManager stateManager;
    private final UndoManager undoManager;
    private final Supplier<LibraryTab> tabSupplier;
    private final TaskExecutor taskExecutor;

    private final ObservableList<BibEntry> targetEntries = FXCollections.observableArrayList();
    private int modifiedEntriesCount;

    public CleanupDialogViewModel(
            @NonNull BibDatabaseContext databaseContext,
            @NonNull CliPreferences preferences,
            @NonNull DialogService dialogService,
            @NonNull StateManager stateManager,
            @NonNull UndoManager undoManager,
            Supplier<LibraryTab> tabSupplier,
            TaskExecutor taskExecutor
    ) {
        this.databaseContext = databaseContext;
        this.preferences = preferences;
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.undoManager = undoManager;

        this.tabSupplier = tabSupplier; // can be null
        this.taskExecutor = taskExecutor; // can be null
    }

    public void setTargetEntries(List<BibEntry> entries) {
        targetEntries.setAll(Objects.requireNonNullElse(entries, List.of()));
    }

    public void apply(CleanupTabSelection selectedTab) {
        if (stateManager.getActiveDatabase().isEmpty()) {
            return;
        }

        List<BibEntry> entriesToProcess = targetEntries.isEmpty() ? List.copyOf(stateManager.getSelectedEntries()) : targetEntries;

        if (entriesToProcess.isEmpty()) { // None selected. Inform the user to select entries first.
            dialogService.showInformationDialogAndWait(Localization.lang("Clean up entry"), Localization.lang("First select entries to clean up.")
            );
            return;
        }

        modifiedEntriesCount = 0;

        if (selectedTab.selectedJobs().contains(CleanupPreferences.CleanupStep.RENAME_PDF) && preferences.getAutoLinkPreferences().shouldAskAutoNamingPdfs()) {
            boolean confirmed = dialogService.showConfirmationDialogWithOptOutAndWait(
                    Localization.lang("Autogenerate PDF Names"),
                    Localization.lang("Auto-generating PDF-Names does not support undo. Continue?"),
                    Localization.lang("Autogenerate PDF Names"),
                    Localization.lang("Cancel"),
                    Localization.lang("Do not ask again"),
                    optOut -> preferences.getAutoLinkPreferences().setAskAutoNamingPdfs(!optOut)
            );
            if (!confirmed) {
                return;
            }
        }

        CleanupPreferences updatedPreferences = selectedTab.updatePreferences(preferences.getCleanupPreferences());

        if (selectedTab.isJobTab()) {
            preferences.getCleanupPreferences().setActiveJobs(updatedPreferences.getActiveJobs());
        }

        if (selectedTab.isFormatterTab()) {
            preferences.getCleanupPreferences().setFieldFormatterCleanups(updatedPreferences.getFieldFormatterCleanups());
        }

        CleanupPreferences cleanupPreset = new CleanupPreferences(EnumSet.copyOf(selectedTab.selectedJobs()));
        selectedTab.formatters().ifPresent(cleanupPreset::setFieldFormatterCleanups);

        if (taskExecutor != null) {
            BackgroundTask.wrap(() -> cleanup(cleanupPreset, entriesToProcess))
                          .onSuccess(result -> showResults())
                          .onFailure(dialogService::showErrorDialogAndWait)
                          .executeWith(taskExecutor);
        } else {
            cleanup(cleanupPreset, entriesToProcess);
        }
    }

    /**
     * Runs the cleanup on the entry and records the change.
     *
     * @return true iff entry was modified
     */
    private boolean doCleanup(CleanupPreferences preset,
                              BibEntry entry,
                              NamedCompoundEdit compoundEdit,
                              List<JabRefException> failures) {
        CleanupWorker cleaner = new CleanupWorker(
                databaseContext,
                preferences.getFilePreferences(),
                preferences.getTimestampPreferences()
        );

        List<FieldChange> changes = cleaner.cleanup(preset, entry);

        for (FieldChange change : changes) {
            compoundEdit.addEdit(new UndoableFieldChange(change));
        }

        failures.addAll(cleaner.getFailures());

        return !changes.isEmpty();
    }

    private void showResults() {
        if (modifiedEntriesCount > 0 && tabSupplier != null) {
            tabSupplier.get().markBaseChanged();
        }

        String message = switch (modifiedEntriesCount) {
            case 0 ->
                    Localization.lang("No entry needed a clean up");
            case 1 ->
                    Localization.lang("One entry needed a clean up");
            default ->
                    Localization.lang("%0 entries needed a clean up", Integer.toString(modifiedEntriesCount));
        };

        dialogService.notifyAll(message);
    }

    private void cleanup(CleanupPreferences cleanupPreferences, List<BibEntry> entries) {
        List<JabRefException> failures = new ArrayList<>();

        String editName = Localization.lang("Clean up entry(s)");
        // undo granularity is on a set of all entries
        NamedCompoundEdit compoundEdit = new NamedCompoundEdit(editName);

        for (BibEntry entry : entries) {
            if (doCleanup(cleanupPreferences, entry, compoundEdit, failures)) {
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
