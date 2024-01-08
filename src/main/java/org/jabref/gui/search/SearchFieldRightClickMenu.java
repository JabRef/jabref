package org.jabref.gui.search;

import javax.swing.undo.UndoManager;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.edit.EditAction;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.logic.l10n.Localization;

import org.controlsfx.control.textfield.CustomTextField;

public class SearchFieldRightClickMenu {
    public static ContextMenu create(KeyBindingRepository keyBindingRepository,
                                     StateManager stateManager,
                                     CustomTextField searchField,
                                     LibraryTabContainer tabContainer,
                                     UndoManager undoManager) {
        ActionFactory factory = new ActionFactory(keyBindingRepository);
        ContextMenu contextMenu = new ContextMenu();

        contextMenu.getItems().addAll(
                factory.createMenuItem(StandardActions.UNDO, new EditAction(StandardActions.UNDO, tabContainer::getCurrentLibraryTab, stateManager, undoManager)),
                factory.createMenuItem(StandardActions.REDO, new EditAction(StandardActions.REDO, tabContainer::getCurrentLibraryTab, stateManager, undoManager)),
                factory.createMenuItem(StandardActions.CUT, new EditAction(StandardActions.CUT, tabContainer::getCurrentLibraryTab, stateManager, undoManager)),
                factory.createMenuItem(StandardActions.COPY, new EditAction(StandardActions.COPY, tabContainer::getCurrentLibraryTab, stateManager, undoManager)),
                factory.createMenuItem(StandardActions.PASTE, new EditAction(StandardActions.PASTE, tabContainer::getCurrentLibraryTab, stateManager, undoManager)),
                factory.createMenuItem(StandardActions.DELETE, new EditAction(StandardActions.DELETE, tabContainer::getCurrentLibraryTab, stateManager, undoManager)),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.SELECT_ALL, new EditAction(StandardActions.SELECT_ALL, null, stateManager, undoManager)),
                createSearchFromHistorySubMenu(factory, stateManager, searchField)
        );

        return contextMenu;
    }

    private static Menu createSearchFromHistorySubMenu(ActionFactory factory,
                                                       StateManager stateManager,
                                                       CustomTextField searchField) {
        Menu searchFromHistorySubMenu = factory.createMenu(() -> Localization.lang("Search from history..."));

        int num = stateManager.getLastSearchHistory(10).size();
        if (num == 0) {
            MenuItem item = new MenuItem(Localization.lang("your search history is empty"));
            searchFromHistorySubMenu.getItems().addAll(item);
        } else {
            for (int i = 0; i < num; i++) {
                int finalI = i;
                MenuItem item = factory.createMenuItem(() -> stateManager.getLastSearchHistory(10).get(finalI), new SimpleCommand() {
                    @Override
                    public void execute() {
                        searchField.setText(stateManager.getLastSearchHistory(10).get(finalI));
                    }
                });
                searchFromHistorySubMenu.getItems().addAll(item);
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
