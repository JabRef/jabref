package org.jabref.gui.sidepane;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.undo.UndoManager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.groups.GroupTreeView;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.preferences.PreferencesService;

public class SidePaneContainerView extends VBox {
    private Map<SidePaneType, SidePaneView> sidePaneViewLookup;
    // TODO('Use preferencesService.getSidePanePreferences().visiblePanes() as the single source of truth')
    private final ObservableList<SidePaneType> visiblePanes = FXCollections.observableArrayList();

    private final PreferencesService preferencesService;
    private final TaskExecutor taskExecutor;
    private final DialogService dialogService;
    private final StateManager stateManager;
    private final UndoManager undoManager;

    public SidePaneContainerView(PreferencesService preferencesService,
                                 TaskExecutor taskExecutor,
                                 DialogService dialogService,
                                 StateManager stateManager,
                                 UndoManager undoManager) {
        this.preferencesService = preferencesService;
        this.taskExecutor = taskExecutor;
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.undoManager = undoManager;
        initSidePanes();
    }

    private void initSidePanes() {
        SidePaneView groupsPaneView = createGroupsPaneView();
    }

    private SidePaneView createGroupsPaneView() {
        GroupsSidePaneHeaderView groupsHeader = new GroupsSidePaneHeaderView(SidePaneType.GROUPS,
                new CloseSidePaneAction(SidePaneType.GROUPS), null, null, preferencesService, dialogService);
        GroupTreeView groupsContent = new GroupTreeView(taskExecutor, stateManager, preferencesService, dialogService);

        return new GroupsSidePaneView(groupsHeader, groupsContent, Priority.ALWAYS, preferencesService, dialogService);
    }

    private void showSidePanes(Set<SidePaneType> toShowSidePanes) {
        getChildren().clear();
        toShowSidePanes.forEach(type -> {
            SidePaneView view = sidePaneViewLookup.get(type);
            getChildren().add(view);
            VBox.setVgrow(view, view.getResizePolicy());
        });
    }

    private void show(SidePaneType paneType) {
        if (!visiblePanes.contains(paneType)) {
            visiblePanes.add(paneType);
            preferencesService.getSidePanePreferences().visiblePanes().add(paneType);
            // TODO('Sort')
            updateView();
        }
    }

    private void hide(SidePaneType paneType) {
        if (visiblePanes.contains(paneType)) {
            // TODO('Before closing')
            visiblePanes.remove(paneType);
            preferencesService.getSidePanePreferences().visiblePanes().remove(paneType);
            updateView();
        }
    }

    private void moveUp(SidePaneType type) {

    }

    public ObservableList<SidePaneType> getVisiblePanes() {
        return visiblePanes;
    }

    private void updateView() {
        showSidePanes(new HashSet<>(visiblePanes));
        setVisible(!visiblePanes.isEmpty());
    }

    private class CloseSidePaneAction extends SimpleCommand {
        private final SidePaneType toClosePaneType;

        public CloseSidePaneAction(SidePaneType toClosePaneType) {
            this.toClosePaneType = toClosePaneType;
        }

        @Override
        public void execute() {
            hide(toClosePaneType);
        }
    }
}
