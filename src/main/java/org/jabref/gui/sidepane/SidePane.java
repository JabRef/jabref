package org.jabref.gui.sidepane;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.undo.UndoManager;

import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.groups.GroupSidePane;
import org.jabref.gui.importer.fetcher.WebSearchPane;
import org.jabref.gui.openoffice.OpenOfficeSidePanel;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.preferences.PreferencesService;

/**
 * Manages which {@link SidePaneComponent}s are shown.
 */
public class SidePane extends VBox {

    private final Map<SidePaneType, SidePaneComponent> components = new LinkedHashMap<>();
    private final List<SidePaneComponent> visibleComponents = new LinkedList<>();

    private final PreferencesService preferencesService;
    private final TaskExecutor taskExecutor;
    private final DialogService dialogService;
    private final StateManager stateManager;
    private final UndoManager undoManager;

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

        setId("sidePane");

        preferencesService.getSidePanePreferences().visiblePanes().forEach(this::show);

        updateView();
    }

    public boolean isComponentVisible(SidePaneType type) {
        return visibleComponents.contains(getComponent(type));
    }

    public SidePaneComponent getComponent(SidePaneType type) {
        SidePaneComponent component = components.get(type);
        if (component == null) {
            component = switch (type) {
                case OPEN_OFFICE -> new OpenOfficeSidePanel(this, taskExecutor, preferencesService, dialogService, stateManager, undoManager);
                case WEB_SEARCH -> new WebSearchPane(this, preferencesService, dialogService, stateManager);
                case GROUPS -> new GroupSidePane(this, taskExecutor, stateManager, preferencesService, dialogService);
            };
            components.put(component.getType(), component);
        }
        return component;
    }

    /**
     * If the given component is visible it will be hidden and the other way around.
     */
    protected void toggle(SidePaneType type) {
        if (isComponentVisible(type)) {
            hide(type);
        } else {
            show(type);
        }
    }

    /**
     * Makes sure that the given component is visible.
     */
    protected void show(SidePaneType type) {
        SidePaneComponent component = getComponent(type);
        if (!visibleComponents.contains(component)) {
            // Add the new component
            visibleComponents.add(component);

            // Sort the visible components by their preferred position
            visibleComponents.sort(new PreferredIndexSort(preferencesService));

            updateView();

            component.afterOpening();
        }
    }

    /**
     * Makes sure that the given component is not visible.
     */
    protected void hide(SidePaneType type) {
        SidePaneComponent component = getComponent(type);
        if (visibleComponents.contains(component)) {
            component.beforeClosing();

            visibleComponents.remove(component);

            updateView();
        }
    }

    /**
     * Stores the current configuration of visible components in the preferences,
     * so that we show components at the preferred position next time.
     */
    private void updatePreferredPositions() {
        Map<SidePaneType, Integer> preferredPositions = new HashMap<>(preferencesService.getSidePanePreferences().getPreferredPositions());

        // Use the currently shown positions of all visible components
        int index = 0;
        for (SidePaneComponent comp : visibleComponents) {
            preferredPositions.put(comp.getType(), index);
            index++;
        }
        preferencesService.getSidePanePreferences().setPreferredPositions(preferredPositions);
    }

    /**
     * Moves the given component up.
     */
    protected void moveUp(SidePaneComponent component) {
        if (visibleComponents.contains(component)) {
            int currentPosition = visibleComponents.indexOf(component);
            if (currentPosition > 0) {
                int newPosition = currentPosition - 1;
                visibleComponents.remove(currentPosition);
                visibleComponents.add(newPosition, component);

                updatePreferredPositions();
                updateView();
            }
        }
    }

    /**
     * Moves the given component down.
     */
    protected void moveDown(SidePaneComponent comp) {
        if (visibleComponents.contains(comp)) {
            int currentPosition = visibleComponents.indexOf(comp);
            if (currentPosition < (visibleComponents.size() - 1)) {
                int newPosition = currentPosition + 1;
                visibleComponents.remove(currentPosition);
                visibleComponents.add(newPosition, comp);

                updatePreferredPositions();
                updateView();
            }
        }
    }

    /**
     * Updates the view to reflect changes to visible components.
     */
    private void updateView() {
        setComponents(visibleComponents);
        setVisible(!visibleComponents.isEmpty());
    }

    private void setComponents(Collection<SidePaneComponent> components) {
        getChildren().clear();

        for (SidePaneComponent component : components) {
            BorderPane node = new BorderPane();
            node.getStyleClass().add("sidePaneComponent");
            node.setTop(component.getHeader());
            node.setCenter(component.getContentPane());
            getChildren().add(node);
            VBox.setVgrow(node, component.getResizePolicy());
        }
    }

    /**
     * Helper class for sorting visible components based on their preferred position.
     */
    private static class PreferredIndexSort implements Comparator<SidePaneComponent> {

        private final Map<SidePaneType, Integer> preferredPositions;

        public PreferredIndexSort(PreferencesService preferencesService) {
            preferredPositions = preferencesService.getSidePanePreferences().getPreferredPositions();
        }

        @Override
        public int compare(SidePaneComponent comp1, SidePaneComponent comp2) {
            int pos1 = preferredPositions.getOrDefault(comp1.getType(), 0);
            int pos2 = preferredPositions.getOrDefault(comp2.getType(), 0);
            return Integer.compare(pos1, pos2);
        }
    }
}
