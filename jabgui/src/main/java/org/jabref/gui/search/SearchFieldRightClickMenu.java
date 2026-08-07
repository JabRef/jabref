package org.jabref.gui.search;

import java.util.List;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.fieldeditors.contextmenu.EditorContextAction;
import org.jabref.logic.l10n.Localization;

import org.controlsfx.control.textfield.CustomTextField;

public class SearchFieldRightClickMenu {
    public static ContextMenu create(StateManager stateManager, CustomTextField searchField) {
        ActionFactory factory = new ActionFactory();
        ContextMenu contextMenu = new ContextMenu();

        contextMenu.getItems().addAll(
                factory.createMenuItem(StandardActions.UNDO, new EditorContextAction(StandardActions.UNDO, searchField)),
                factory.createMenuItem(StandardActions.REDO, new EditorContextAction(StandardActions.REDO, searchField)),
                factory.createMenuItem(StandardActions.CUT, new EditorContextAction(StandardActions.CUT, searchField)),
                factory.createMenuItem(StandardActions.COPY, new EditorContextAction(StandardActions.COPY, searchField)),
                factory.createMenuItem(StandardActions.PASTE, new EditorContextAction(StandardActions.PASTE, searchField)),
                factory.createMenuItem(StandardActions.DELETE, new EditorContextAction(StandardActions.DELETE, searchField)),
                factory.createMenuItem(StandardActions.SELECT_ALL, new EditorContextAction(StandardActions.SELECT_ALL, searchField)),
                new SeparatorMenuItem(),
                createSearchFromHistorySubMenu(stateManager, searchField));
        return contextMenu;
    }

    public static Menu createSearchFromHistorySubMenu(StateManager stateManager, CustomTextField searchField) {
        ActionFactory factory = new ActionFactory();
        Menu searchFromHistorySubMenu = factory.createMenu(() -> Localization.lang("Search from history..."));

        final int numberOfLastQueries = 10;
        List<String> searchHistory = stateManager.getLastSearchHistory(numberOfLastQueries);
        if (searchHistory.isEmpty()) {
            MenuItem item = new MenuItem(Localization.lang("your search history is empty"));
            searchFromHistorySubMenu.getItems().add(item);
        } else {
            for (String query : searchHistory) {
                MenuItem item = factory.createMenuItem(() -> query, new SimpleCommand() {
                    @Override
                    public void execute() {
                        searchField.setText(query);
                    }
                });
                searchFromHistorySubMenu.getItems().add(item);
            }
            MenuItem clear = factory.createMenuItem(() -> Localization.lang("Clear history"), new SimpleCommand() {
                @Override
                public void execute() {
                    stateManager.clearSearchHistory();
                }
            });
            searchFromHistorySubMenu.getItems().addAll(new SeparatorMenuItem(), clear);
        }
        return searchFromHistorySubMenu;
    }
}
