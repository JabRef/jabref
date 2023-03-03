package org.jabref.gui.search;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.SeparatorMenuItem;

import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.edit.EditAction;
import org.jabref.gui.keyboard.KeyBindingRepository;

public class SearchFieldRightClickMenu {
    @SuppressWarnings({"checkstyle:WhitespaceAfter", "checkstyle:WhitespaceAround"})
    public static ContextMenu create(KeyBindingRepository keyBindingRepository,
                                     StateManager stateManager
                                     ) {
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
                factory.createMenuItem(()-> "Search from history...", new SimpleCommand() {
                    @Override
                    public void execute() {
                        // TODO Auto-generated method stub
                    }
                })
        );

        return contextMenu;
    }
}
