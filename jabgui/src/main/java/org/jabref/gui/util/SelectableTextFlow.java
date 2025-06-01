package org.jabref.gui.util;

import java.util.Optional;

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

/**
 * A TextFlow that allows text selection and copying.
 */
public class SelectableTextFlow extends TextFlow {
    protected Optional<HitInfo> startHit = Optional.empty();
    protected Optional<HitInfo> endHit = Optional.empty();
    protected Optional<Path> selectionPath = Optional.empty();

    private final Pane parentPane;

    /**
     * Creates a SelectableTextFlow instance.
     *
     * @param parent The parent Pane to which this TextFlow will be added.
     */
    public SelectableTextFlow(Pane parent) {
        this.parentPane = parent;
        setCursor(Cursor.TEXT);
        setFocusTraversable(true);

        setOnMouseClicked(_ -> {
            requestFocus();
            deselect();
        });

        setOnMousePressed(event -> {
            requestFocus();
            handleMousePressed(event);
        });

        setOnMouseDragged(event -> {
            requestFocus();
            handleMouseDragged(event);
        });

        focusedProperty().addListener((_, _, newFocus) -> {
            if (!newFocus) {
                deselect();
            }
        });
    }

    public void copySelectedText() {
        if (startHit.isEmpty() || endHit.isEmpty()) {
            return;
        }

        int startIndex = Math.min(startHit.get().getCharIndex(), endHit.get().getCharIndex());
        int endIndex = Math.max(startHit.get().getCharIndex() + 1, endHit.get().getCharIndex() + 1);

        String fullText = getTextFlowContent();
        if (startIndex >= 0 && endIndex <= fullText.length() && startIndex < endIndex) {
            String selectedText = fullText.substring(startIndex, endIndex);
            ClipBoardManager clipBoardManager = Injector.instantiateModelOrService(ClipBoardManager.class);
            clipBoardManager.setContent(selectedText);
        }
    }

    public void selectAll() {
        if (getChildren().isEmpty()) {
            return;
        }
        startHit = Optional.of(hitTest(new Point2D(0, 0)));
        endHit = Optional.of(hitTest(new Point2D(getLayoutBounds().getWidth(), getLayoutBounds().getHeight())));
        updateSelectionHighlight();
    }

    public void clearSelection() {
        getChildren().clear();
        startHit = Optional.empty();
        endHit = Optional.empty();
        deselect();
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
        selectionPath.ifPresent(parentPane.getChildren()::remove);

        if (startHit.isEmpty() || endHit.isEmpty() ||
                startHit.get().getCharIndex() == endHit.get().getCharIndex()) {
            return;
        }

        int startIndex = Math.min(startHit.get().getCharIndex(), endHit.get().getCharIndex());
        int endIndex = Math.max(startHit.get().getCharIndex() + 1, endHit.get().getCharIndex() + 1);

        PathElement[] elements = rangeShape(startIndex, endIndex);

        Path path = new Path();
        path.getElements().addAll(elements);
        path.setFill(Color.LIGHTBLUE.deriveColor(0, 1, 1, 0.5));
        path.setStroke(null);
        path.setCursor(Cursor.TEXT);
        path.setOnMouseClicked(_ -> deselect());
        path.getTransforms().add(getLocalToParentTransform());
        path.setManaged(false);

        parentPane.getChildren().add(path);
        selectionPath = Optional.of(path);
    }

    private void handleMousePressed(MouseEvent event) {
        deselect();
        startHit = Optional.of(hitTest(new Point2D(event.getX(), event.getY())));
        endHit = startHit;
    }

    private void handleMouseDragged(MouseEvent event) {
        startHit.ifPresent((_) -> {
            endHit = Optional.of(hitTest(new Point2D(event.getX(), event.getY())));
            updateSelectionHighlight();
        });
    }

    private void deselect() {
        selectionPath.ifPresent(parentPane.getChildren()::remove);
        selectionPath = Optional.empty();
    }
}
