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

public class SidePaneContainerView extends VBox {
    // Don't use this map directly to lookup sidePaneViews, instead use getSidePaneView() for lazy loading
    private final Map<SidePaneType, SidePaneView> sidePaneViewLookup = new HashMap<>();
    private final SidePaneContainerViewModel viewModel;

    private final PreferencesService preferencesService;
    private final TaskExecutor taskExecutor;
    private final DialogService dialogService;
    private final StateManager stateManager;
    private final UndoManager undoManager;

    private final SidePaneViewContentFactory sidePaneViewContentFactory;

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
        this.sidePaneViewContentFactory = new SidePaneViewContentFactory(preferencesService, taskExecutor, dialogService, stateManager, undoManager);
        this.viewModel = new SidePaneContainerViewModel(preferencesService);

        preferencesService.getSidePanePreferences().visiblePanes().forEach(this::show);
        updateView();
    }

    private SidePaneView getSidePaneView(SidePaneType sidePane) {
        SidePaneView sidePaneView = sidePaneViewLookup.get(sidePane);
        if (sidePaneView == null) {
            sidePaneView = switch (sidePane) {
                case GROUPS -> new GroupsSidePaneView(new CloseSidePaneAction(sidePane), new MoveUpAction(sidePane), new MoveDownAction(sidePane), sidePaneViewContentFactory, preferencesService, dialogService);
                case WEB_SEARCH, OPEN_OFFICE -> new SidePaneView(sidePane, new CloseSidePaneAction(sidePane), new MoveUpAction(sidePane), new MoveDownAction(sidePane), sidePaneViewContentFactory);
            };
            sidePaneViewLookup.put(sidePane, sidePaneView);
        }
        return sidePaneView;
    }

    private void showVisibleSidePanes() {
        getChildren().clear();
        viewModel.getVisiblePanes().forEach(type -> {
            SidePaneView view = getSidePaneView(type);
            getChildren().add(view);
        });
    }

    private void show(SidePaneType sidePane) {
        if (viewModel.show(sidePane)) {
            updateView();
            if (sidePane == GROUPS) {
                ((GroupsSidePaneView) getSidePaneView(sidePane)).afterOpening();
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

    public BooleanProperty sidePaneVisibleProperty(SidePaneType sidePane) {
        return switch (sidePane) {
            case GROUPS -> viewModel.groupsSidePaneVisibleProperty();
            case WEB_SEARCH -> viewModel.webSearchSidePaneVisibleProperty();
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
