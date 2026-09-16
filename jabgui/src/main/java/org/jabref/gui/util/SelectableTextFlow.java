package org.jabref.gui.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.text.HitInfo;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import org.jabref.gui.clipboard.ClipBoardManager;

import com.airhacks.afterburner.injection.Injector;
import org.jspecify.annotations.Nullable;

public class SelectableTextFlow extends TextFlow {
    private static final Color OCCURRENCE_COLOR = Color.GOLD.deriveColor(0, 1, 1, 0.4);
    private static final Color CURRENT_OCCURRENCE_COLOR = Color.ORANGE.deriveColor(0, 1, 1, 0.7);

    @Nullable private HitInfo startHit;
    @Nullable private HitInfo endHit;
    @Nullable private Path selectionPath;

    private final List<Path> occurrencePaths = new ArrayList<>();
    @Nullable private Path currentOccurrencePath;
    private String occurrenceQuery = "";
    private int currentOccurrence = -1;

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

        // Highlights are unmanaged snapshots of the text layout; redraw them when the text moves or rewraps.
        boundsInParentProperty().addListener(_ -> {
            if (!occurrenceQuery.isEmpty()) {
                highlightOccurrences(occurrenceQuery, currentOccurrence);
            }
        });
    }

    /// Highlights all case-insensitive occurrences of `query`; the `current`-th one is emphasized (none if out of range).
    ///
    /// @return the number of occurrences
    public int highlightOccurrences(String query, int current) {
        parentPane.getChildren().removeAll(occurrencePaths);
        occurrencePaths.clear();
        currentOccurrencePath = null;
        occurrenceQuery = query;
        currentOccurrence = current;
        if (query.isEmpty()) {
            return 0;
        }

        Matcher matcher = Pattern.compile(Pattern.quote(query), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(getTextFlowContent());
        while (matcher.find()) {
            boolean isCurrent = occurrencePaths.size() == current;
            Path path = createHighlight(matcher.start(), matcher.end(), isCurrent ? CURRENT_OCCURRENCE_COLOR : OCCURRENCE_COLOR);
            path.setMouseTransparent(true);
            if (isCurrent) {
                currentOccurrencePath = path;
            }
            occurrencePaths.add(path);
        }
        parentPane.getChildren().addAll(occurrencePaths);
        return occurrencePaths.size();
    }

    /// The highlight of the emphasized occurrence of the last [#highlightOccurrences(String, int)] call.
    public Optional<Node> getCurrentOccurrence() {
        return Optional.ofNullable(currentOccurrencePath);
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
        return startHit != null && endHit != null && startHit.getInsertionIndex() != endHit.getInsertionIndex();
    }

    /// Returns the start index of the selection. Assumes that the selection is active.
    public int getSelectionStartIndex() {
        assert isSelectionActive();
        return Math.min(startHit.getInsertionIndex(), endHit.getInsertionIndex());
    }

    /// Returns the end index of the selection. Assumes that the selection is active.
    public int getSelectionEndIndex() {
        assert isSelectionActive();
        return Math.max(startHit.getInsertionIndex(), endHit.getInsertionIndex());
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

        Path path = createHighlight(getSelectionStartIndex(), getSelectionEndIndex(), Color.LIGHTBLUE.deriveColor(0, 1, 1, 0.5));
        path.setCursor(Cursor.TEXT);
        path.setOnMouseClicked(_ -> removeHighlight());

        parentPane.getChildren().add(path);
        selectionPath = path;
    }

    private Path createHighlight(int start, int end, Color color) {
        PathElement[] elements = rangeShape(start, end);

        Path path = new Path();
        path.getElements().addAll(elements);
        path.setFill(color);
        path.setStroke(null);
        path.getTransforms().add(getLocalToParentTransform());
        path.setManaged(false);
        return path;
    }

    private void onMousePressed(MouseEvent event) {
        if (isInHyperlink(event)) {
            clearSelection();
            return;
        }

        // Consumed so that enclosing controls (e.g. the ScrollPane of the AI chat) do not grab the focus, which would clear the selection.
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

        if (isInHyperlink(event)) {
            clearSelection();
            return;
        }

        event.consume();
        removeHighlight();
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
