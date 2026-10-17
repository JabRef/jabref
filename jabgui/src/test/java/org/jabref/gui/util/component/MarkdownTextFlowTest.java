package org.jabref.gui.util.component;

import java.util.List;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Separator;
import javafx.scene.input.Clipboard;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.robot.Robot;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import org.jabref.gui.StateManager;
import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.testutils.JavaFxExtension;
import org.jabref.gui.testutils.JavaFxTest;

import com.airhacks.afterburner.injection.Injector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class MarkdownTextFlowTest extends JavaFxTest {
    private StackPane rootPane;
    private RecordingClipBoardManager clipBoardManager;

    @Override
    public void start(Stage stage) {
        clipBoardManager = new RecordingClipBoardManager();
        Injector.setModelOrService(ClipBoardManager.class, clipBoardManager);

        rootPane = new StackPane();
        rootPane.setPrefSize(400, 200);
        stage.setScene(new Scene(rootPane, 400, 200));
        stage.show();
    }

    @BeforeEach
    void resetMocks() {
        clipBoardManager.clear();
    }

    private MarkdownTextFlow markdownTextFlow() {
        AtomicReference<MarkdownTextFlow> textFlowReference = new AtomicReference<>();
        interact(() -> {
            MarkdownTextFlow textFlow = new MarkdownTextFlow(rootPane);
            rootPane.getChildren().setAll(textFlow);
            textFlow.setPrefWidth(380);
            textFlowReference.set(textFlow);
        });
        return textFlowReference.get();
    }

    /// Text content with tab-separated table cells.
    private static String renderedText(MarkdownTextFlow textFlow) {
        AtomicReference<String> renderedTextReference = new AtomicReference<>();
        JavaFxExtension.invokeAndWait(() -> {
            StringBuilder builder = new StringBuilder();
            for (Node child : textFlow.getChildren()) {
                if (child instanceof Text text) {
                    builder.append(text.getText());
                } else if (child instanceof Hyperlink hyperlink) {
                    builder.append(hyperlink.getText());
                } else if (child instanceof GridPane table) {
                    TreeMap<Integer, StringJoiner> rows = new TreeMap<>();
                    for (Node cell : table.getChildren()) {
                        if (cell instanceof StackPane pane && pane.getChildren().getFirst() instanceof MarkdownTextFlow content) {
                            rows.computeIfAbsent(GridPane.getRowIndex(cell), _ -> new StringJoiner("\t")).add(renderedText(content));
                        }
                    }
                    builder.append(String.join("\n", rows.values().stream().map(StringJoiner::toString).toList()));
                }
            }
            renderedTextReference.set(builder.toString());
        });
        return renderedTextReference.get();
    }

    @Test
    void setPlainTextKeepsMarkdownMarkupLiteral() {
        MarkdownTextFlow textFlow = markdownTextFlow();

        interact(() -> textFlow.setPlainText("Hello **world**"));

        // Plain text is rendered as a single, unparsed Text node.
        assertEquals(1, childCount(textFlow));
        assertEquals("Hello **world**", renderedText(textFlow));
    }

    @Test
    void setPlainTextPreservesNewlines() {
        MarkdownTextFlow textFlow = markdownTextFlow();

        interact(() -> textFlow.setPlainText("line1\nline2"));

        assertEquals("line1\nline2", renderedText(textFlow));
    }

    @Test
    void setMarkdownStripsBoldMarkupAndStylesIt() {
        MarkdownTextFlow textFlow = markdownTextFlow();

        interact(() -> textFlow.setMarkdown("**bold**"));

        assertEquals("bold", renderedText(textFlow));
        assertTrue(hasChildWithStyleClass(textFlow, "bold"));
    }

    @Test
    void setMarkdownStripsInlineMarkupButKeepsText() {
        MarkdownTextFlow textFlow = markdownTextFlow();

        interact(() -> textFlow.setMarkdown("Hello **world**"));

        // The '**' markers are consumed by the parser; the words remain.
        assertEquals("Hello world", renderedText(textFlow));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\n"})
    void blankInputRendersNothing(String blank) {
        MarkdownTextFlow markdown = markdownTextFlow();
        MarkdownTextFlow plain = markdownTextFlow();

        interact(() -> {
            markdown.setMarkdown(blank);
            plain.setPlainText(blank);
        });

        assertTrue(hasNoChildren(markdown));
        assertTrue(hasNoChildren(plain));
    }

    @Test
    void switchingFromMarkdownToPlainTextReplacesContent() {
        MarkdownTextFlow textFlow = markdownTextFlow();

        interact(() -> {
            textFlow.setMarkdown("**bold**");
            textFlow.setPlainText("**bold**");
        });

        // Now interpreted verbatim: markup is visible and no bold styling remains.
        assertEquals(1, childCount(textFlow));
        assertEquals("**bold**", renderedText(textFlow));
        assertFalse(hasChildWithStyleClass(textFlow, "bold"));
    }

    @Test
    void switchingFromPlainTextToMarkdownReplacesContent() {
        MarkdownTextFlow textFlow = markdownTextFlow();

        interact(() -> {
            textFlow.setPlainText("**bold**");
            textFlow.setMarkdown("**bold**");
        });

        assertEquals("bold", renderedText(textFlow));
        assertTrue(hasChildWithStyleClass(textFlow, "bold"));
    }

    @Test
    void copySelectedTextFromPlainTextUsesVerbatimClipboardContent() {
        MarkdownTextFlow textFlow = markdownTextFlow();

        interact(() -> {
            textFlow.setPlainText("**bold**");
            rootPane.applyCss();
            rootPane.layout();
            textFlow.applyCss();
            textFlow.autosize();
            textFlow.layout();
        });
        dragAcrossText(textFlow);
        interact(() -> {
            assertTrue(textFlow.isSelectionActive());
            textFlow.copySelectedText();
        });

        assertEquals("**bold**", clipBoardManager.stringContent.get());
    }

    @Test
    void copySelectedTextFromMarkdownUsesMarkdownClipboardContent() {
        MarkdownTextFlow textFlow = markdownTextFlow();

        interact(() -> {
            textFlow.setMarkdown("**bold**");
            rootPane.applyCss();
            rootPane.layout();
            textFlow.applyCss();
            textFlow.autosize();
            textFlow.layout();
        });
        dragAcrossText(textFlow);
        interact(() -> {
            assertTrue(textFlow.isSelectionActive());
            textFlow.copySelectedText();
        });

        assertEquals("**bold**", clipBoardManager.stringContent.get());
        assertTrue(clipBoardManager.htmlContent.get().contains("<strong>bold</strong>"));
    }

    // [utest->req~ai.chat.markdown-tables~1]
    @Test
    void setMarkdownRendersTableWithAlignedColumns() {
        MarkdownTextFlow textFlow = markdownTextFlow();

        interact(() -> textFlow.setMarkdown("""
                | Name | Year |
                |------|------|
                | **JabRef** | 2003 |
                """));

        assertEquals("""
                Name\tYear
                JabRef\t2003""", renderedText(textFlow));
        interact(() -> {
            GridPane table = table(textFlow);
            assertEquals(List.of(0, 1, 0, 1), table.getChildren().stream().filter(StackPane.class::isInstance).map(GridPane::getColumnIndex).toList());
            assertEquals(1, table.getChildren().stream().filter(Separator.class::isInstance).count());
            assertEquals("bold", tableCell(textFlow, 0).getParent().getStyleClass().getLast());
            assertEquals("bold", tableCell(textFlow, 2).getChildren().getFirst().getStyleClass().getLast());
        });
    }

    @Test
    void setMarkdownRendersTableColumnAlignment() {
        MarkdownTextFlow textFlow = markdownTextFlow();

        interact(() -> textFlow.setMarkdown("""
                | left | center | right |
                |:-----|:------:|------:|
                | a | b | c |
                """));

        interact(() -> assertEquals(List.of(TextAlignment.LEFT, TextAlignment.CENTER, TextAlignment.RIGHT),
                List.of(tableCell(textFlow, 3).getTextAlignment(), tableCell(textFlow, 4).getTextAlignment(), tableCell(textFlow, 5).getTextAlignment())));
        interact(() -> {
            rootPane.getStylesheets().add(MarkdownTextFlow.class.getResource("/org/jabref/gui/theme/internal/jabref-base.css").toExternalForm());
            rootPane.applyCss();
            rootPane.layout();
            assertEquals(List.of(TextAlignment.LEFT, TextAlignment.CENTER, TextAlignment.RIGHT),
                    List.of(tableCell(textFlow, 3).getTextAlignment(), tableCell(textFlow, 4).getTextAlignment(), tableCell(textFlow, 5).getTextAlignment()));
        });
    }

    @ParameterizedTest
    @CsvSource(delimiter = ';', textBlock = """
            '| a | b |\n|---|---|';                  'a\tb'
            '| a |\n|---|\n| 1 | 2 |';               'a\n1\t2'
            '| e\u0301 | 🙂 |\n|---|---|\n| ab | cd |'; 'e\u0301\t🙂\nab\tcd'
            """)
    void setMarkdownRendersHeaderOnlyAndIrregularTables(String markdown, String expected) {
        MarkdownTextFlow textFlow = markdownTextFlow();

        interact(() -> textFlow.setMarkdown(markdown.translateEscapes()));

        assertEquals(expected.translateEscapes(), renderedText(textFlow));
        interact(() -> assertEquals(1, table(textFlow).getChildren().stream().filter(Separator.class::isInstance).count()));
    }

    @Test
    void setMarkdownKeepsQuotedTableNextToQuoteMarker() {
        MarkdownTextFlow textFlow = markdownTextFlow();

        interact(() -> textFlow.setMarkdown("""
                > | a | b |
                > |---|---|
                > | 1 | 2 |
                """));

        assertEquals("""
                > a\tb
                1\t2""", renderedText(textFlow));
        interact(() -> {
            rootPane.applyCss();
            rootPane.layout();
            assertEquals(tableCell(textFlow, 0).getParent().getLayoutX(), tableCell(textFlow, 2).getParent().getLayoutX());
        });
    }

    @Test
    void copySelectedTableUsesMarkdownAndHtmlTable() {
        MarkdownTextFlow textFlow = markdownTextFlow();
        String table = """
                | a | b |
                |---|---|
                | 1 | 2 |""";

        interact(() -> {
            textFlow.setMarkdown(table);
            rootPane.applyCss();
            rootPane.layout();
            textFlow.applyCss();
            textFlow.autosize();
            textFlow.layout();
            textFlow.selectAll();
            textFlow.copySelectedText();
        });

        assertEquals(table, clipBoardManager.stringContent.get());
        assertTrue(clipBoardManager.htmlContent.get().contains("<table>"));
    }

    @Test
    void setMarkdownRespectsSpanningCells() {
        MarkdownTextFlow textFlow = markdownTextFlow();

        interact(() -> {
            textFlow.setMarkdown("""
                    | A | B | C |
                    |---|---|---|
                    | 1 || 3 |
                    """);

            assertEquals(2, GridPane.getColumnSpan(tableCell(textFlow, 3).getParent()));
            assertEquals(2, GridPane.getColumnIndex(tableCell(textFlow, 4).getParent()));
            assertEquals("3", renderedText(tableCell(textFlow, 4)));
        });
    }

    @Test
    void setMarkdownPreservesTableCaptionAndCodeWhitespace() {
        MarkdownTextFlow textFlow = markdownTextFlow();

        interact(() -> textFlow.setMarkdown("""
                | Value | Next |
                |---|---|
                | `  value  ` | x |
                [Important caption]
                """));

        assertEquals("Value\tNext\n  value  \tx\nImportant caption", renderedText(textFlow));
    }

    @ParameterizedTest
    @ValueSource(strings = {"x\u0301", "中文", "🙂"})
    void tableColumnPositionsDoNotDependOnGlyphWidth(String value) {
        MarkdownTextFlow textFlow = markdownTextFlow();

        interact(() -> {
            textFlow.setMarkdown("""
                    | Name | Year |
                    |---|---|
                    | %s | 2026 |
                    | ab | 2025 |
                    """.formatted(value));
            rootPane.applyCss();
            rootPane.layout();

            assertEquals(tableCell(textFlow, 3).getParent().getLayoutX(), tableCell(textFlow, 5).getParent().getLayoutX());
            assertEquals(tableCell(textFlow, 3).getParent().getBoundsInParent().getWidth(), tableCell(textFlow, 5).getParent().getBoundsInParent().getWidth());
        });
    }

    @Test
    void wideTableWrapsWithinCellsWithoutMovingColumns() {
        MarkdownTextFlow textFlow = markdownTextFlow();

        interact(() -> {
            textFlow.setMarkdown("""
                    | Title | Year | Description |
                    |---|---|---|
                    | A long title consisting of several words | 2026 | An extended explanation that makes this table wider than the chat bubble |
                    """);
            rootPane.applyCss();
            rootPane.layout();
            GridPane table = table(textFlow);

            assertEquals(textFlow.getWidth(), table.getWidth(), 1);
            for (int column = 0; column < 3; column++) {
                assertEquals(tableCell(textFlow, column).getParent().getLayoutX(), tableCell(textFlow, column + 3).getParent().getLayoutX());
            }
            assertTrue(tableCell(textFlow, 5).getHeight() > tableCell(textFlow, 2).getHeight());

            rootPane.resize(200, 400);
            rootPane.layout();
            assertEquals(textFlow.getWidth(), table.getWidth(), 1);
            assertEquals(tableCell(textFlow, 2).getParent().getLayoutX(), tableCell(textFlow, 5).getParent().getLayoutX());
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"- ", "1. ", "> "})
    void copyNestedTableKeepsParagraphsAndContainerPrefixes(String prefix) {
        MarkdownTextFlow textFlow = markdownTextFlow();
        String continuation = "> ".equals(prefix) ? prefix : " ".repeat(prefix.length());
        String separator = "> ".equals(prefix) ? "\n>\n" : "\n\n";
        String markdown = prefix + "Intro" + separator + continuation + "| A | B |\n" + continuation + "|---|---|\n" + continuation + "| 1 | 2 |" + separator + continuation + "After";

        interact(() -> {
            textFlow.setMarkdown(markdown);
            rootPane.applyCss();
            rootPane.layout();
            textFlow.selectAll();
            textFlow.copySelectedText();
        });

        assertEquals(markdown, clipBoardManager.stringContent.get());
        assertTrue(clipBoardManager.htmlContent.get().contains("<p>After</p>"));
    }

    @Test
    void copyQuotedTableRetainsEveryQuotePrefix() {
        MarkdownTextFlow textFlow = markdownTextFlow();
        String markdown = """
                > | A | B |
                > |---|---|
                > | 1 | 2 |""";

        interact(() -> {
            textFlow.setMarkdown(markdown);
            rootPane.applyCss();
            rootPane.layout();
            textFlow.selectAll();
            textFlow.copySelectedText();
        });

        assertEquals(markdown, clipBoardManager.stringContent.get());
        assertEquals("""
                <blockquote>
                <table>
                <thead>
                <tr><th>A</th><th>B</th></tr>
                </thead>
                <tbody>
                <tr><td>1</td><td>2</td></tr>
                </tbody>
                </table>
                </blockquote>
                """, clipBoardManager.htmlContent.get());
    }

    @Test
    void copySelectionAcrossTableKeepsSurroundingMarkup() {
        MarkdownTextFlow textFlow = markdownTextFlow();
        String markdown = """
                **Before**

                | A | B |
                |---|---|
                | 1 | 2 |

                *After*""";

        interact(() -> {
            textFlow.setMarkdown(markdown);
            rootPane.applyCss();
            rootPane.layout();
            textFlow.selectAll();
            textFlow.copySelectedText();
        });

        assertEquals(markdown, clipBoardManager.stringContent.get());
    }

    @Test
    void mouseClickOnTableHyperlinkInvokesCustomHandler() {
        MarkdownTextFlow textFlow = markdownTextFlow();
        AtomicReference<String> clickedUrl = new AtomicReference<>();

        interact(() -> {
            textFlow.setMarkdown("""
                    | Reference | Year |
                    |---|---|
                    | [JabRef](https://www.jabref.org) | 2026 |
                    """);
            textFlow.setHyperlinkHandler(clickedUrl::set);
            rootPane.applyCss();
            rootPane.layout();
        });
        JavaFxExtension.invokeAndWait(() -> {
            Hyperlink hyperlink = (Hyperlink) tableCell(textFlow, 2).getChildren().getFirst();
            Bounds bounds = hyperlink.localToScreen(hyperlink.getBoundsInLocal());
            Robot robot = new Robot();
            robot.mouseMove(bounds.getCenterX(), bounds.getCenterY());
            robot.mouseClick(MouseButton.PRIMARY);
        });
        awaitEvents();

        assertEquals("https://www.jabref.org", clickedUrl.get());
    }

    @Test
    void copySelectedTableCellPreservesCodeWhitespace() {
        MarkdownTextFlow textFlow = markdownTextFlow();

        interact(() -> {
            textFlow.setMarkdown("""
                    | Value | Next |
                    |---|---|
                    | `  value  ` | x |
                    """);
            rootPane.applyCss();
            rootPane.layout();
            tableCell(textFlow, 2).selectAll();
            textFlow.copySelectedText();
        });

        assertEquals("`  value  `", clipBoardManager.stringContent.get());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 20})
    void doubleClickSelectsWordWithPadding(int padding) {
        MarkdownTextFlow textFlow = markdownTextFlow();

        interact(() -> {
            textFlow.setPlainText("hello world");
            textFlow.setPadding(new Insets(padding));
            rootPane.applyCss();
            rootPane.layout();
            textFlow.applyCss();
            textFlow.autosize();
            textFlow.layout();
        });
        JavaFxExtension.invokeAndWait(() -> {
            Bounds bounds = firstTextBounds(textFlow);
            Robot robot = new Robot();
            robot.mouseMove(bounds.getMinX() + 3, bounds.getMinY() + (bounds.getHeight() / 2));
            robot.mouseClick(MouseButton.PRIMARY);
            robot.mouseClick(MouseButton.PRIMARY);
        });
        interact(() -> {
            assertTrue(textFlow.isSelectionActive());
            textFlow.copySelectedText();
        });

        assertEquals("hello", clipBoardManager.stringContent.get());
    }

    @Test
    void doubleClickSelectsWordInsideTableCell() {
        MarkdownTextFlow textFlow = markdownTextFlow();

        interact(() -> {
            textFlow.setMarkdown("""
                    | Value | Next |
                    |---|---|
                    | hello world | x |
                    """);
            rootPane.applyCss();
            rootPane.layout();
        });
        JavaFxExtension.invokeAndWait(() -> {
            Bounds bounds = firstTextBounds(tableCell(textFlow, 2));
            Robot robot = new Robot();
            robot.mouseMove(bounds.getMinX() + 3, bounds.getCenterY());
            robot.mouseClick(MouseButton.PRIMARY);
            robot.mouseClick(MouseButton.PRIMARY);
        });
        interact(() -> {
            assertTrue(tableCell(textFlow, 2).isSelectionActive());
            textFlow.copySelectedText();
        });

        assertEquals("hello", clipBoardManager.stringContent.get());
    }

    @Test
    void doubleClickAfterHyperlinkSelectsClickedWord() {
        MarkdownTextFlow textFlow = markdownTextFlow();

        interact(() -> {
            textFlow.setMarkdown("[link](https://example.com) hello world");
            rootPane.applyCss();
            rootPane.layout();
            textFlow.applyCss();
            textFlow.autosize();
            textFlow.layout();
        });
        JavaFxExtension.invokeAndWait(() -> {
            Node lastText = textFlow.getChildren().getLast();
            Bounds bounds = lastText.localToScreen(lastText.getBoundsInLocal());
            Robot robot = new Robot();
            robot.mouseMove(bounds.getMaxX() - 3, bounds.getMinY() + (bounds.getHeight() / 2));
            robot.mouseClick(MouseButton.PRIMARY);
            robot.mouseClick(MouseButton.PRIMARY);
        });
        interact(() -> {
            assertTrue(textFlow.isSelectionActive());
            textFlow.copySelectedText();
        });

        assertEquals("world", clipBoardManager.stringContent.get());
    }

    @Test
    void hyperlinkHandlerDefaultsToNonNull() {
        MarkdownTextFlow textFlow = markdownTextFlow();

        assertNotNull(textFlow.getHyperlinkHandler());
    }

    @Test
    void clickingHyperlinkInvokesCustomHandler() {
        MarkdownTextFlow textFlow = markdownTextFlow();
        AtomicReference<String> clickedUrl = new AtomicReference<>();

        interact(() -> {
            textFlow.setHyperlinkHandler(clickedUrl::set);
            textFlow.setMarkdown("[link](https://example.com)");
        });

        interact(() -> {
            Hyperlink hyperlink = (Hyperlink) textFlow.getChildren().getFirst();
            hyperlink.fire();
        });

        assertEquals("https://example.com", clickedUrl.get());
    }

    @Test
    void mouseClickOnHyperlinkInvokesCustomHandler() {
        MarkdownTextFlow textFlow = markdownTextFlow();
        AtomicReference<String> clickedUrl = new AtomicReference<>();

        interact(() -> {
            textFlow.setHyperlinkHandler(clickedUrl::set);
            textFlow.setMarkdown("[link](https://example.com)");
            rootPane.applyCss();
            rootPane.layout();
        });
        awaitEvents();

        JavaFxExtension.invokeAndWait(() -> {
            Hyperlink hyperlink = (Hyperlink) textFlow.getChildren().getFirst();
            Bounds bounds = hyperlink.localToScreen(hyperlink.getBoundsInLocal());
            Robot robot = new Robot();
            robot.mouseMove(bounds.getCenterX(), bounds.getCenterY());
            robot.mouseClick(MouseButton.PRIMARY);
        });
        awaitEvents();

        assertEquals("https://example.com", clickedUrl.get());
    }

    private static int childCount(MarkdownTextFlow textFlow) {
        AtomicReference<Integer> childCountReference = new AtomicReference<>();
        JavaFxExtension.invokeAndWait(() -> childCountReference.set(textFlow.getChildren().size()));
        return childCountReference.get();
    }

    private static GridPane table(MarkdownTextFlow flow) {
        return (GridPane) flow.getChildren().stream().filter(GridPane.class::isInstance).findFirst().orElseThrow();
    }

    private static MarkdownTextFlow tableCell(MarkdownTextFlow flow, int index) {
        StackPane pane = (StackPane) table(flow).getChildren().stream().filter(StackPane.class::isInstance).toList().get(index);
        return (MarkdownTextFlow) pane.getChildren().getFirst();
    }

    private static boolean hasNoChildren(MarkdownTextFlow textFlow) {
        AtomicReference<Boolean> hasNoChildrenReference = new AtomicReference<>();
        JavaFxExtension.invokeAndWait(() -> hasNoChildrenReference.set(textFlow.getChildren().isEmpty()));
        return hasNoChildrenReference.get();
    }

    private static boolean hasChildWithStyleClass(MarkdownTextFlow textFlow, String styleClass) {
        AtomicReference<Boolean> hasChildWithStyleClassReference = new AtomicReference<>();
        JavaFxExtension.invokeAndWait(() -> hasChildWithStyleClassReference.set(textFlow.getChildren().stream().anyMatch(child -> child.getStyleClass().contains(styleClass))));
        return hasChildWithStyleClassReference.get();
    }

    private static void dragAcrossText(MarkdownTextFlow textFlow) {
        JavaFxExtension.invokeAndWait(() -> {
            Bounds bounds = firstTextBounds(textFlow);
            double centerY = bounds.getMinY() + (bounds.getHeight() / 2);
            Robot robot = new Robot();
            robot.mouseMove(bounds.getMinX() + 1, centerY);
            robot.mousePress(MouseButton.PRIMARY);
            robot.mouseMove(bounds.getMaxX() - 1, centerY);
            robot.mouseRelease(MouseButton.PRIMARY);
        });
    }

    /// The bounds of the first rendered line in screen coordinates.
    private static Bounds firstTextBounds(MarkdownTextFlow textFlow) {
        AtomicReference<Bounds> boundsReference = new AtomicReference<>();
        JavaFxExtension.invokeAndWait(() -> {
            for (Node child : textFlow.getChildren()) {
                Bounds childBounds = child.localToScreen(child.getBoundsInLocal());
                if (childBounds != null && childBounds.getWidth() > 2) {
                    boundsReference.set(childBounds);
                    return;
                }
            }
        });
        return boundsReference.get() == null ? new BoundingBox(0, 0, 0, 0) : boundsReference.get();
    }

    private static class RecordingClipBoardManager extends ClipBoardManager {
        private final AtomicReference<String> stringContent = new AtomicReference<>("");
        private final AtomicReference<String> htmlContent = new AtomicReference<>("");

        RecordingClipBoardManager() {
            super(mock(StateManager.class), mock(Clipboard.class), mock(java.awt.datatransfer.Clipboard.class));
        }

        @Override
        public void setContent(String string) {
            stringContent.set(string);
        }

        @Override
        public void setHtmlContent(String html, String fallbackPlain) {
            htmlContent.set(html);
            stringContent.set(fallbackPlain);
        }

        void clear() {
            stringContent.set("");
            htmlContent.set("");
        }
    }
}
