package org.jabref.gui.util.component;

import java.util.concurrent.atomic.AtomicReference;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.Clipboard;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.scene.robot.Robot;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import org.jabref.gui.StateManager;
import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.testutils.JavaFxExtension;
import org.jabref.gui.testutils.JavaFxTest;

import com.airhacks.afterburner.injection.Injector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    /// Concatenation of the text of every [Text] child, i.e. what the user actually sees.
    private static String renderedText(MarkdownTextFlow textFlow) {
        AtomicReference<String> renderedTextReference = new AtomicReference<>();
        JavaFxExtension.invokeAndWait(() -> {
            StringBuilder builder = new StringBuilder();
            for (Node child : textFlow.getChildren()) {
                if (child instanceof Text text) {
                    builder.append(text.getText());
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
        assertTrue(hasChildWithStyleClass(textFlow, "markdown-bold"));
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
        assertFalse(hasChildWithStyleClass(textFlow, "markdown-bold"));
    }

    @Test
    void switchingFromPlainTextToMarkdownReplacesContent() {
        MarkdownTextFlow textFlow = markdownTextFlow();

        interact(() -> {
            textFlow.setPlainText("**bold**");
            textFlow.setMarkdown("**bold**");
        });

        assertEquals("bold", renderedText(textFlow));
        assertTrue(hasChildWithStyleClass(textFlow, "markdown-bold"));
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

    private static int childCount(MarkdownTextFlow textFlow) {
        AtomicReference<Integer> childCountReference = new AtomicReference<>();
        JavaFxExtension.invokeAndWait(() -> childCountReference.set(textFlow.getChildren().size()));
        return childCountReference.get();
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
