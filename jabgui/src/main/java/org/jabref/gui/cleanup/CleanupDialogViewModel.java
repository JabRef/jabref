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
import org.jabref.logic.conferences.ConferenceAbbreviationRepository;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

public class CleanupDialogViewModel extends AbstractViewModel {

    private final BibDatabaseContext databaseContext;

    private final CliPreferences preferences;

    private final DialogService dialogService;

    private final StateManager stateManager;

    private final UndoManager undoManager;

    @Nullable
    private final Supplier<LibraryTab> tabSupplier;

    @Nullable
    private final TaskExecutor taskExecutor;

    private final JournalAbbreviationRepository journalAbbreviationRepository;
    private final ConferenceAbbreviationRepository conferenceAbbreviationRepository;

    private final ObservableList<BibEntry> targetEntries = FXCollections.observableArrayList();
    private int modifiedEntriesCount;

    @NullMarked
    public CleanupDialogViewModel(
            BibDatabaseContext databaseContext,
            CliPreferences preferences,
            DialogService dialogService,
            StateManager stateManager,
            UndoManager undoManager,
            @Nullable Supplier<LibraryTab> tabSupplier,
            @Nullable TaskExecutor taskExecutor,
            JournalAbbreviationRepository journalAbbreviationRepository,
            ConferenceAbbreviationRepository conferenceAbbreviationRepository
    ) {
        this.databaseContext = databaseContext;
        this.preferences = preferences;
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.undoManager = undoManager;
        this.tabSupplier = tabSupplier;
        this.taskExecutor = taskExecutor;
        this.journalAbbreviationRepository = journalAbbreviationRepository;
        this.conferenceAbbreviationRepository = conferenceAbbreviationRepository;
    }

    public void setTargetEntries(List<BibEntry> entries) {
        targetEntries.setAll(Objects.requireNonNullElse(entries, List.of()));
    }

    public void apply(CleanupTabSelection selectedTab) {
        apply(selectedTab, true);
    }

    public void apply(CleanupTabSelection selectedTab, boolean showFeedback) {
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
            new BackgroundTask<Void>() {
                @Override
                public Void call() {
                    cleanupWithProgress(cleanupPreset, entriesToProcess, this);
                    return null;
                }
            }
                    .onSuccess(result -> {
                        if (showFeedback) {
                            showResults();
                        }
                    })
                    .onFailure(dialogService::showErrorDialogAndWait)
                    .executeWith(taskExecutor);
        } else {
            cleanup(cleanupPreset, entriesToProcess);
            if (showFeedback) {
                showResults();
            }
        }
    }

    /// Runs the cleanup on the entry and records the change.
    ///
    /// @return true iff entry was modified
    private boolean doCleanup(CleanupPreferences preset,
                              BibEntry entry,
                              NamedCompoundEdit compoundEdit,
                              List<JabRefException> failures) {
        CleanupWorker cleaner = new CleanupWorker(
                databaseContext,
                preferences.getFilePreferences(),
                preferences.getTimestampPreferences(),
                preferences.getJournalAbbreviationPreferences().shouldUseFJournalField(),
                journalAbbreviationRepository,
                conferenceAbbreviationRepository
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
        dialogService.notify(Localization.lang("%0 entry(s) needed a clean up", Integer.toString(modifiedEntriesCount)));
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

    private void cleanupWithProgress(CleanupPreferences cleanupPreferences,
                                     List<BibEntry> entries,
                                     BackgroundTask<?> task) {
        int count = entries.size();
        if (count > 1) {
            task.showToUser(true);
            task.setTitle(Localization.lang("Cleaning up entries"));
        }

        List<JabRefException> failures = new ArrayList<>();

        String editName = Localization.lang("Clean up entry(s)");
        NamedCompoundEdit compoundEdit = new NamedCompoundEdit(editName);

        for (int i = 0; i < count; i++) {
            if (task.isCancelled()) {
                break;
            }

            if (doCleanup(cleanupPreferences, entries.get(i), compoundEdit, failures)) {
                modifiedEntriesCount++;
            }

            task.updateProgress(i, count);
            task.updateMessage(Localization.lang("%0 of %1 entries cleaned up.",
                    String.valueOf(i + 1),
                    String.valueOf(count)));
        }

        task.updateProgress(count, count);

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
