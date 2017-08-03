package org.jabref.gui.fieldeditors;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import javafx.fxml.Initializable;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Font;

import org.jabref.Globals;
import org.jabref.preferences.JabRefPreferences;

import com.sun.javafx.scene.control.skin.TextAreaSkin;

public class EditorTextArea extends javafx.scene.control.TextArea implements Initializable {

    private static final String FONT_STYLE = "-fx-font-size: " + Globals.prefs.getInt(JabRefPreferences.MENU_FONT_SIZE) + "pt;";

    public EditorTextArea() {
        this("");
    }

    public EditorTextArea(String text) {
        super(text);

        setMinHeight(1);
        setMinWidth(200);

        this.setFont(Font.font("Verdana", Globals.prefs.getInt(JabRefPreferences.MENU_FONT_SIZE)));

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
                contextMenu.getItems().stream().forEach(item -> item.setStyle(FONT_STYLE));
            }
        };
        setSkin(customContextSkin);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }
}
