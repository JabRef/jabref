package org.jabref.gui.search;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.edit.EditAction;
import org.jabref.gui.keyboard.KeyBindingRepository;

public class SearchFieldRightClickMenu {
    public static ContextMenu create(KeyBindingRepository keyBindingRepository,
                                     StateManager stateManager,
                                     Label currentResults) {
        ActionFactory factory = new ActionFactory(keyBindingRepository);
        ContextMenu contextMenu = new ContextMenu();

        contextMenu.getItems().addAll(
                factory.createMenuItem(StandardActions.UNDO, new EditAction(StandardActions.UNDO, null, stateManager)),
                factory.createMenuItem(StandardActions.REDO, new EditAction(StandardActions.REDO, null, stateManager)),
                factory.createMenuItem(StandardActions.CUT, new EditAction(StandardActions.CUT, null, stateManager)),
                factory.createMenuItem(StandardActions.COPY, new EditAction(StandardActions.COPY, null, stateManager)),
                factory.createMenuItem(StandardActions.PASTE, new EditAction(StandardActions.PASTE, null, stateManager)),
                factory.createMenuItem(StandardActions.DELETE, new EditAction(StandardActions.DELETE, null, stateManager)),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.SELECT_ALL, new EditAction(StandardActions.SELECT_ALL, null, stateManager)),
                createSearchFromHistorySubMenu(factory, stateManager, currentResults)
        );

        return contextMenu;
    }

    private static Menu createSearchFromHistorySubMenu(ActionFactory factory,
                                                       StateManager stateManager,
                                                       Label currentResults) {
        Menu searchFromHistorySubMenu = factory.createMenu(() -> "Search from history...");

        int num = stateManager.getLastSearchHistory(10).size();
        if (num == 0) {
            MenuItem item = new MenuItem("your search history is empty");
            searchFromHistorySubMenu.getItems().addAll(item);
        } else {
            for (int i = 0; i < num; i++) {
                int finalI = i;
                MenuItem item = factory.createMenuItem(() -> stateManager.getLastSearchHistory(10).get(finalI), new SimpleCommand() {
                    @Override
                    public void execute() {
                        currentResults.setText(stateManager.getLastSearchHistory(10).get(finalI));
                    }
                });
                searchFromHistorySubMenu.getItems().addAll(item);
            }
        }
        return searchFromHistorySubMenu;
    }
}
