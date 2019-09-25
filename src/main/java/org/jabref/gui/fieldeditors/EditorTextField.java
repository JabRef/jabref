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
    }

    @Override
    public void addToContextMenu(final Supplier<List<MenuItem>> items) {
        setOnContextMenuRequested(event -> {
            contextMenu.getItems().setAll(TextInputControlBehavior.getDefaultContextMenuItems(this));
            contextMenu.getItems().addAll(0, items.get());

            TextInputControlBehavior.showContextMenu(this, contextMenu, event);
        });
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // not needed
    }
}
