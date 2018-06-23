package org.jabref.gui.fieldeditors;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Supplier;

import javafx.fxml.Initializable;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import com.sun.javafx.scene.control.skin.TextFieldSkin;

public class EditorTextField extends javafx.scene.control.TextField implements Initializable, ContextMenuAddable {

    public EditorTextField() {
        this("");
    }

    public EditorTextField(final String text) {
        super(text);

        setMinHeight(1);
        setMinWidth(200);

        // Should behave as a normal text field with respect to TAB behaviour
        addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.TAB) {
                TextFieldSkin skin = (TextFieldSkin) getSkin();
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
        TextFieldSkin customContextSkin = new TextFieldSkin(this) {
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
