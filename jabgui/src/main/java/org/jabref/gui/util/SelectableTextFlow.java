package org.jabref.gui.util;

import java.text.BreakIterator;

import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import org.jabref.gui.clipboard.ClipBoardManager;

import com.airhacks.afterburner.injection.Injector;
import org.jspecify.annotations.Nullable;

public class SelectableTextFlow extends TextFlow {
    /// Insertion indices into [#getTextFlowContent()]; -1 when there is no selection.
    private int selectionStart = -1;
    private int selectionEnd = -1;
    @Nullable private Path selectionPath;

    private final Pane parentPane;
    private boolean isDragging = false;
    private boolean justFinishedDrag = false;
    private final ClipBoardManager clipBoardManager;

    public SelectableTextFlow(Pane parent) {
        this.parentPane = parent;
        clipBoardManager = Injector.instantiateModelOrService(ClipBoardManager.class);
        setCursor(Cursor.TEXT);
        setFocusTraversable(true);

        addEventFilter(MouseEvent.MOUSE_PRESSED, this::onMousePressed);
        addEventFilter(MouseEvent.MOUSE_DRAGGED, this::onMouseDragged);
        addEventFilter(MouseEvent.MOUSE_RELEASED, this::onMouseReleased);
        addEventFilter(MouseEvent.MOUSE_CLICKED, this::onMouseClicked);

        focusedProperty().addListener((_, _, newFocus) -> {
            if (!newFocus) {
                clearSelection();
            }
        });
    }

    public void copySelectedText() {
        if (!isSelectionActive()) {
            return;
        }

        int startIndex = getSelectionStartIndex();
        int endIndex = getSelectionEndIndex();

        String fullText = getTextFlowContent();
        if (startIndex < 0 || endIndex > fullText.length() || startIndex >= endIndex) {
            return;
        }

        String selectedText = fullText.substring(startIndex, endIndex);
        clipBoardManager.setContent(selectedText);
    }

    public void selectAll() {
        if (getChildren().isEmpty()) {
            return;
        }
        selectionStart = hitTest(new Point2D(0, 0)).getInsertionIndex();
        selectionEnd = hitTest(new Point2D(getLayoutBounds().getWidth(), getLayoutBounds().getHeight())).getInsertionIndex();
        updateSelectionHighlight();
    }

    public void clearSelection() {
        selectionStart = -1;
        selectionEnd = -1;
        removeHighlight();
    }

    public boolean isSelectionActive() {
        return selectionStart >= 0 && selectionEnd >= 0 && selectionStart != selectionEnd;
    }

    /// Returns the start index of the selection. Assumes that the selection is active.
    public int getSelectionStartIndex() {
        assert isSelectionActive();
        return Math.min(selectionStart, selectionEnd);
    }

    /// Returns the end index of the selection. Assumes that the selection is active.
    public int getSelectionEndIndex() {
        assert isSelectionActive();
        return Math.max(selectionStart, selectionEnd);
    }

    /// The text in the index space of [TextFlow#hitTest(Point2D)] and [TextFlow#rangeShape(int, int)]:
    /// every embedded non-[Text] child (e.g. a [Hyperlink]) occupies one U+FFFC character there.
    protected String getTextFlowContent() {
        StringBuilder sb = new StringBuilder();
        for (Node node : getChildren()) {
            if (node instanceof Text text) {
                sb.append(text.getText());
            } else {
                sb.append('\uFFFC');
            }
        }
        return sb.toString();
    }

    private void updateSelectionHighlight() {
        removeHighlight();

        if (!isSelectionActive()) {
            return;
        }

        PathElement[] elements = rangeShape(getSelectionStartIndex(), getSelectionEndIndex());

        Path path = new Path();
        path.getElements().addAll(elements);
        path.setFill(Color.LIGHTBLUE.deriveColor(0, 1, 1, 0.5));
        path.setStroke(null);
        path.setCursor(Cursor.TEXT);
        path.setOnMouseClicked(_ -> removeHighlight());
        path.getTransforms().add(getLocalToParentTransform());
        path.setManaged(false);

        parentPane.getChildren().add(path);
        selectionPath = path;
    }

    private void onMousePressed(MouseEvent event) {
        if (isInHyperlink(event)) {
            clearSelection();
            return;
        }

        requestFocus();

        selectionStart = hitTest(new Point2D(event.getX(), event.getY())).getInsertionIndex();
        selectionEnd = selectionStart;
        isDragging = false;
        justFinishedDrag = false;

        removeHighlight();
    }

    private void onMouseDragged(MouseEvent event) {
        if (selectionStart < 0) {
            return;
        }
        isDragging = true;
        selectionEnd = hitTest(new Point2D(event.getX(), event.getY())).getInsertionIndex();
        updateSelectionHighlight();
    }

    private void onMouseReleased(MouseEvent event) {
        if (isDragging) {
            justFinishedDrag = true;
            isDragging = false;
        }
    }

    private void onMouseClicked(MouseEvent event) {
        // NOTE: When drag event is finished, a mouse click event at the same position
        // of the drag will be triggered.
        if (justFinishedDrag) {
            justFinishedDrag = false;
            return;
        }

        if (isInHyperlink(event)) {
            clearSelection();
            return;
        }

        if (event.getClickCount() == 2) {
            selectWordAt(hitTest(new Point2D(event.getX(), event.getY())).getCharIndex());
            return;
        }

        removeHighlight();
    }

    private void selectWordAt(int charIndex) {
        String text = getTextFlowContent();
        if (charIndex < 0 || charIndex >= text.length()) {
            return;
        }
        BreakIterator words = BreakIterator.getWordInstance();
        words.setText(text);
        selectionEnd = words.following(charIndex);
        selectionStart = words.previous();
        updateSelectionHighlight();
    }

    private boolean isInHyperlink(MouseEvent event) {
        if (!(event.getTarget() instanceof Node eventTarget)) {
            return false;
        }

        Node currentNode = eventTarget;
        while (currentNode != null && currentNode != this) {
            if (currentNode instanceof Hyperlink) {
                return true;
            }
            currentNode = currentNode.getParent();
        }

        return false;
    }

    private void removeHighlight() {
        if (selectionPath == null) {
            return;
        }
        parentPane.getChildren().remove(selectionPath);
        selectionPath = null;
    }
}
