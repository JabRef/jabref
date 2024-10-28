package org.jabref.gui.fieldeditors;

import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.Supplier;

import javafx.fxml.Initializable;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;

import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.fieldeditors.contextmenu.EditorContextAction;
import org.jabref.gui.keyboard.KeyBindingRepository;

public class EditorTextArea extends TextArea implements Initializable, ContextMenuAddable {

    private final ContextMenu contextMenu = new ContextMenu();
    /**
     * Variable that contains user-defined behavior for paste action.
     */
    private Runnable pasteActionHandler = () -> {
        // Set empty paste behavior by default
    };

    public EditorTextArea() {
        this("");
    }

    public EditorTextArea(final String text) {
        super(text);

        // Hide horizontal scrollbar and always wrap text
        setWrapText(true);

        ClipBoardManager.addX11Support(this);
    }

    @Override
    public void initContextMenu(final Supplier<List<MenuItem>> items, KeyBindingRepository keyBindingRepository) {
        setOnContextMenuRequested(event -> {
            contextMenu.getItems().setAll(EditorContextAction.getDefaultContextMenuItems(this));
            contextMenu.getItems().addAll(0, items.get());
            contextMenu.show(this, event.getScreenX(), event.getScreenY());
        });
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // not needed
    }

    /**
     * Set pasteActionHandler variable to passed handler
     *
     * @param handler an instance of PasteActionHandler that describes paste behavior
     */
    public void setPasteActionHandler(Runnable handler) {
        Objects.requireNonNull(handler);
        this.pasteActionHandler = handler;
    }

    /**
     * Override javafx TextArea method applying TextArea.paste() and pasteActionHandler after
     */
    @Override
    public void paste() {
        super.paste();
        pasteActionHandler.run();
    }
}
