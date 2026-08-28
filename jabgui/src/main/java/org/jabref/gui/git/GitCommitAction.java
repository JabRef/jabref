package org.jabref.gui.git;

import java.util.function.Supplier;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.exporter.SaveDatabaseAction;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.git.GitHandler;
import org.jabref.logic.git.status.GitStatusChecker;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryTypesManager;

public class GitCommitAction extends SimpleCommand {

    private final Supplier<LibraryTab> tabSupplier;
    private final DialogService dialogService;
    private final StateManager stateManager;
    private final GuiPreferences preferences;
    private final BibEntryTypesManager entryTypesManager;
    private final JournalAbbreviationRepository journalAbbreviationRepository;

    public GitCommitAction(Supplier<LibraryTab> tabSupplier,
                           DialogService dialogService,
                           StateManager stateManager,
                           GuiPreferences preferences,
                           BibEntryTypesManager entryTypesManager,
                           JournalAbbreviationRepository journalAbbreviationRepository) {
        this.tabSupplier = tabSupplier;
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.preferences = preferences;
        this.entryTypesManager = entryTypesManager;
        this.journalAbbreviationRepository = journalAbbreviationRepository;

        this.executable.bind(ActionHelper.needsSavedLocalDatabase(stateManager));
    }

    @Override
    public void execute() {
        // Git operates on the file on disk, so in-memory changes would silently be left out of the commit.
        if (!saveActiveLibrary()) {
            return;
        }

        if (hasNothingToCommit()) {
            dialogService.notify(Localization.lang("Nothing to commit."));
            return;
        }

        dialogService.showCustomDialogAndWait(
                new GitCommitDialogView()
        );
    }

    private boolean saveActiveLibrary() {
        LibraryTab libraryTab = tabSupplier.get();
        if (libraryTab == null || !libraryTab.isModified()) {
            return true;
        }

        boolean saved = new SaveDatabaseAction(
                libraryTab,
                dialogService,
                preferences,
                entryTypesManager,
                stateManager,
                journalAbbreviationRepository).save();
        if (!saved) {
            dialogService.notify(Localization.lang("Unable to save library"));
        }
        return saved;
    }

    private boolean hasNothingToCommit() {
        return stateManager.getActiveDatabase()
                           .flatMap(BibDatabaseContext::getDatabasePath)
                           .flatMap(path -> GitHandler.fromAnyPath(path, preferences.getGitPreferences()))
                           .map(GitStatusChecker::checkStatus)
                           .map(status -> !status.uncommittedChanges())
                           .orElse(true);
    }
}
