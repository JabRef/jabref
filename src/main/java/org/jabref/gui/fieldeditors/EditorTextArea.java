package org.jabref.gui.fieldeditors;

import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.Supplier;

import javafx.fxml.Initializable;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.Globals;
import org.jabref.gui.fieldeditors.contextmenu.EditorContextAction;

public class EditorTextArea extends javafx.scene.control.TextArea implements Initializable, ContextMenuAddable {

    private final ContextMenu contextMenu = new ContextMenu();
    /**
     * Variable that contains user-defined behavior for paste action.
     */
    private PasteActionHandler pasteActionHandler = () -> {
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

    /**
     * Set pasteActionHandler variable to passed handler
     *
     * @param handler an instance of PasteActionHandler that describes paste behavior
     */
    public void setPasteActionHandler(PasteActionHandler handler) {
        Objects.requireNonNull(handler);
        this.pasteActionHandler = handler;
    }

    /**
     * Override javafx TextArea method applying TextArea.paste() and pasteActionHandler after
     */
    @Override
    public void paste() {
        super.paste();
        pasteActionHandler.handle();
    }

    /**
     * Interface presents user-described paste behaviour applying to paste method
     */
    @FunctionalInterface
    public interface PasteActionHandler {

        void handle();
    }
}
