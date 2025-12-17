package org.jabref.gui.mergeentries;

import javax.swing.undo.UndoManager;

import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.mergeentries.multiwaymerge.MultiMergeEntriesView;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.NotificationService;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntry;

public class UpdateEntryFromWebSourcesAction extends SimpleCommand {
    private final StateManager stateManager;
    private final UndoManager undoManager;
    private final GuiPreferences preferences;
    private final NotificationService notificationService;
    private final TaskExecutor taskExecutor;

    public UpdateEntryFromWebSourcesAction(StateManager stateManager, UndoManager undoManager, GuiPreferences preferences, NotificationService notificationService, TaskExecutor taskExecutor) {
        this.stateManager = stateManager;
        this.undoManager = undoManager;
        this.preferences = preferences;
        this.notificationService = notificationService;
        this.taskExecutor = taskExecutor;

        this.executable.bind(ActionHelper.needsDatabase(stateManager));
    }

    @Override
    public void execute() {
        if (stateManager.getActiveDatabase().isEmpty()) {
            return;
        }

        if (stateManager.getSelectedEntries().isEmpty()) {
            notificationService.notify(Localization.lang("No entry selected"));
            return;
        }

        BibEntry entry = stateManager.getSelectedEntries().getFirst();

        MultiMergeEntriesView view = new MultiMergeEntriesView(preferences, taskExecutor);
    }
}
