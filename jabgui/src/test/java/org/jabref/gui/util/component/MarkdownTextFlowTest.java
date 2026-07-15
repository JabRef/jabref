package org.jabref.gui.util.component;

import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;

import org.jabref.gui.clipboard.ClipBoardManager;

import com.airhacks.afterburner.injection.Injector;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@ExtendWith(ApplicationExtension.class)
class MarkdownTextFlowTest {

    @BeforeAll
    static void registerClipBoardManager() {
        // SelectableTextFlow's constructor resolves a ClipBoardManager via the Injector.
        Injector.setModelOrService(ClipBoardManager.class, mock(ClipBoardManager.class));
    }

    private MarkdownTextFlow markdownTextFlow() {
        return new MarkdownTextFlow(new Pane());
    }

    /// Concatenation of the text of every {@link Text} child, i.e. what the user actually sees.
    private static String renderedText(MarkdownTextFlow textFlow) {
        StringBuilder builder = new StringBuilder();
        for (Node child : textFlow.getChildren()) {
            if (child instanceof Text text) {
                builder.append(text.getText());
            }
        }
        return builder.toString();
    }

    @Test
    void setPlainTextKeepsMarkdownMarkupLiteral() {
        MarkdownTextFlow textFlow = markdownTextFlow();

        textFlow.setPlainText("Hello **world**");

        // Plain text is rendered as a single, unparsed Text node.
        assertEquals(1, textFlow.getChildren().size());
        assertEquals("Hello **world**", renderedText(textFlow));
    }

    @Test
    void setPlainTextPreservesNewlines() {
        MarkdownTextFlow textFlow = markdownTextFlow();

        textFlow.setPlainText("line1\nline2");

        assertEquals("line1\nline2", renderedText(textFlow));
    }

    @Test
    void setMarkdownStripsBoldMarkupAndStylesIt() {
        MarkdownTextFlow textFlow = markdownTextFlow();

        textFlow.setMarkdown("**bold**");

        assertEquals("bold", renderedText(textFlow));
        assertTrue(hasChildWithStyleClass(textFlow, "markdown-bold"));
    }

    @Test
    void setMarkdownStripsInlineMarkupButKeepsText() {
        MarkdownTextFlow textFlow = markdownTextFlow();

        textFlow.setMarkdown("Hello **world**");

        // The '**' markers are consumed by the parser; the words remain.
        assertEquals("Hello world", renderedText(textFlow));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\n"})
    void blankInputRendersNothing(String blank) {
        MarkdownTextFlow markdown = markdownTextFlow();
        MarkdownTextFlow plain = markdownTextFlow();

        markdown.setMarkdown(blank);
        plain.setPlainText(blank);

        assertTrue(markdown.getChildren().isEmpty());
        assertTrue(plain.getChildren().isEmpty());
    }

    @Test
    void switchingFromMarkdownToPlainTextReplacesContent() {
        MarkdownTextFlow textFlow = markdownTextFlow();

        textFlow.setMarkdown("**bold**");
        textFlow.setPlainText("**bold**");

        // Now interpreted verbatim: markup is visible and no bold styling remains.
        assertEquals(1, textFlow.getChildren().size());
        assertEquals("**bold**", renderedText(textFlow));
        assertFalse(hasChildWithStyleClass(textFlow, "markdown-bold"));
    }

    @Test
    void switchingFromPlainTextToMarkdownReplacesContent() {
        MarkdownTextFlow textFlow = markdownTextFlow();

        textFlow.setPlainText("**bold**");
        textFlow.setMarkdown("**bold**");

        assertEquals("bold", renderedText(textFlow));
        assertTrue(hasChildWithStyleClass(textFlow, "markdown-bold"));
    }

    private static boolean hasChildWithStyleClass(MarkdownTextFlow textFlow, String styleClass) {
        return textFlow.getChildren().stream().anyMatch(child -> child.getStyleClass().contains(styleClass));
    }
}
