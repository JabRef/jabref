package org.jabref.gui.fieldeditors;

import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.Supplier;

import javafx.fxml.Initializable;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import com.sun.javafx.scene.control.skin.TextAreaSkin;

public class EditorTextArea extends javafx.scene.control.TextArea implements Initializable, ContextMenuAddable {

    /**
     *  Variable that contains user-defined behavior for paste action.
     */
    private PasteActionHandler pasteActionHandler = () -> {
        // Set empty paste behavior by default
    };

    public EditorTextArea() {
        this("");
    }

    public EditorTextArea(final String text) {
        super(text);

        setMinHeight(1);
        setMinWidth(200);

        // Hide horizontal scrollbar and always wrap text
        setWrapText(true);

        // Should behave as a normal text field with respect to TAB behaviour
        addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.TAB) {
                TextAreaSkin skin = (TextAreaSkin) getSkin();
                if (event.isShiftDown()) {
                    // Shift + Tab > previous text area
                    skin.getBehavior().traversePrevious();
                } else {
                    if (event.isControlDown()) {
                        // Ctrl + Tab > insert tab
                        skin.getBehavior().callAction("InsertTab");
                    } else {
                        // Tab > next text area
                        skin.getBehavior().traverseNext();
                    }
                }
                event.consume();
            }
        });
    }

    @Override
    public void addToContextMenu(final Supplier<List<MenuItem>> items) {
        TextAreaSkin customContextSkin = new TextAreaSkin(this) {
            @Override
            public void populateContextMenu(ContextMenu contextMenu) {
                super.populateContextMenu(contextMenu);
                contextMenu.getItems().addAll(0, items.get());
            }
        };
        setSkin(customContextSkin);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // not needed
    }

    /**
     * Set pasteActionHandler variable to passed handler
     * @param  handler an instance of PasteActionHandler that describes paste behavior
     */
    public void setPasteActionHandler(PasteActionHandler handler) {
        Objects.requireNonNull(handler);
        this.pasteActionHandler = handler;
    }
  
    /**
     *  Override javafx TextArea method applying TextArea.paste() and pasteActionHandler after
     */
    @Override
    public void paste() {
        super.paste();
        pasteActionHandler.handle();
    }
  
    /**
     *  Interface presents user-described paste behaviour applying to paste method
     */
    @FunctionalInterface
    public interface PasteActionHandler {

        void handle();
    }
}
