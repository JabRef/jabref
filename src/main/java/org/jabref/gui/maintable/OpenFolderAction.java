package org.jabref.gui.maintable;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.fieldeditors.LinkedFileViewModel;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.Preferences;

public class OpenFolderAction extends SimpleCommand {

    private final DialogService dialogService;
    private final StateManager stateManager;
    private final Preferences preferences;

    private final BibEntry entry;
    private final LinkedFile linkedFile;
    private final TaskExecutor taskExecutor;

    public OpenFolderAction(DialogService dialogService,
                            StateManager stateManager,
                            Preferences preferences,
                            TaskExecutor taskExecutor) {
        this(dialogService, stateManager, preferences, null, null, taskExecutor);
    }

    public OpenFolderAction(DialogService dialogService,
                            StateManager stateManager,
                            Preferences preferences,
                            BibEntry entry,
                            LinkedFile linkedFile,
                            TaskExecutor taskExecutor) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.preferences = preferences;
        this.entry = entry;
        this.linkedFile = linkedFile;
        this.taskExecutor = taskExecutor;

        if (this.linkedFile == null) {
            this.executable.bind(ActionHelper.isFilePresentForSelectedEntry(stateManager, preferences));
        } else {
            this.setExecutable(true);
        }
    }

    @Override
    public void execute() {
            stateManager.getActiveDatabase().ifPresent(databaseContext -> {
                if (entry == null) {
                    stateManager.getSelectedEntries().stream().filter(entry -> !entry.getFiles().isEmpty()).forEach(entry -> {
                        LinkedFileViewModel linkedFileViewModel = new LinkedFileViewModel(
                                entry.getFiles().getFirst(),
                                entry,
                                databaseContext,
                                taskExecutor,
                                dialogService,
                                preferences);
                        linkedFileViewModel.openFolder();
                    });
                } else {
                    LinkedFileViewModel linkedFileViewModel = new LinkedFileViewModel(
                            linkedFile,
                            entry,
                            databaseContext,
                            taskExecutor,
                            dialogService,
                            preferences);
                    linkedFileViewModel.openFolder();
                }
            });
    }
}
