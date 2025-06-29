package org.jabref.gui.util;

import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.text.HitInfo;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import org.jabref.gui.ClipBoardManager;

import com.airhacks.afterburner.injection.Injector;
import org.jspecify.annotations.Nullable;

public class SelectableTextFlow extends TextFlow {
    @Nullable private HitInfo startHit;
    @Nullable private HitInfo endHit;
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
        startHit = hitTest(new Point2D(0, 0));
        endHit = hitTest(new Point2D(getLayoutBounds().getWidth(), getLayoutBounds().getHeight()));
        updateSelectionHighlight();
    }

    public void clearSelection() {
        startHit = null;
        endHit = null;
        removeHighlight();
    }

    public boolean isSelectionActive() {
        return startHit != null && endHit != null && startHit.getCharIndex() != endHit.getCharIndex();
    }

    /// Returns the start index of the selection. Assumes that the selection is active.
    public int getSelectionStartIndex() {
        assert isSelectionActive();
        return Math.min(startHit.getCharIndex(), endHit.getCharIndex());
    }

    /// Returns the end index of the selection. Assumes that the selection is active.
    public int getSelectionEndIndex() {
        assert isSelectionActive();
        return Math.max(startHit.getCharIndex() + 1, endHit.getCharIndex() + 1);
    }

    private String getTextFlowContent() {
        StringBuilder sb = new StringBuilder();
        for (Node node : getChildren()) {
            if (node instanceof Text text) {
                sb.append(text.getText());
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
        event.consume();
        requestFocus();

        startHit = hitTest(new Point2D(event.getX(), event.getY()));
        endHit = startHit;
        isDragging = false;
        justFinishedDrag = false;

        removeHighlight();
    }

    private void onMouseDragged(MouseEvent event) {
        if (startHit == null) {
            return;
        }
        event.consume();
        isDragging = true;
        endHit = hitTest(new Point2D(event.getX(), event.getY()));
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

        event.consume();
        removeHighlight();
    }

    private void removeHighlight() {
        if (selectionPath == null) {
            return;
        }
        parentPane.getChildren().remove(selectionPath);
        selectionPath = null;
    }
}
