package org.jabref.gui.fieldeditors;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Supplier;

import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Skin;
import javafx.scene.control.TextField;
import javafx.scene.control.skin.TextFieldSkin;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.shape.Rectangle;

import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.fieldeditors.contextmenu.EditorContextAction;
import org.jabref.gui.keyboard.KeyBindingRepository;

import org.jspecify.annotations.NonNull;

public class EditorTextField extends TextField implements Initializable, ContextMenuAddable {

    // Extra pixels added to the text clip so the caret and last glyph are not cut off
    private static final double CLIP_EXTRA_WIDTH = 2.0;

    private final ContextMenu contextMenu = new ContextMenu();

    private Runnable additionalPasteActionHandler = () -> {
        // No additional paste behavior
    };

    public EditorTextField() {
        this("");
    }

    public EditorTextField(final String text) {
        super(text);

        // Always fill out all the available space
        setPrefHeight(Double.POSITIVE_INFINITY);
        // Detach the reported preferred width from the current text's length (JavaFX's default
        // TextField.computePrefWidth grows with content) so Hgrow/fillWidth fill the available
        // space instead of growing the field - and its containers - to fit long values.
        setPrefWidth(1);
        HBox.setHgrow(this, Priority.ALWAYS);

        ClipBoardManager.addX11Support(this);
    }

    /// Workaround for #14318: `TextFieldSkin` clips the text to the content area,
    /// which cuts off the caret and the last character at the end of long text.
    @Override
    protected Skin<?> createDefaultSkin() {
        return new TextFieldSkin(this) {
            @Override
            protected void layoutChildren(double x, double y, double w, double h) {
                super.layoutChildren(x, y, w, h);
                for (Node child : getChildren()) {
                    if (child instanceof Group group && group.getClip() instanceof Rectangle clip) {
                        clip.setWidth(w + CLIP_EXTRA_WIDTH);
                    }
                }
            }
        };
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

    public void setAdditionalPasteActionHandler(@NonNull Runnable handler) {
        this.additionalPasteActionHandler = handler;
    }

    @Override
    public void paste() {
        super.paste();
        additionalPasteActionHandler.run();
    }
}
