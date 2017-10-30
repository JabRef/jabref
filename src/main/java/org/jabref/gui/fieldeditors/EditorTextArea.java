package org.jabref.gui.fieldeditors;

import com.sun.javafx.scene.control.skin.TextAreaSkin;
import javafx.fxml.Initializable;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import javax.annotation.Nonnull;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class EditorTextArea extends javafx.scene.control.TextArea implements Initializable {

    /**
     *  Variable that contains user-defined behavior for paste action.
     *  Set empty paste behavior by default
     */
    @Nonnull
    private PasteActionHandler pasteActionHandler = new EmptyPasteHandler();

    public EditorTextArea() {
        this("");
    }

    public EditorTextArea(String text) {
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

    /**
     * Adds the given list of menu items to the context menu.
     */
    public void addToContextMenu(List<MenuItem> items) {
        TextAreaSkin customContextSkin = new TextAreaSkin(this) {
            @Override
            public void populateContextMenu(ContextMenu contextMenu) {
                super.populateContextMenu(contextMenu);
                contextMenu.getItems().addAll(0, items);
            }
        };
        setSkin(customContextSkin);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    /**
     * Set pasteActionHandler variable to passed handler
     * @param  handler an instance of PasteActionHandler that describes paste behavior
     */
    public void setPasteActionHandler(@Nonnull PasteActionHandler handler) {
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

    /**
     * Empty interface implementation to do nothing external on paste method
     */
    private static class EmptyPasteHandler implements PasteActionHandler {
        @Override
        public void handle() {

        }
    }
}
