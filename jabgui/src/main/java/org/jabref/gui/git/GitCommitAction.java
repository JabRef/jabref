package org.jabref.gui.git;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Supplier;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.exporter.SaveDatabaseAction;
import org.jabref.gui.exporter.SaveDatabaseAction.SaveDatabaseMode;
import org.jabref.gui.exporter.SaveDatabaseAction.SaveResult;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.git.GitHandler;
import org.jabref.logic.git.status.GitStatusChecker;
import org.jabref.logic.git.util.GitHandlerRegistry;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryTypesManager;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public class GitCommitAction extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitCommitAction.class);

    private final Supplier<@Nullable LibraryTab> tabSupplier;
    private final DialogService dialogService;
    private final StateManager stateManager;
    private final GuiPreferences preferences;
    private final BibEntryTypesManager entryTypesManager;
    private final JournalAbbreviationRepository journalAbbreviationRepository;
    private final TaskExecutor taskExecutor;
    private final GitHandlerRegistry gitHandlerRegistry;

    public GitCommitAction(Supplier<@Nullable LibraryTab> tabSupplier,
                           DialogService dialogService,
                           StateManager stateManager,
                           GuiPreferences preferences,
                           BibEntryTypesManager entryTypesManager,
                           JournalAbbreviationRepository journalAbbreviationRepository,
                           TaskExecutor taskExecutor,
                           GitHandlerRegistry gitHandlerRegistry) {
        this.tabSupplier = tabSupplier;
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.preferences = preferences;
        this.entryTypesManager = entryTypesManager;
        this.journalAbbreviationRepository = journalAbbreviationRepository;
        this.taskExecutor = taskExecutor;
        this.gitHandlerRegistry = gitHandlerRegistry;

        this.executable.bind(ActionHelper.needsSavedLocalDatabase(stateManager));
    }

    @Override
    public void execute() {
        // Git operates on the file on disk, so in-memory changes would silently be left out of the commit.
        if (!isReadyToCommit()) {
            return;
        }

        stateManager.getActiveDatabase()
                    .flatMap(BibDatabaseContext::getDatabasePath)
                    .ifPresent(this::commit);
    }

    private void commit(Path bibFilePath) {
        // A library opened through a symlink must be handled as its real file: repository detection on
        // the link path would misclassify it, and staging the link would commit only the symlink.
        Path libraryFile = GitHandler.resolveToRealPath(bibFilePath);

        // [impl->req~git.commit.initialize-repository~1]
        if (GitHandler.findRepositoryRoot(libraryFile).isEmpty()) {
            initRepository(libraryFile);
            return;
        }

        if (hasNothingToCommit(libraryFile)) {
            dialogService.notify(Localization.lang("Nothing to commit."));
            return;
        }

        dialogService.showCustomDialogAndWait(
                new GitCommitDialogView()
        );
    }

    /// Offers to put a library that is not under version control into a fresh repository.
    /// Cancelling is a valid choice: the user may want to clone an existing repository into that folder instead.
    private void initRepository(Path libraryFile) {
        Path directory = libraryFile.getParent();
        boolean initialize = dialogService.showConfirmationDialogAndWait(
                Localization.lang("Git commit"),
                Localization.lang("This library is not under Git version control.\nInitialize a Git repository in %0 and commit %1?\nOther files in that folder stay untracked.", directory.toString(), libraryFile.getFileName().toString()),
                Localization.lang("Initialize"),
                Localization.lang("Do not initialize"));
        if (!initialize) {
            return;
        }

        BackgroundTask.wrap(() -> {
                          gitHandlerRegistry.get(directory).initAndCommit(libraryFile);
                          return null;
                      })
                      .onSuccess(_ -> dialogService.notify(Localization.lang("Initialized Git repository in %0.", directory.toString())))
                      .onFailure(e -> {
                          LOGGER.error("Could not initialize a Git repository in {}", directory, e);
                          dialogService.showErrorDialogAndWait(
                                  Localization.lang("Git commit"),
                                  Localization.lang("Could not initialize a Git repository in %0.", directory.toString()),
                                  e);
                      })
                      .executeWith(taskExecutor);
    }

    private boolean isReadyToCommit() {
        LibraryTab libraryTab = tabSupplier.get();
        if (libraryTab == null || !libraryTab.isModified()) {
            return true;
        }

        if (preferences.getLibraryPreferences().shouldAutoSave()) {
            return save(libraryTab);
        }
        return commitWithUnsavedChanges(libraryTab);
    }

    /// Without autosave the user decides when the library is written, so committing only the work
    /// already on disk stays available, as in other Git clients.
    /// [impl->req~git.commit.unsaved-changes~1]
    private boolean commitWithUnsavedChanges(LibraryTab libraryTab) {
        ButtonType saveAndCommit = new ButtonType(Localization.lang("Save and commit"), ButtonBar.ButtonData.YES);
        ButtonType commitWithoutSaving = new ButtonType(Localization.lang("Commit without saving"), ButtonBar.ButtonData.NO);
        ButtonType cancel = new ButtonType(Localization.lang("Cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);

        Optional<ButtonType> choice = dialogService.showCustomButtonDialogAndWait(
                Alert.AlertType.CONFIRMATION,
                Localization.lang("Git commit"),
                Localization.lang("The library has unsaved changes that are not in the file on disk."),
                saveAndCommit, commitWithoutSaving, cancel);

        if (choice.filter(saveAndCommit::equals).isPresent()) {
            return save(libraryTab);
        }
        return choice.filter(commitWithoutSaving::equals).isPresent();
    }

    /// The save must not commit by itself: this one is on its way to the commit dialog.
    private boolean save(LibraryTab libraryTab) {
        SaveResult saveResult = new SaveDatabaseAction(
                libraryTab,
                dialogService,
                preferences,
                entryTypesManager,
                stateManager,
                journalAbbreviationRepository,
                gitHandlerRegistry,
                taskExecutor).saveWithoutGitAutoCommit(SaveDatabaseMode.NORMAL);
        if (saveResult == SaveResult.ALREADY_SAVING) {
            // A save is still running (e.g. autosave), so the file on disk is not yet what the user sees.
            dialogService.notify(Localization.lang("The library is currently being saved. Please try again."));
        }
        // The action needs a saved local database, so a failed save has already reported itself.
        return saveResult == SaveResult.SUCCESS;
    }

    private boolean hasNothingToCommit(Path bibFilePath) {
        return gitHandlerRegistry.fromAnyPath(bibFilePath)
                                 .map(GitStatusChecker::checkStatus)
                                 .map(status -> !status.uncommittedChanges())
                                 .orElse(true);
    }
}
