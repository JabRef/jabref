package org.jabref.gui.cleanup;

import java.util.function.Supplier;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.TaskExecutor;

public class CleanupAction extends SimpleCommand {

    private final Supplier<LibraryTab> tabSupplier;
    private final CliPreferences preferences;
    private final DialogService dialogService;
    private final StateManager stateManager;
    private final TaskExecutor taskExecutor;
    private final UndoManager undoManager;
    private final JournalAbbreviationRepository journalAbbreviationRepository;

    public CleanupAction(Supplier<LibraryTab> tabSupplier,
                         CliPreferences preferences,
                         DialogService dialogService,
                         StateManager stateManager,
                         TaskExecutor taskExecutor,
                         UndoManager undoManager,
                         JournalAbbreviationRepository journalAbbreviationRepository) {
        this.tabSupplier = tabSupplier;
        this.preferences = preferences;
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.taskExecutor = taskExecutor;
        this.undoManager = undoManager;
        this.journalAbbreviationRepository = journalAbbreviationRepository;

        this.executable.bind(ActionHelper.needsEntriesSelected(stateManager));
    }

    @Override
    public void execute() {
        if (stateManager.getActiveDatabase().isEmpty()) {
            return;
        }

        CleanupDialog cleanupDialog = new CleanupDialog(
                stateManager.getActiveDatabase().get(),
                preferences,
                dialogService,
                stateManager,
                undoManager,
                tabSupplier,
                taskExecutor,
                journalAbbreviationRepository
        );

        dialogService.showCustomDialogAndWait(cleanupDialog);
    }
}
