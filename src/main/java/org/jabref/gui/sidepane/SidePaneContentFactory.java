package org.jabref.gui.sidepane;

import javax.swing.undo.UndoManager;

import javafx.scene.Node;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.groups.GroupTreeView;
import org.jabref.gui.importer.fetcher.WebSearchPaneView;
import org.jabref.gui.openoffice.OpenOfficePanel;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.preferences.PreferencesService;

public class SidePaneContentFactory {
    private final PreferencesService preferences;
    private final TaskExecutor taskExecutor;
    private final DialogService dialogService;
    private final StateManager stateManager;
    private final UndoManager undoManager;

    public SidePaneContentFactory(PreferencesService preferences,
                                  TaskExecutor taskExecutor,
                                  DialogService dialogService,
                                  StateManager stateManager,
                                  UndoManager undoManager) {
        this.preferences = preferences;
        this.taskExecutor = taskExecutor;
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.undoManager = undoManager;
    }

    public Node create(SidePaneType sidePaneType) {
        return switch (sidePaneType) {
            case GROUPS -> new GroupTreeView(
                    taskExecutor,
                    stateManager,
                    preferences,
                    dialogService);
            case OPEN_OFFICE -> new OpenOfficePanel(
                    preferences,
                    preferences.getOpenOfficePreferences(),
                    preferences.getKeyBindingRepository(),
                    taskExecutor,
                    dialogService,
                    stateManager,
                    undoManager).getContent();
            case WEB_SEARCH -> new WebSearchPaneView(
                    preferences,
                    dialogService,
                    stateManager);
        };
    }
}
