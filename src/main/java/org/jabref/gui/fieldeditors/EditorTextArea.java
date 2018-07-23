package org.jabref.gui.fieldeditors;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Supplier;

import javafx.fxml.Initializable;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.skin.TextAreaSkin;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class EditorTextArea extends javafx.scene.control.TextArea implements Initializable, ContextMenuAddable {

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
                // TODO: temporarily removed, as this is internal API
//                TextAreaSkin skin = (TextAreaSkin) getSkin();
//                if (event.isShiftDown()) {
//                    // Shift + Tab > previous text area
//                    skin.getBehavior().traversePrevious();
//                } else {
//                    if (event.isControlDown()) {
//                        // Ctrl + Tab > insert tab
//                        skin.getBehavior().callAction("InsertTab");
//                    } else {
//                        // Tab > next text area
//                        skin.getBehavior().traverseNext();
//                    }
//                }
                event.consume();
            }
        });
    }

    @Override
    public void addToContextMenu(final Supplier<List<MenuItem>> items) {
        TextAreaSkin customContextSkin = new TextAreaSkin(this) {
            // TODO: temporarily removed, internal API
//            @Override
//            public void populateContextMenu(ContextMenu contextMenu) {
//                super.populateContextMenu(contextMenu);
//                contextMenu.getItems().addAll(0, items.get());
//            }
        };
        setSkin(customContextSkin);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // not needed
    }
}
