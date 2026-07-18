package org.jabref.gui.util.component;

import java.util.concurrent.atomic.AtomicReference;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.Clipboard;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import org.jabref.gui.StateManager;
import org.jabref.gui.clipboard.ClipBoardManager;

import com.airhacks.afterburner.injection.Injector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@ExtendWith(ApplicationExtension.class)
class MarkdownTextFlowTest {
    private StackPane rootPane;
    private RecordingClipBoardManager clipBoardManager;

    @Start
    void start(Stage stage) {
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

    private MarkdownTextFlow markdownTextFlow(FxRobot robot) {
        AtomicReference<MarkdownTextFlow> textFlowReference = new AtomicReference<>();
        robot.interact(() -> {
            MarkdownTextFlow textFlow = new MarkdownTextFlow(rootPane);
            rootPane.getChildren().setAll(textFlow);
            textFlow.setPrefWidth(380);
            textFlowReference.set(textFlow);
        });
        return textFlowReference.get();
    }

    /// Concatenation of the text of every {@link Text} child, i.e. what the user actually sees.
    private static String renderedText(FxRobot robot, MarkdownTextFlow textFlow) {
        AtomicReference<String> renderedTextReference = new AtomicReference<>();
        robot.interact(() -> {
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
    void setPlainTextKeepsMarkdownMarkupLiteral(FxRobot robot) {
        MarkdownTextFlow textFlow = markdownTextFlow(robot);

        robot.interact(() -> textFlow.setPlainText("Hello **world**"));

        // Plain text is rendered as a single, unparsed Text node.
        assertEquals(1, childCount(robot, textFlow));
        assertEquals("Hello **world**", renderedText(robot, textFlow));
    }

    @Test
    void setPlainTextPreservesNewlines(FxRobot robot) {
        MarkdownTextFlow textFlow = markdownTextFlow(robot);

        robot.interact(() -> textFlow.setPlainText("line1\nline2"));

        assertEquals("line1\nline2", renderedText(robot, textFlow));
    }

    @Test
    void setMarkdownStripsBoldMarkupAndStylesIt(FxRobot robot) {
        MarkdownTextFlow textFlow = markdownTextFlow(robot);

        robot.interact(() -> textFlow.setMarkdown("**bold**"));

        assertEquals("bold", renderedText(robot, textFlow));
        assertTrue(hasChildWithStyleClass(robot, textFlow, "markdown-bold"));
    }

    @Test
    void setMarkdownStripsInlineMarkupButKeepsText(FxRobot robot) {
        MarkdownTextFlow textFlow = markdownTextFlow(robot);

        robot.interact(() -> textFlow.setMarkdown("Hello **world**"));

        // The '**' markers are consumed by the parser; the words remain.
        assertEquals("Hello world", renderedText(robot, textFlow));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\n"})
    void blankInputRendersNothing(String blank, FxRobot robot) {
        MarkdownTextFlow markdown = markdownTextFlow(robot);
        MarkdownTextFlow plain = markdownTextFlow(robot);

        robot.interact(() -> {
            markdown.setMarkdown(blank);
            plain.setPlainText(blank);
        });

        assertTrue(hasNoChildren(robot, markdown));
        assertTrue(hasNoChildren(robot, plain));
    }

    @Test
    void switchingFromMarkdownToPlainTextReplacesContent(FxRobot robot) {
        MarkdownTextFlow textFlow = markdownTextFlow(robot);

        robot.interact(() -> {
            textFlow.setMarkdown("**bold**");
            textFlow.setPlainText("**bold**");
        });

        // Now interpreted verbatim: markup is visible and no bold styling remains.
        assertEquals(1, childCount(robot, textFlow));
        assertEquals("**bold**", renderedText(robot, textFlow));
        assertFalse(hasChildWithStyleClass(robot, textFlow, "markdown-bold"));
    }

    @Test
    void switchingFromPlainTextToMarkdownReplacesContent(FxRobot robot) {
        MarkdownTextFlow textFlow = markdownTextFlow(robot);

        robot.interact(() -> {
            textFlow.setPlainText("**bold**");
            textFlow.setMarkdown("**bold**");
        });

        assertEquals("bold", renderedText(robot, textFlow));
        assertTrue(hasChildWithStyleClass(robot, textFlow, "markdown-bold"));
    }

    @Test
    void copySelectedTextFromPlainTextUsesVerbatimClipboardContent(FxRobot robot) {
        MarkdownTextFlow textFlow = markdownTextFlow(robot);

        robot.interact(() -> {
            textFlow.setPlainText("**bold**");
            rootPane.applyCss();
            rootPane.layout();
            textFlow.applyCss();
            textFlow.autosize();
            textFlow.layout();
        });
        dragAcrossText(robot, textFlow);
        robot.interact(() -> {
            assertTrue(textFlow.isSelectionActive());
            textFlow.copySelectedText();
        });

        assertEquals("**bold**", clipBoardManager.stringContent.get());
    }

    @Test
    void copySelectedTextFromMarkdownUsesMarkdownClipboardContent(FxRobot robot) {
        MarkdownTextFlow textFlow = markdownTextFlow(robot);

        robot.interact(() -> {
            textFlow.setMarkdown("**bold**");
            rootPane.applyCss();
            rootPane.layout();
            textFlow.applyCss();
            textFlow.autosize();
            textFlow.layout();
        });
        dragAcrossText(robot, textFlow);
        robot.interact(() -> {
            assertTrue(textFlow.isSelectionActive());
            textFlow.copySelectedText();
        });

        assertEquals("**bold**", clipBoardManager.stringContent.get());
        assertTrue(clipBoardManager.htmlContent.get().contains("<strong>bold</strong>"));
    }

    private static int childCount(FxRobot robot, MarkdownTextFlow textFlow) {
        AtomicReference<Integer> childCountReference = new AtomicReference<>();
        robot.interact(() -> childCountReference.set(textFlow.getChildren().size()));
        return childCountReference.get();
    }

    private static boolean hasNoChildren(FxRobot robot, MarkdownTextFlow textFlow) {
        AtomicReference<Boolean> hasNoChildrenReference = new AtomicReference<>();
        robot.interact(() -> hasNoChildrenReference.set(textFlow.getChildren().isEmpty()));
        return hasNoChildrenReference.get();
    }

    private static boolean hasChildWithStyleClass(FxRobot robot, MarkdownTextFlow textFlow, String styleClass) {
        AtomicReference<Boolean> hasChildWithStyleClassReference = new AtomicReference<>();
        robot.interact(() -> hasChildWithStyleClassReference.set(textFlow.getChildren().stream().anyMatch(child -> child.getStyleClass().contains(styleClass))));
        return hasChildWithStyleClassReference.get();
    }

    private static void dragAcrossText(FxRobot robot, MarkdownTextFlow textFlow) {
        Bounds bounds = firstTextBounds(robot, textFlow);
        double centerY = bounds.getMinY() + (bounds.getHeight() / 2);

        robot.moveTo(bounds.getMinX() + 1, centerY)
             .press(MouseButton.PRIMARY)
             .moveTo(bounds.getMaxX() - 1, centerY)
             .release(MouseButton.PRIMARY);
    }

    private static Bounds firstTextBounds(FxRobot robot, MarkdownTextFlow textFlow) {
        AtomicReference<Bounds> boundsReference = new AtomicReference<>();
        robot.interact(() -> {
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
