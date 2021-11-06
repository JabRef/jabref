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

import com.tobiasdiez.easybind.EasyBind;

import static org.jabref.gui.sidepane.SidePaneType.GROUPS;
import static org.jabref.gui.sidepane.SidePaneType.OPEN_OFFICE;
import static org.jabref.gui.sidepane.SidePaneType.WEB_SEARCH;

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
        this.viewModel = new SidePaneViewModel(preferencesService, stateManager);

        EasyBind.subscribe(stateManager.sidePaneComponentVisiblePropertyFor(GROUPS), isShow -> showOrHidePane(GROUPS, isShow));
        EasyBind.subscribe(stateManager.sidePaneComponentVisiblePropertyFor(WEB_SEARCH), isShow -> showOrHidePane(WEB_SEARCH, isShow));
        EasyBind.subscribe(stateManager.sidePaneComponentVisiblePropertyFor(OPEN_OFFICE), isShow -> showOrHidePane(OPEN_OFFICE, isShow));

        preferencesService.getSidePanePreferences().visiblePanes().forEach(this::show);
        updateView();
    }

    private void showOrHidePane(SidePaneType pane, boolean isShow) {
        if (isShow) {
            show(pane);
        } else {
            hide(pane);
        }
    }

    private SidePaneComponent getSidePaneComponent(SidePaneType pane) {
        SidePaneComponent sidePaneComponent = sidePaneComponentLookup.get(pane);
        if (sidePaneComponent == null) {
            sidePaneComponent = switch (pane) {
                case GROUPS -> new GroupsSidePaneComponent(new ClosePaneAction(pane), new MoveUpAction(pane), new MoveDownAction(pane), sidePaneContentFactory, preferencesService, dialogService);
                case WEB_SEARCH, OPEN_OFFICE -> new SidePaneComponent(pane, new ClosePaneAction(pane), new MoveUpAction(pane), new MoveDownAction(pane), sidePaneContentFactory);
            };
            sidePaneComponentLookup.put(pane, sidePaneComponent);
        }
        return sidePaneComponent;
    }

    private void showVisiblePanes() {
        getChildren().clear();
        viewModel.getVisiblePanes().forEach(type -> {
            SidePaneComponent view = getSidePaneComponent(type);
            getChildren().add(view);
        });
    }

    private void show(SidePaneType pane) {
        if (viewModel.show(pane)) {
            updateView();
            if (pane == GROUPS) {
                ((GroupsSidePaneComponent) getSidePaneComponent(pane)).afterOpening();
            }
        }
    }

    private void hide(SidePaneType pane) {
        if (viewModel.hide(pane)) {
            updateView();
        }
    }

    private void moveUp(SidePaneType pane) {
        if (viewModel.moveUp(pane)) {
            updateView();
        }
    }

    private void moveDown(SidePaneType pane) {
        if (viewModel.moveDown(pane)) {
            updateView();
        }
    }

    /**
     * If the given component is visible it will be hidden and the other way around.
     */
    private void toggle(SidePaneType pane) {
        if (viewModel.isPaneVisible(pane)) {
            hide(pane);
        } else {
            show(pane);
        }
    }

    private void updateView() {
        showVisiblePanes();
        setVisible(!viewModel.getVisiblePanes().isEmpty());
    }

    public BooleanProperty paneVisibleProperty(SidePaneType pane) {
        return stateManager.sidePaneComponentVisiblePropertyFor(pane);
    }

    public ToggleCommand getToggleCommandFor(SidePaneType sidePane) {
        return new ToggleCommand(sidePane);
    }

    private class ClosePaneAction extends SimpleCommand {
        private final SidePaneType toClosePane;

        public ClosePaneAction(SidePaneType toClosePane) {
            this.toClosePane = toClosePane;
        }

        @Override
        public void execute() {
            hide(toClosePane);
        }
    }

    private class MoveUpAction extends SimpleCommand {
        private final SidePaneType toMoveUpPane;

        public MoveUpAction(SidePaneType toMoveUpPane) {
            this.toMoveUpPane = toMoveUpPane;
        }

        @Override
        public void execute() {
            moveUp(toMoveUpPane);
        }
    }

    private class MoveDownAction extends SimpleCommand {
        private final SidePaneType toMoveDownPane;

        public MoveDownAction(SidePaneType toMoveDownPane) {
            this.toMoveDownPane = toMoveDownPane;
        }

        @Override
        public void execute() {
            moveDown(toMoveDownPane);
        }
    }

    public class ToggleCommand extends SimpleCommand {

        private final SidePaneType pane;

        public ToggleCommand(SidePaneType pane) {
            this.pane = pane;
        }

        @Override
        public void execute() {
            toggle(pane);
        }
    }
}
