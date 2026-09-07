package org.jabref.gui.fieldeditors;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Supplier;

import javafx.event.EventHandler;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;

import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.fieldeditors.contextmenu.EditorContextAction;
import org.jabref.gui.keyboard.KeyBindingRepository;

import org.jspecify.annotations.NonNull;

public class EditorTextArea extends TextArea implements Initializable, ContextMenuAddable {

    private final ContextMenu contextMenu = new ContextMenu();
    private boolean growWithContent;
    private double widthHint = -1;
    /// Variable that contains user-defined behavior for paste action.
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

        // Add our custom Tab key event handler to traverse focus when empty.
        addEventFilter(KeyEvent.KEY_PRESSED, new FieldTraversalEventHandler());
    }

    /// Sizes the area to its wrapped text (one row when empty, growing as text is typed) instead
    /// of the fixed preferred row count. Intended for natural-height lists that scroll as a whole.
    /// `widthHint` is the width to wrap at until the first layout has set the real width — pass
    /// the width the same field had before, so a rebuilt editor is right in its very first frame.
    public void setGrowWithContent(double widthHint) {
        this.growWithContent = true;
        this.widthHint = widthHint;
        setPrefRowCount(1);
        // The real width only exists after the first layout; if it differs from the hint the
        // wrapped height changes, so ask for another pass.
        widthProperty().addListener(_ -> requestLayout());
    }

    /// Wraps at the last laid-out width (or the hint before the first layout) instead of
    /// reporting a horizontal content bias: a biased child makes the enclosing grid measure
    /// every row at a width, which lets sibling editors such as the tags field wrap at their
    /// preferred width and briefly show too tall — a visible jump on every entry switch.
    @Override
    protected double computePrefHeight(double width) {
        double oneRow = super.computePrefHeight(width);
        double wrapWidth = width >= 0 ? width : (getWidth() > 0 ? getWidth() : widthHint);
        if (!growWithContent || wrapWidth <= 0) {
            return oneRow;
        }
        // Wrap at the skin's content width: the area's own insets plus the content padding.
        double horizontalInsets = snappedLeftInset() + snappedRightInset();
        if (lookup(".content") instanceof Region content) {
            horizontalInsets += content.snappedLeftInset() + content.snappedRightInset();
        }
        Text measure = new Text(textProperty().getValueSafe());
        measure.setFont(getFont());
        measure.setWrappingWidth(Math.max(0, wrapWidth - horizontalInsets));
        Text singleRow = new Text("X");
        singleRow.setFont(getFont());
        double rowHeight = singleRow.getLayoutBounds().getHeight();
        long rows = Math.max(1, Math.round(measure.getLayoutBounds().getHeight() / rowHeight));
        return oneRow + (rows - 1) * rowHeight;
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

    /// Set pasteActionHandler variable to passed handler
    ///
    /// @param handler an instance of PasteActionHandler that describes paste behavior
    public void setPasteActionHandler(@NonNull Runnable handler) {
        this.pasteActionHandler = handler;
    }

    /// Override javafx TextArea method applying TextArea.paste() and pasteActionHandler after
    @Override
    public void paste() {
        super.paste();
        pasteActionHandler.run();
    }

    // Custom event handler for Tab key presses.
    private static class FieldTraversalEventHandler implements EventHandler<KeyEvent> {
        @Override
        public void handle(KeyEvent event) {
            if (event.getCode() == KeyCode.TAB && !event.isShiftDown() && !event.isControlDown()) {
                event.consume();

                // Get the current text area
                Node node = (Node) event.getSource();
                KeyEvent newEvent = new KeyEvent(node,
                        event.getTarget(), event.getEventType(),
                        event.getCharacter(), event.getText(),
                        event.getCode(), event.isShiftDown(),
                        true, event.isAltDown(),
                        event.isMetaDown());

                node.fireEvent(newEvent);
            }
        }
    }
}
