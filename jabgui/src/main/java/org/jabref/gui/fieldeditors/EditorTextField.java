package org.jabref.gui.fieldeditors;

import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javafx.fxml.Initializable;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.fieldeditors.contextmenu.EditorContextAction;
import org.jabref.gui.keyboard.KeyBindingRepository;

public class EditorTextField extends TextField implements Initializable, ContextMenuAddable {

    private static Runnable nextTabSelector;
    private static Predicate<TextField> isLastFieldChecker;
    private final ContextMenu contextMenu = new ContextMenu();

    // Callbacks for tab navigation - replaces static dependency
    private static Predicate<TextField> isLastFieldChecker;
    private static Runnable nextTabSelector;

    private Runnable additionalPasteActionHandler = () -> {
        // No additional paste behavior
    };

    public EditorTextField() {
        this("");
        this.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            String keyText = event.getText();
            if ("\t".equals(keyText) &&
                    isLastFieldChecker != null &&
                    isLastFieldChecker.test(this)) {
                if (nextTabSelector != null) {
                    nextTabSelector.run();
                }
                event.consume();
            }
        });
    }

    public EditorTextField(final String text) {
        super(text);

        // Always fill out all the available space
        setPrefHeight(Double.POSITIVE_INFINITY);
        HBox.setHgrow(this, Priority.ALWAYS);

        ClipBoardManager.addX11Support(this);
    }

    public static void setupTabNavigation(Predicate<TextField> isLastFieldChecker, Runnable nextTabSelector) {
        EditorTextField.isLastFieldChecker = isLastFieldChecker;
        EditorTextField.nextTabSelector = nextTabSelector;
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

    public void setAdditionalPasteActionHandler(Runnable handler) {
        Objects.requireNonNull(handler);
        this.additionalPasteActionHandler = handler;
    }

    @Override
    public void paste() {
        super.paste();
        additionalPasteActionHandler.run();
    }
}
