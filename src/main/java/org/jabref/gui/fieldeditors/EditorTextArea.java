package org.jabref.gui.fieldeditors;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Supplier;

import javafx.fxml.Initializable;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import com.sun.javafx.scene.control.skin.TextAreaSkin;

public class EditorTextArea extends javafx.scene.control.TextArea implements Initializable {

    public EditorTextArea() {
        this("", false);
    }

    public EditorTextArea(final boolean hasSingleLine) {
        this("", hasSingleLine);
    }

    public EditorTextArea(final String text, final boolean hasSingleLine) {
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

        if (hasSingleLine) {
            setTextFormatter(createSingleLineTextFormatter());
        }
    }

    private static <T> TextFormatter<T> createSingleLineTextFormatter() {
        return new TextFormatter<>(change -> {
            if (change.isAdded()) {
                change.setText(change.getText().replaceAll("\\s+", " "));
            }
            return change;
        });
    }

    /**
     * Adds the given list of menu items to the context menu. The usage of {@link Supplier} prevents that the menus need
     * to be instantiated at this point. They are populated when the user needs them which prevents many unnecessary
     * allocations when the main table is just scrolled with the entry editor open.
     */
    public void addToContextMenu(Supplier<List<MenuItem>> items) {
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
}
