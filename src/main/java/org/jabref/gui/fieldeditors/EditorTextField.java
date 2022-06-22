package org.jabref.gui.fieldeditors;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Supplier;

import javafx.fxml.Initializable;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.Globals;
import org.jabref.gui.fieldeditors.contextmenu.EditorContextAction;

public class EditorTextField extends javafx.scene.control.TextField implements Initializable, ContextMenuAddable {

    private final ContextMenu contextMenu = new ContextMenu();

    public EditorTextField() {
        this("");
    }

    public EditorTextField(final String text) {
        super(text);

        // Always fill out all the available space
        setPrefHeight(Double.POSITIVE_INFINITY);
        HBox.setHgrow(this, Priority.ALWAYS);

        ClipBoardManager.addX11Support(this);
    }

    @Override
    public void initContextMenu(final Supplier<List<MenuItem>> items) {
        setOnContextMenuRequested(event -> {
            contextMenu.getItems().setAll(EditorContextAction.getDefaultContextMenuItems(this, Globals.getKeyPrefs()));
            contextMenu.getItems().addAll(0, items.get());

            TextInputControlBehavior.showContextMenu(this, contextMenu, event);
        });
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // not needed
    }
}
