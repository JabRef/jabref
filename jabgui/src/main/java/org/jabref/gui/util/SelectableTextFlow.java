package org.jabref.gui.util;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Hyperlink;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import org.jabref.gui.clipboard.ClipBoardManager;

import com.airhacks.afterburner.injection.Injector;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class SelectableTextFlow extends TextFlow {
    private static final Pattern EMPTY_QUERY_PATTERN = Pattern.compile("");

    /// Unicode "OBJECT REPLACEMENT CHARACTER". JavaFX's [TextFlow] puts it in place of every child that is not a [Text]
    /// (e.g. a [Hyperlink]), so such a child counts as exactly one character in [TextFlow#getHitInfo(Point2D)] indices.
    private static final char OBJECT_REPLACEMENT_CHARACTER = '\uFFFC';

    /// Insertion indices into [#getTextFlowContent()]; -1 when there is no selection.
    private int selectionStart = -1;
    private int selectionEnd = -1;
    @Nullable private Path selectionPath;

    private final List<Path> occurrencePaths = new ArrayList<>();
    @Nullable private Path currentOccurrencePath;
    private String occurrenceQuery = "";
    private Pattern occurrencePattern = EMPTY_QUERY_PATTERN;
    private int currentOccurrence = -1;
    private boolean occurrenceRefreshPending;

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
            if (!occurrenceQuery.isEmpty() && !occurrenceRefreshPending) {
                occurrenceRefreshPending = true;
                // Computing range geometry can invalidate bounds inside a refresh; finish that refresh before redrawing.
                Platform.runLater(() -> {
                    try {
                        highlightOccurrences(occurrenceQuery, currentOccurrence);
                    } finally {
                        occurrenceRefreshPending = false;
                    }
                });
            }
        });
    }

    /// Highlights all case-insensitive occurrences of `query`; the `current`-th one is emphasized (none if out of range).
    ///
    /// Search text and JavaFX layout use different indices: a hyperlink label contributes all its characters to the
    /// search, but its node occupies only one layout position. We concatenate adjacent text and link labels so a match
    /// can span formatting boundaries, recording the layout position of each searchable character in `layoutIndices`.
    /// At other nodes, we search the accumulated text before descending into embedded flows (such as table cells),
    /// preserving reading order. Each match is mapped back to a layout range and drawn as a path in the parent pane,
    /// behind the text. The bounds listener redraws these paths when the text moves or wraps.
    ///
    /// @return the number of occurrences
    public int highlightOccurrences(String query, int current) {
        clearOccurrenceHighlights();
        if (!query.equals(occurrenceQuery)) {
            occurrencePattern = Pattern.compile(Pattern.quote(query), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        }
        occurrenceQuery = query;
        currentOccurrence = current;
        if (query.isEmpty()) {
            getChildren().stream()
                         .flatMap(SelectableTextFlow::embeddedTextFlows)
                         .forEach(flow -> flow.highlightOccurrences("", -1));
            return 0;
        }

        int total = highlightChildren(query, current);
        parentPane.getChildren().addAll(occurrencePaths);
        return total;
    }

    private void clearOccurrenceHighlights() {
        parentPane.getChildren().removeAll(occurrencePaths);
        occurrencePaths.clear();
        currentOccurrencePath = null;
    }

    private int highlightChildren(String query, int current) {
        StringBuilder content = new StringBuilder();
        List<Integer> layoutIndices = new ArrayList<>();
        int layoutIndex = 0;
        int total = 0;
        for (Node child : getChildren()) {
            if (child instanceof Text || child instanceof Hyperlink) {
                layoutIndex += appendSearchableText(child, content, layoutIndices, layoutIndex);
            } else {
                total += highlightText(content, layoutIndices, current - total);
                content.setLength(0);
                layoutIndices.clear();
                total += highlightEmbeddedFlows(child, query, current - total);
                layoutIndex++;
            }
        }
        return total + highlightText(content, layoutIndices, current - total);
    }

    /// Appends searchable characters and returns the number of layout positions occupied by the node.
    private static int appendSearchableText(Node child, StringBuilder content, List<Integer> layoutIndices, int layoutIndex) {
        String text = child instanceof Text textNode ? textNode.getText() : ((Hyperlink) child).getText();
        content.append(text);
        for (int index = 0; index < text.length(); index++) {
            layoutIndices.add(layoutIndex + (child instanceof Text ? index : 0));
        }
        return child instanceof Text ? text.length() : 1;
    }

    private static int highlightEmbeddedFlows(Node node, String query, int current) {
        int total = 0;
        for (SelectableTextFlow embedded : embeddedTextFlows(node).toList()) {
            total += embedded.highlightOccurrences(query, current - total);
        }
        return total;
    }

    private int highlightText(StringBuilder content, List<Integer> layoutIndices, int current) {
        int count = 0;
        Matcher matcher = occurrencePattern.matcher(content);
        while (matcher.find()) {
            addOccurrenceHighlight(layoutIndices.get(matcher.start()), layoutIndices.get(matcher.end() - 1) + 1, count == current);
            count++;
        }
        return count;
    }

    private void addOccurrenceHighlight(int start, int end, boolean isCurrent) {
        Path path = createHighlight(start, end);
        // [impl->req~ai.chat.find.highlight-theme~1]
        path.getStyleClass().add("text-search-highlight");
        // [impl->req~ai.chat.find.highlight-layer~1]
        path.setViewOrder(2);
        path.setMouseTransparent(true);
        if (isCurrent) {
            // [impl->req~ai.chat.find.current-outline~1]
            path.getStyleClass().add("current");
            currentOccurrencePath = path;
        }
        occurrencePaths.add(path);
    }

    private static Stream<SelectableTextFlow> embeddedTextFlows(Node node) {
        if (node instanceof SelectableTextFlow flow) {
            return Stream.of(flow);
        }
        if (node instanceof Parent parent) {
            return parent.getChildrenUnmodifiable().stream().flatMap(SelectableTextFlow::embeddedTextFlows);
        }
        return Stream.empty();
    }

    /// The highlight of the emphasized occurrence of the last [#highlightOccurrences(String, int)] call.
    public Optional<Node> getCurrentOccurrence() {
        return Optional.<Node>ofNullable(currentOccurrencePath)
                       .or(() -> getChildren().stream()
                                              .flatMap(SelectableTextFlow::embeddedTextFlows)
                                              .flatMap(flow -> flow.getCurrentOccurrence().stream())
                                              .findFirst());
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
        selectionStart = 0;
        selectionEnd = getTextFlowContent().length();
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

    /// The text in the index space of [TextFlow#getHitInfo(Point2D)] and [TextFlow#getRangeShape(int, int, boolean)]:
    /// every embedded non-[Text] child occupies one [#OBJECT_REPLACEMENT_CHARACTER] there.
    protected String getTextFlowContent() {
        StringBuilder sb = new StringBuilder();
        for (Node node : getChildren()) {
            if (node instanceof Text text) {
                sb.append(text.getText());
            } else {
                sb.append(OBJECT_REPLACEMENT_CHARACTER);
            }
        }
        return sb.toString();
    }

    private void updateSelectionHighlight() {
        removeHighlight();

        if (!isSelectionActive()) {
            return;
        }

        Path path = createHighlight(getSelectionStartIndex(), getSelectionEndIndex());
        path.getStyleClass().add("text-selection-highlight");
        path.setViewOrder(1);
        path.setCursor(Cursor.TEXT);
        path.setOnMouseClicked(_ -> removeHighlight());

        parentPane.getChildren().add(path);
        selectionPath = path;
    }

    private Path createHighlight(int start, int end) {
        PathElement[] elements = getRangeShape(start, end, false);

        Path path = new Path();
        path.getElements().addAll(elements);
        path.getTransforms().add(getLocalToParentTransform());
        path.setManaged(false);
        return path;
    }

    private void onMousePressed(MouseEvent event) {
        if (isInInteractiveContent(event)) {
            clearSelection();
            return;
        }

        // Consumed so that enclosing controls (e.g. the ScrollPane of the AI chat) do not grab the focus, which would clear the selection.
        event.consume();
        requestFocus();

        selectionStart = getHitInfo(new Point2D(event.getX(), event.getY())).getInsertionIndex();
        selectionEnd = selectionStart;
        isDragging = false;
        justFinishedDrag = false;

        removeHighlight();
    }

    private void onMouseDragged(MouseEvent event) {
        if (selectionStart < 0) {
            return;
        }
        event.consume();
        isDragging = true;
        selectionEnd = getHitInfo(new Point2D(event.getX(), event.getY())).getInsertionIndex();
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

        if (isInInteractiveContent(event)) {
            clearSelection();
            return;
        }

        event.consume();
        if (event.getClickCount() == 2) {
            selectWordAt(getHitInfo(new Point2D(event.getX(), event.getY())).getCharIndex());
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

    private boolean isInInteractiveContent(MouseEvent event) {
        if (!(event.getTarget() instanceof Node eventTarget)) {
            return false;
        }

        Node currentNode = eventTarget;
        while (currentNode != null && currentNode != this) {
            if (currentNode instanceof Hyperlink || currentNode instanceof SelectableTextFlow) {
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
