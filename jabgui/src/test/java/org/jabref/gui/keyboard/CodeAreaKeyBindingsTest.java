package org.jabref.gui.keyboard;

import java.util.Optional;
import java.util.stream.Stream;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import org.jabref.gui.testutils.JavaFxExtension;

import jfx.incubator.scene.control.richtext.CodeArea;
import jfx.incubator.scene.control.richtext.LineEnding;
import jfx.incubator.scene.control.richtext.TextPos;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(JavaFxExtension.class)
@NullMarked
        // [utest->req~macos.text-editing-shortcuts~1]
class CodeAreaKeyBindingsTest {

    @ParameterizedTest
    @MethodSource("verticalNavigationBoundaryData")
        // [utest->req~ux.text-editing.visual-line-navigation~1]
    void verticalNavigationHandlesShortAndEmptyLines(KeyBinding binding, TextPos start, TextPos expected) {
        JavaFxExtension.invokeAndWait(() -> {
            CodeArea codeArea = new CodeArea();
            codeArea.setText("abcdefghij\nx\n\nlastline");
            Stage stage = new Stage();
            stage.setScene(new Scene(codeArea, 400, 200));
            stage.show();
            codeArea.applyCss();
            codeArea.layout();
            codeArea.select(start);
            codeArea.requestLayout();
            codeArea.layout();
            KeyBindingRepository repository = mock(KeyBindingRepository.class);
            when(repository.mapToKeyBinding(any(KeyEvent.class))).thenReturn(Optional.of(binding));

            CodeAreaKeyBindings.call(codeArea, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.N, false, true, false, false), repository);

            assertEquals(expected.index(), codeArea.getCaretPosition().index());
            assertEquals(expected.offset(), codeArea.getCaretPosition().offset());
            if (start.equals(expected)) {
                codeArea.requestLayout();
                codeArea.layout();
                CodeAreaKeyBindings.call(codeArea, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.N, false, true, false, false), repository);
                assertEquals(expected.index(), codeArea.getCaretPosition().index());
                assertEquals(expected.offset(), codeArea.getCaretPosition().offset());
            }
        });
    }

    private static Stream<Arguments> verticalNavigationBoundaryData() {
        return Stream.of(
                Arguments.of(KeyBinding.EDITOR_DOWN, TextPos.ofLeading(0, 8), TextPos.ofLeading(1, 1)),
                Arguments.of(KeyBinding.EDITOR_DOWN, TextPos.ofLeading(1, 1), TextPos.ofLeading(2, 0)),
                Arguments.of(KeyBinding.EDITOR_DOWN, TextPos.ofLeading(2, 0), TextPos.ofLeading(3, 0)),
                Arguments.of(KeyBinding.EDITOR_UP, TextPos.ofLeading(2, 0), TextPos.ofLeading(1, 0)),
                Arguments.of(KeyBinding.EDITOR_UP, TextPos.ZERO, TextPos.ZERO),
                Arguments.of(KeyBinding.EDITOR_DOWN, TextPos.ofLeading(3, 8), TextPos.ofLeading(3, 8)));
    }

    @Test
        // [utest->req~ux.text-editing.visual-line-navigation~1]
    void verticalNavigationMovesAcrossWrappedVisualLines() {
        JavaFxExtension.invokeAndWait(() -> {
            CodeArea codeArea = new CodeArea();
            codeArea.setWrapText(true);
            codeArea.setText("A long paragraph that wraps over multiple visible lines without any newline characters. More words to ensure wrapping.");
            Stage stage = new Stage();
            stage.setScene(new Scene(codeArea, 150, 200));
            stage.show();
            codeArea.applyCss();
            codeArea.layout();
            codeArea.select(TextPos.ZERO);
            KeyBindingRepository repository = mock(KeyBindingRepository.class);
            when(repository.mapToKeyBinding(any(KeyEvent.class))).thenReturn(Optional.of(KeyBinding.EDITOR_DOWN), Optional.of(KeyBinding.EDITOR_UP));

            CodeAreaKeyBindings.call(codeArea, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.N, false, true, false, false), repository);
            int nextLine = codeArea.getCaretPosition().offset();
            assertTrue(nextLine > 0 && nextLine < codeArea.getText().length());
            codeArea.requestLayout();
            codeArea.layout();

            CodeAreaKeyBindings.call(codeArea, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.P, false, true, false, false), repository);
            assertEquals(TextPos.ZERO, codeArea.getCaretPosition());
        });
    }

    @ParameterizedTest
    @MethodSource("killLineTestData")
        // [utest->req~ux.text-editing.line-boundaries~2]
    void killLinePreservesFollowingLines(LineEnding lineEnding, String text, TextPos caretPosition, String expectedText) {
        JavaFxExtension.invokeAndWait(() -> {
            CodeArea codeArea = new CodeArea();
            new Scene(codeArea);
            codeArea.applyCss();
            codeArea.getModel().setLineEnding(lineEnding);
            codeArea.setText(text);
            codeArea.select(caretPosition);
            KeyBindingRepository repository = mock(KeyBindingRepository.class);
            when(repository.mapToKeyBinding(any(KeyEvent.class))).thenReturn(Optional.of(KeyBinding.EDITOR_KILL_LINE));
            KeyEvent event = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.K, false, true, false, false);

            CodeAreaKeyBindings.call(codeArea, event, repository);

            assertEquals(expectedText.replace("\n", lineEnding.getText()), codeArea.getText());
            assertEquals(caretPosition, codeArea.getCaretPosition());
            assertTrue(event.isConsumed());
            codeArea.undo();
            assertEquals(text, codeArea.getText());
        });
    }

    private static Stream<Arguments> killLineTestData() {
        return Stream.of(LineEnding.LF, LineEnding.CRLF).flatMap(lineEnding -> {
            String separator = lineEnding.getText();
            return Stream.of(
                    Arguments.of(lineEnding, "First" + separator + "Second" + separator + "Third", TextPos.ofLeading(0, 3), "Fir\nSecond\nThird"),
                    Arguments.of(lineEnding, "First" + separator + "Second" + separator + "Third", TextPos.ofLeading(0, 5), "FirstSecond\nThird"),
                    Arguments.of(lineEnding, "First" + separator + "Second" + separator + "Third", TextPos.ofLeading(1, 0), "First\n\nThird"),
                    Arguments.of(lineEnding, "First" + separator + "Second" + separator + "Third", TextPos.ofLeading(1, 3), "First\nSec\nThird"),
                    Arguments.of(lineEnding, "First" + separator + "Second" + separator + "Third", TextPos.ofLeading(2, 5), "First\nSecond\nThird"),
                    Arguments.of(lineEnding, "First" + separator, TextPos.ofLeading(0, 5), "First"),
                    Arguments.of(lineEnding, "First" + separator, TextPos.ofLeading(1, 0), "First\n"),
                    Arguments.of(lineEnding, "", TextPos.ZERO, ""));
        });
    }

    @Test
    void transposeCharactersAroundCaret() {
        CodeArea codeArea = new CodeArea();
        codeArea.setText("abcd");
        codeArea.select(TextPos.ofLeading(0, 2));
        KeyBindingRepository repository = new KeyBindingRepository();
        repository.put(KeyBinding.EDITOR_TRANSPOSE_CHARACTERS, "ctrl+T");
        KeyEvent event = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.T, false, true, false, false);

        CodeAreaKeyBindings.call(codeArea, event, repository);

        assertEquals("acbd", codeArea.getText());
        assertEquals(TextPos.ofLeading(0, 3), codeArea.getCaretPosition());
        assertTrue(event.isConsumed());
    }

    @Test
    void transposeCharactersDoesNothingAtDocumentStart() {
        CodeArea codeArea = new CodeArea();
        codeArea.setText("abcd");
        codeArea.select(TextPos.ofLeading(0, 0));

        CodeAreaKeyBindings.transposeCharacters(codeArea);

        assertEquals("abcd", codeArea.getText());
        assertEquals(TextPos.ofLeading(0, 0), codeArea.getCaretPosition());
    }

    @Test
    void nonMacOsDoesNothing() {
        CodeArea codeArea = mock(CodeArea.class);
        KeyEvent event = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.LEFT, false, false, false, true);

        CodeAreaKeyBindings.handleMacCursorMovementShortcuts(codeArea, event, false);

        verifyNoInteractions(codeArea);
        assertFalse(event.isConsumed());
    }

    @Test
    void commandLeftMovesToLineStart() {
        CodeArea codeArea = mock(CodeArea.class);
        KeyEvent event = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.LEFT, false, false, false, true);

        CodeAreaKeyBindings.handleMacCursorMovementShortcuts(codeArea, event, true);

        verify(codeArea).moveLineStart();
        assertTrue(event.isConsumed());
    }

    @Test
    void commandRightMovesToLineEnd() {
        CodeArea codeArea = mock(CodeArea.class);
        KeyEvent event = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.RIGHT, false, false, false, true);

        CodeAreaKeyBindings.handleMacCursorMovementShortcuts(codeArea, event, true);

        verify(codeArea).moveLineEnd();
        assertTrue(event.isConsumed());
    }

    @Test
    void optionLeftMovesWordBackward() {
        CodeArea codeArea = mock(CodeArea.class);
        KeyEvent event = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.LEFT, false, false, true, false);

        CodeAreaKeyBindings.handleMacCursorMovementShortcuts(codeArea, event, true);

        verify(codeArea).moveWordLeft();
        assertTrue(event.isConsumed());
    }

    @Test
    void optionRightMovesWordForward() {
        CodeArea codeArea = mock(CodeArea.class);
        KeyEvent event = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.RIGHT, false, false, true, false);

        CodeAreaKeyBindings.handleMacCursorMovementShortcuts(codeArea, event, true);

        verify(codeArea).moveWordRight();
        assertTrue(event.isConsumed());
    }

    @Test
    void shiftCommandLeftExtendsSelectionToLineStart() {
        CodeArea codeArea = mock(CodeArea.class);
        KeyEvent event = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.LEFT, true, false, false, true);

        CodeAreaKeyBindings.handleMacCursorMovementShortcuts(codeArea, event, true);

        verify(codeArea).selectToLineStart();
        assertTrue(event.isConsumed());
    }

    @Test
    void shiftCommandRightExtendsSelectionToLineEnd() {
        CodeArea codeArea = mock(CodeArea.class);
        KeyEvent event = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.RIGHT, true, false, false, true);

        CodeAreaKeyBindings.handleMacCursorMovementShortcuts(codeArea, event, true);

        verify(codeArea).selectToLineEnd();
        assertTrue(event.isConsumed());
    }

    @Test
    void commandUpMovesToDocumentStart() {
        CodeArea codeArea = mock(CodeArea.class);
        KeyEvent event = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.UP, false, false, false, true);

        CodeAreaKeyBindings.handleMacCursorMovementShortcuts(codeArea, event, true);

        verify(codeArea).moveDocumentStart();
        assertTrue(event.isConsumed());
    }

    @Test
    void commandDownMovesToDocumentEnd() {
        CodeArea codeArea = mock(CodeArea.class);
        KeyEvent event = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.DOWN, false, false, false, true);

        CodeAreaKeyBindings.handleMacCursorMovementShortcuts(codeArea, event, true);

        verify(codeArea).moveDocumentEnd();
        assertTrue(event.isConsumed());
    }

    @Test
    void optionUpMovesToParagraphStart() {
        CodeArea codeArea = mock(CodeArea.class);
        KeyEvent event = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.UP, false, false, true, false);

        CodeAreaKeyBindings.handleMacCursorMovementShortcuts(codeArea, event, true);

        verify(codeArea).moveParagraphStart();
        assertTrue(event.isConsumed());
    }

    @Test
    void optionDownMovesToParagraphEnd() {
        CodeArea codeArea = mock(CodeArea.class);
        KeyEvent event = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.DOWN, false, false, true, false);

        CodeAreaKeyBindings.handleMacCursorMovementShortcuts(codeArea, event, true);

        verify(codeArea).moveParagraphEnd();
        assertTrue(event.isConsumed());
    }

    @Test
    void shiftOptionUpExtendsSelectionToParagraphStart() {
        CodeArea codeArea = mock(CodeArea.class);
        KeyEvent event = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.UP, true, false, true, false);

        CodeAreaKeyBindings.handleMacCursorMovementShortcuts(codeArea, event, true);

        verify(codeArea).selectParagraphStart();
        assertTrue(event.isConsumed());
    }

    @Test
    void shiftOptionDownExtendsSelectionToParagraphEnd() {
        CodeArea codeArea = mock(CodeArea.class);
        KeyEvent event = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.DOWN, true, false, true, false);

        CodeAreaKeyBindings.handleMacCursorMovementShortcuts(codeArea, event, true);

        verify(codeArea).selectParagraphEnd();
        assertTrue(event.isConsumed());
    }

    @Test
    void shiftCommandUpExtendsSelectionToDocumentStart() {
        CodeArea codeArea = mock(CodeArea.class);
        KeyEvent event = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.UP, true, false, false, true);

        CodeAreaKeyBindings.handleMacCursorMovementShortcuts(codeArea, event, true);

        verify(codeArea).selectToDocumentStart();
        assertTrue(event.isConsumed());
    }

    @Test
    void shiftCommandDownExtendsSelectionToDocumentEnd() {
        CodeArea codeArea = mock(CodeArea.class);
        KeyEvent event = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.DOWN, true, false, false, true);

        CodeAreaKeyBindings.handleMacCursorMovementShortcuts(codeArea, event, true);

        verify(codeArea).selectToDocumentEnd();
        assertTrue(event.isConsumed());
    }

    @Test
    void nonArrowKeyIsIgnored() {
        CodeArea codeArea = mock(CodeArea.class);
        KeyEvent event = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.A, false, false, false, true);

        CodeAreaKeyBindings.handleMacCursorMovementShortcuts(codeArea, event, true);

        verifyNoInteractions(codeArea);
        assertFalse(event.isConsumed());
    }

    @Test
    void ctrlPMovesCaretUpOneLine() {
        CodeArea codeArea = mock(CodeArea.class);
        KeyBindingRepository repository = new KeyBindingRepository();
        repository.put(KeyBinding.EDITOR_UP, "ctrl+P");
        KeyEvent event = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.P, false, true, false, false);

        CodeAreaKeyBindings.call(codeArea, event, repository);

        verify(codeArea).moveUp();
        assertTrue(event.isConsumed());
    }

    @Test
    void ctrlNMovesCaretDownOneLine() {
        CodeArea codeArea = mock(CodeArea.class);
        KeyBindingRepository repository = new KeyBindingRepository();
        repository.put(KeyBinding.EDITOR_DOWN, "ctrl+N");
        KeyEvent event = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.N, false, true, false, false);

        CodeAreaKeyBindings.call(codeArea, event, repository);

        verify(codeArea).moveDown();
        assertTrue(event.isConsumed());
    }

    @Test
    void insertLineAfterCaretSplitsLineAndKeepsCaret() {
        CodeArea codeArea = new CodeArea();
        codeArea.setText("  title = {Hello}");
        codeArea.select(TextPos.ofLeading(0, 8));

        CodeAreaKeyBindings.insertLineAfterCaret(codeArea);

        assertEquals(2, codeArea.getParagraphCount());
        assertEquals("  title ", codeArea.getPlainText(0));
        assertEquals("= {Hello}", codeArea.getPlainText(1));
        assertEquals(0, codeArea.getCaretPosition().index());
        assertEquals(8, codeArea.getCaretPosition().offset());
    }

    @Test
    void insertLineAfterCaretAtEndOfLineKeepsCaretOnSameLine() {
        CodeArea codeArea = new CodeArea();
        codeArea.setText("abc");
        codeArea.select(TextPos.ofLeading(0, 3));

        CodeAreaKeyBindings.insertLineAfterCaret(codeArea);

        assertEquals(2, codeArea.getParagraphCount());
        assertEquals("abc", codeArea.getPlainText(0));
        assertEquals("", codeArea.getPlainText(1));
        assertEquals(0, codeArea.getCaretPosition().index());
        assertEquals(3, codeArea.getCaretPosition().offset());
    }

    @Test
    void insertLineAfterCaretDoesNothingWhenNotEditable() {
        CodeArea codeArea = new CodeArea();
        codeArea.setText("abc");
        codeArea.select(TextPos.ofLeading(0, 1));
        codeArea.setEditable(false);

        CodeAreaKeyBindings.insertLineAfterCaret(codeArea);

        assertEquals(1, codeArea.getParagraphCount());
        assertEquals("abc", codeArea.getPlainText(0));
    }

    @Test
    void ctrlOInsertsLineAfterCaret() {
        CodeArea codeArea = new CodeArea();
        codeArea.setText("abcdef");
        codeArea.select(TextPos.ofLeading(0, 3));
        KeyBindingRepository repository = new KeyBindingRepository();
        repository.put(KeyBinding.EDITOR_INSERT_LINE_AFTER, "ctrl+O");
        KeyEvent event = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.O, false, true, false, false);

        CodeAreaKeyBindings.call(codeArea, event, repository);

        assertTrue(event.isConsumed());
        assertEquals("abc", codeArea.getPlainText(0));
        assertEquals("def", codeArea.getPlainText(1));
        assertEquals(0, codeArea.getCaretPosition().index());
        assertEquals(3, codeArea.getCaretPosition().offset());
    }
}
