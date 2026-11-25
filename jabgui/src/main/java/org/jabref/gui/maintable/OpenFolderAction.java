package org.jabref.gui.maintable;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.fieldeditors.LinkedFileViewModel;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

public class OpenFolderAction extends SimpleCommand {

    private final DialogService dialogService;
    private final StateManager stateManager;
    private final GuiPreferences preferences;

    private final BibEntry entry;
    private final LinkedFile linkedFile;
    private final TaskExecutor taskExecutor;
    private final NativeDesktop nativeDesktop;

    public OpenFolderAction(DialogService dialogService,
                            StateManager stateManager,
                            GuiPreferences preferences,
                            TaskExecutor taskExecutor,
                            NativeDesktop nativeDesktop) {
        this(dialogService, stateManager, preferences, null, null, taskExecutor, nativeDesktop);
    }

    public OpenFolderAction(DialogService dialogService,
                            StateManager stateManager,
                            GuiPreferences preferences,
                            BibEntry entry,
                            LinkedFile linkedFile,
                            TaskExecutor taskExecutor,
                            NativeDesktop nativeDesktop) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.preferences = preferences;
        this.entry = entry;
        this.linkedFile = linkedFile;
        this.taskExecutor = taskExecutor;
        this.nativeDesktop = nativeDesktop;

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
                            preferences,
                            nativeDesktop);
                    linkedFileViewModel.openFolder();
                });
            } else {
                LinkedFileViewModel linkedFileViewModel = new LinkedFileViewModel(
                        linkedFile,
                        entry,
                        databaseContext,
                        taskExecutor,
                        dialogService,
                        preferences,
                        nativeDesktop);
                linkedFileViewModel.openFolder();
            }
        });
    }
}
