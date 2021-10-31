package org.jabref.gui.sidepane;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.undo.UndoManager;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.groups.GroupTreeView;
import org.jabref.gui.importer.fetcher.WebSearchPaneView;
import org.jabref.gui.openoffice.OpenOfficePanel;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.preferences.PreferencesService;

import static org.jabref.gui.sidepane.SidePaneType.GROUPS;
import static org.jabref.gui.sidepane.SidePaneType.OPEN_OFFICE;
import static org.jabref.gui.sidepane.SidePaneType.WEB_SEARCH;

public class SidePaneContainerView extends VBox {
    // Don't use this map directly to lookup sidePaneViews, instead use getSidePaneView() for lazy loading
    private final Map<SidePaneType, SidePaneView> sidePaneViewLookup = new HashMap<>();
    private final SidePaneContainerViewModel viewModel;

    private final PreferencesService preferencesService;
    private final TaskExecutor taskExecutor;
    private final DialogService dialogService;
    private final StateManager stateManager;
    private final UndoManager undoManager;

    // Probably should go into the view model
    private final BooleanProperty groupsPaneVisible = new SimpleBooleanProperty();
    private final BooleanProperty openOfficePaneVisible = new SimpleBooleanProperty();
    private final BooleanProperty webSearchPaneVisible = new SimpleBooleanProperty();

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
        this.viewModel = new SidePaneContainerViewModel(preferencesService);

        preferencesService.getSidePanePreferences().visiblePanes().forEach(this::show);
        updateView();
    }

    private SidePaneView getSidePaneView(SidePaneType sidePaneType) {
        SidePaneView sidePaneView = sidePaneViewLookup.get(sidePaneType);
        if (sidePaneView == null) {
            sidePaneView = switch (sidePaneType) {
                case GROUPS -> createGroupsSidePaneView();
                case WEB_SEARCH -> createWebSearchSidePaneView();
                case OPEN_OFFICE -> createOpenOfficeSidePaneView();
            };
            sidePaneViewLookup.put(sidePaneType, sidePaneView);
        }
        return sidePaneView;
    }

    private SidePaneView createGroupsSidePaneView() {
        GroupsSidePaneHeaderView groupsHeader = new GroupsSidePaneHeaderView(GROUPS,
                new CloseSidePaneAction(GROUPS), new MoveUpAction(GROUPS), new MoveDownAction(GROUPS), preferencesService, dialogService);
        GroupTreeView groupsContent = new GroupTreeView(taskExecutor, stateManager, preferencesService, dialogService);

        return new GroupsSidePaneView(groupsHeader, groupsContent, Priority.ALWAYS, preferencesService, dialogService);
    }

    private SidePaneView createOpenOfficeSidePaneView() {
        Node openOfficePaneContent = new OpenOfficePanel(preferencesService, preferencesService.getOpenOfficePreferences(),
                preferencesService.getKeyBindingRepository(), taskExecutor, dialogService, stateManager, undoManager).getContent();
        return createSidePaneView(OPEN_OFFICE, openOfficePaneContent);
    }

    private SidePaneView createWebSearchSidePaneView() {
        Node webSearchPaneContent = new WebSearchPaneView(preferencesService, dialogService, stateManager);
        return createSidePaneView(WEB_SEARCH, webSearchPaneContent);
    }

    private SidePaneView createSidePaneView(SidePaneType sidePaneType, Node paneContent) {
        SidePaneHeaderView paneHeaderView = new SidePaneHeaderView(sidePaneType, new CloseSidePaneAction(sidePaneType),
                new MoveUpAction(sidePaneType), new MoveDownAction(sidePaneType));
        return new SidePaneView(paneHeaderView, paneContent, Priority.NEVER);
    }

    private void showSidePanes(Set<SidePaneType> toShowSidePanes) {
        getChildren().clear();
        toShowSidePanes.forEach(type -> {
            SidePaneView view = getSidePaneView(type);
            getChildren().add(view);
            VBox.setVgrow(view, view.getResizePolicy());
        });
    }

    private void show(SidePaneType sidePane) {
        if (viewModel.show(sidePane)) {
            updateView();
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

    private void updateView() {
        showSidePanes(new HashSet<>(viewModel.getVisiblePanes()));
        setVisible(!viewModel.getVisiblePanes().isEmpty());
    }

    public BooleanProperty isPaneVisibleProperty(SidePaneType sidePaneType) {
        return switch (sidePaneType) {
            case GROUPS -> groupsPaneVisible;
            case WEB_SEARCH -> webSearchPaneVisible;
            case OPEN_OFFICE -> openOfficePaneVisible;
        };
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
}
