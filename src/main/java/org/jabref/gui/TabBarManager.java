package org.jabref.gui;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.ListChangeListener;
import javafx.scene.control.TabPane;

import org.jabref.gui.util.BindingsHelper;
import org.jabref.model.database.BibDatabaseContext;

public class TabBarManager {

    private final TabPane tabPane;
    private final StateManager stateManager;
    private final WorkspacePreferences workspacePreferences;
    private final IntegerProperty numberOfOpenDatabases;

    public TabBarManager(TabPane tabPane,
                         StateManager stateManager,
                         WorkspacePreferences workspacePreferences) {
        this.tabPane = tabPane;
        this.stateManager = stateManager;
        this.workspacePreferences = workspacePreferences;
        this.numberOfOpenDatabases = new SimpleIntegerProperty();

        stateManager.getOpenDatabases().addListener((ListChangeListener<BibDatabaseContext>) change -> {
            this.numberOfOpenDatabases.set(stateManager.getOpenDatabases().size());
            updateTabBarState();
        });

        BindingsHelper.subscribeFuture(workspacePreferences.confirmHideTabBarProperty(), hideTabBar -> updateTabBarState());
        maintainInitialTabBarState(workspacePreferences.shouldHideTabBar());
    }

    public void updateTabBarState() {
        if (workspacePreferences.shouldHideTabBar() && numberOfOpenDatabases.get() == 1) {
            if (!tabPane.getStyleClass().contains("hide-tab-bar")) {
                tabPane.getStyleClass().add("hide-tab-bar");
            }
        } else {
            tabPane.getStyleClass().remove("hide-tab-bar");
        }
    }

    public void maintainInitialTabBarState(boolean show) {
        if (show) {
            if (stateManager.getOpenDatabases().size() == 1) {
                if (!tabPane.getStyleClass().contains("hide-tab-bar")) {
                    tabPane.getStyleClass().add("hide-tab-bar");
                }
            } else {
                tabPane.getStyleClass().remove("hide-tab-bar");
            }
        }
    }
}
