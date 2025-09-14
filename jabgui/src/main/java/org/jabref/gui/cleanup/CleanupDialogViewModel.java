package org.jabref.gui.cleanup;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.swing.undo.UndoManager;

import javafx.application.Platform;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.logic.NamedCompound;
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

public class CleanupDialogViewModel extends AbstractViewModel {

    private final Optional<Supplier<LibraryTab>> tabSupplier;
    private final BibDatabaseContext databaseContext;
    private final CliPreferences preferences;
    private final DialogService dialogService;
    private final StateManager stateManager;
    private final Optional<TaskExecutor> taskExecutor;
    private final UndoManager undoManager;

    private final List<JabRefException> failures;
    private Optional<List<BibEntry>> targetEntries;

    private boolean isCanceled;
    private int modifiedEntriesCount;

    public CleanupDialogViewModel(Optional<Supplier<LibraryTab>> tabSupplier,
                                  BibDatabaseContext databaseContext,
                                  CliPreferences preferences,
                                  DialogService dialogService,
                                  StateManager stateManager,
                                  Optional<TaskExecutor> taskExecutor,
                                  UndoManager undoManager) {
        this.databaseContext = Objects.requireNonNull(databaseContext, "databaseContext must not be null");
        this.preferences = Objects.requireNonNull(preferences, "preferences must not be null");
        this.dialogService = Objects.requireNonNull(dialogService, "dialogService must not be null");
        this.stateManager = Objects.requireNonNull(stateManager, "stateManager must not be null");
        this.undoManager = Objects.requireNonNull(undoManager, "undoManager must not be null");

        this.tabSupplier = (tabSupplier.isEmpty()) ? Optional.empty() : tabSupplier;
        this.taskExecutor = (taskExecutor.isEmpty()) ? Optional.empty() : taskExecutor;

        this.failures = new ArrayList<>();
        this.targetEntries = Optional.empty();
    }

    public CleanupDialogViewModel(BibDatabaseContext databaseContext,
                                  CliPreferences preferences,
                                  DialogService dialogService,
                                  StateManager stateManager,
                                  UndoManager undoManager) {
        this(
                Optional.empty(),
                databaseContext,
                preferences,
                dialogService,
                stateManager,
                Optional.empty(),
                undoManager
        );
    }

    public void setTargetEntries(Optional<List<BibEntry>> entries) {
        this.targetEntries = (entries.isEmpty()) ? Optional.empty() : entries.map(List::copyOf);
    }

    public void apply(CleanupTabSelection selectedTab) {
        if (stateManager.getActiveDatabase().isEmpty()) {
            return;
        }

        List<BibEntry> entriesToProcess = targetEntries
                .orElseGet(() -> List.copyOf(stateManager.getSelectedEntries()));

        if (entriesToProcess.isEmpty()) { // None selected. Inform the user to select entries first.
            dialogService.showInformationDialogAndWait(Localization.lang("Cleanup entry"), Localization.lang("First select entries to clean up.")
            );
            return;
        }

        isCanceled = false;
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
                isCanceled = true;
                return;
            }
        }

        CleanupPreferences updatedPreferences = preferences.getCleanupPreferences()
                                                           .updateWith(Optional.ofNullable(selectedTab.allJobs()), Optional.of(selectedTab.selectedJobs()), selectedTab.formatters());

        preferences.getCleanupPreferences().setActiveJobs(updatedPreferences.getActiveJobs());
        preferences.getCleanupPreferences().setFieldFormatterCleanups(updatedPreferences.getFieldFormatterCleanups());

        CleanupPreferences runPreset = new CleanupPreferences(EnumSet.copyOf(selectedTab.selectedJobs()));
        selectedTab.formatters().ifPresent(runPreset::setFieldFormatterCleanups);

        taskExecutor.ifPresentOrElse(
                executor -> BackgroundTask.wrap(() -> cleanup(databaseContext, runPreset, entriesToProcess))
                                          .onSuccess(result -> showResults())
                                          .onFailure(dialogService::showErrorDialogAndWait)
                                          .executeWith(executor),
                () -> {
                    cleanup(databaseContext, runPreset, entriesToProcess);
                }
        );
    }

    /**
     * Runs the cleanup on the entry and records the change.
     *
     * @return true iff entry was modified
     */
    private boolean doCleanup(BibDatabaseContext databaseContext, CleanupPreferences preset, BibEntry entry, NamedCompound compound) {
        // Create and run cleaner
        CleanupWorker cleaner = new CleanupWorker(
                databaseContext,
                preferences.getFilePreferences(),
                preferences.getTimestampPreferences()
        );

        List<FieldChange> changes = cleaner.cleanup(preset, entry);

        // Register undo action
        for (FieldChange change : changes) {
            compound.addEdit(new UndoableFieldChange(change));
        }

        failures.addAll(cleaner.getFailures());

        return !changes.isEmpty();
    }

    private void showResults() {
        if (isCanceled) {
            return;
        }

        if (modifiedEntriesCount > 0) {
            tabSupplier.ifPresent(s -> s.get().markBaseChanged());
        }

        if (modifiedEntriesCount == 0) {
            dialogService.notify(Localization.lang("No entry needed a clean up"));
        } else if (modifiedEntriesCount == 1) {
            dialogService.notify(Localization.lang("One entry needed a clean up"));
        } else {
            dialogService.notify(Localization.lang("%0 entries needed a clean up", Integer.toString(modifiedEntriesCount)));
        }
    }

    private void cleanup(BibDatabaseContext databaseContext, CleanupPreferences cleanupPreferences, List<BibEntry> entries) {
        this.failures.clear();

        // undo granularity is on a set of all entries
        NamedCompound compound = new NamedCompound(Localization.lang("Clean up entries"));

        for (BibEntry entry : entries) {
            if (doCleanup(databaseContext, cleanupPreferences, entry, compound)) {
                modifiedEntriesCount++;
            }
        }

        compound.end();

        if (compound.hasEdits()) {
            undoManager.addEdit(compound);
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

