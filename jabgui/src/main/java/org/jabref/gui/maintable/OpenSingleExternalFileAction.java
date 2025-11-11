package org.jabref.gui.maintable;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.fieldeditors.LinkedFileViewModel;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import org.jspecify.annotations.NonNull;

public class OpenSingleExternalFileAction extends SimpleCommand {

    private final DialogService dialogService;
    private final GuiPreferences preferences;
    private final BibEntry entry;
    private final LinkedFile linkedFile;
    private final TaskExecutor taskExecutor;
    private final StateManager stateManager;

    public OpenSingleExternalFileAction(@NonNull DialogService dialogService,
                                        @NonNull GuiPreferences preferences,
                                        @NonNull BibEntry entry,
                                        @NonNull LinkedFile linkedFile,
                                        @NonNull TaskExecutor taskExecutor,
                                        @NonNull StateManager stateManager) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.entry = entry;
        this.linkedFile = linkedFile;
        this.taskExecutor = taskExecutor;
        this.stateManager = stateManager;

        this.setExecutable(true);
    }

    @Override
    public void execute() {
        stateManager.getActiveDatabase()
                    .ifPresent(databaseContext -> new LinkedFileViewModel(
                            linkedFile, entry, databaseContext, taskExecutor, dialogService, preferences)
                            .open());
    }
}
