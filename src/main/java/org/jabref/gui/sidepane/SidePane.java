package org.jabref.gui.sidepane;

import java.util.HashMap;
import java.util.Map;

import javax.swing.undo.UndoManager;

import javafx.beans.property.BooleanProperty;
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.preferences.PreferencesService;

import static org.jabref.gui.sidepane.SidePaneType.GROUPS;

public class SidePane extends VBox {
    // Don't use this map directly to lookup sidePaneViews, instead use getSidePaneView() for lazy loading
    private final Map<SidePaneType, SidePaneComponent> sidePaneComponentLookup = new HashMap<>();
    private final SidePaneViewModel viewModel;

    private final PreferencesService preferencesService;
    private final TaskExecutor taskExecutor;
    private final DialogService dialogService;
    private final StateManager stateManager;
    private final UndoManager undoManager;

    private final SidePaneContentFactory sidePaneContentFactory;

    public SidePane(PreferencesService preferencesService,
                    TaskExecutor taskExecutor,
                    DialogService dialogService,
                    StateManager stateManager,
                    UndoManager undoManager) {
        this.preferencesService = preferencesService;
        this.taskExecutor = taskExecutor;
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.undoManager = undoManager;
        this.sidePaneContentFactory = new SidePaneContentFactory(preferencesService, taskExecutor, dialogService, stateManager, undoManager);
        this.viewModel = new SidePaneViewModel(preferencesService);

        preferencesService.getSidePanePreferences().visiblePanes().forEach(this::show);
        updateView();
    }

    private SidePaneComponent getSidePaneComponent(SidePaneType sidePane) {
        SidePaneComponent sidePaneComponent = sidePaneComponentLookup.get(sidePane);
        if (sidePaneComponent == null) {
            sidePaneComponent = switch (sidePane) {
                case GROUPS -> new GroupsSidePaneComponent(new CloseSidePaneAction(sidePane), new MoveUpAction(sidePane), new MoveDownAction(sidePane), sidePaneContentFactory, preferencesService, dialogService);
                case WEB_SEARCH, OPEN_OFFICE -> new SidePaneComponent(sidePane, new CloseSidePaneAction(sidePane), new MoveUpAction(sidePane), new MoveDownAction(sidePane), sidePaneContentFactory);
            };
            sidePaneComponentLookup.put(sidePane, sidePaneComponent);
        }
        return sidePaneComponent;
    }

    private void showVisibleSidePanes() {
        getChildren().clear();
        viewModel.getVisiblePanes().forEach(type -> {
            SidePaneComponent view = getSidePaneComponent(type);
            getChildren().add(view);
        });
    }

    private void show(SidePaneType sidePane) {
        if (viewModel.show(sidePane)) {
            updateView();
            if (sidePane == GROUPS) {
                ((GroupsSidePaneComponent) getSidePaneComponent(sidePane)).afterOpening();
            }
        }
    }

    private void hide(SidePaneType sidePane) {
        if (viewModel.hide(sidePane)) {
            updateView();
        }
    }

    private void moveUp(SidePaneType sidePane) {
        if (viewModel.moveUp(sidePane)) {
            updateView();
        }
    }

    private void moveDown(SidePaneType sidePane) {
        if (viewModel.moveDown(sidePane)) {
            updateView();
        }
    }

    /**
     * If the given component is visible it will be hidden and the other way around.
     */
    private void toggle(SidePaneType sidePane) {
        if (viewModel.isSidePaneVisible(sidePane)) {
            hide(sidePane);
        } else {
            show(sidePane);
        }
    }

    private void updateView() {
        showVisibleSidePanes();
        setVisible(!viewModel.getVisiblePanes().isEmpty());
    }

    public BooleanProperty paneVisibleProperty(SidePaneType pane) {
        return switch (pane) {
            case GROUPS -> viewModel.groupsPaneVisibleProperty();
            case WEB_SEARCH -> viewModel.webSearchPaneVisibleProperty();
            case OPEN_OFFICE -> viewModel.openOfficePaneVisibleProperty();
        };
    }

    public ToggleCommand getToggleCommandFor(SidePaneType sidePane) {
        return new ToggleCommand(sidePane);
    }

    private class CloseSidePaneAction extends SimpleCommand {
        private final SidePaneType toCloseSidePane;

        public CloseSidePaneAction(SidePaneType toCloseSidePane) {
            this.toCloseSidePane = toCloseSidePane;
        }

        @Override
        public void execute() {
            hide(toCloseSidePane);
        }
    }

    private class MoveUpAction extends SimpleCommand {
        private final SidePaneType toMoveUpSidePane;

        public MoveUpAction(SidePaneType toMoveUpSidePane) {
            this.toMoveUpSidePane = toMoveUpSidePane;
        }

        @Override
        public void execute() {
            moveUp(toMoveUpSidePane);
        }
    }

    private class MoveDownAction extends SimpleCommand {
        private final SidePaneType toMoveDownSidePane;

        public MoveDownAction(SidePaneType toMoveDownSidePane) {
            this.toMoveDownSidePane = toMoveDownSidePane;
        }

        @Override
        public void execute() {
            moveDown(toMoveDownSidePane);
        }
    }

    public class ToggleCommand extends SimpleCommand {

        private final SidePaneType sidePane;

        public ToggleCommand(SidePaneType sidePane) {
            this.sidePane = sidePane;
        }

        @Override
        public void execute() {
            toggle(sidePane);
        }
    }
}
