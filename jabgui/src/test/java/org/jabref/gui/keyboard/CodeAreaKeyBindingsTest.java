package org.jabref.gui.keyboard;

import java.util.Optional;
import java.util.stream.Stream;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import org.jabref.gui.testutils.JavaFxExtension;

import jfx.incubator.scene.control.richtext.CodeArea;
import jfx.incubator.scene.control.richtext.TextPos;
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
class CodeAreaKeyBindingsTest {

    @ParameterizedTest
    @MethodSource("killLineTestData")
    void killLinePreservesFollowingLines(TextPos caretPosition, String expectedText) {
        CodeArea codeArea = new CodeArea();
        codeArea.setText("First\nSecond\nThird");
        codeArea.select(caretPosition);
        KeyBindingRepository repository = mock(KeyBindingRepository.class);
        when(repository.mapToKeyBinding(any(KeyEvent.class))).thenReturn(Optional.of(KeyBinding.EDITOR_KILL_LINE));
        KeyEvent event = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.K, false, true, false, false);

        CodeAreaKeyBindings.call(codeArea, event, repository);

        assertEquals(expectedText, codeArea.getText());
        assertEquals(caretPosition, codeArea.getCaretPosition());
        assertTrue(event.isConsumed());
    }

    private static Stream<Arguments> killLineTestData() {
        return Stream.of(
                Arguments.of(TextPos.ofLeading(0, 3), "Fir\nSecond\nThird"),
                Arguments.of(TextPos.ofLeading(0, 5), "FirstSecond\nThird"),
                Arguments.of(TextPos.ofLeading(1, 0), "First\n\nThird"),
                Arguments.of(TextPos.ofLeading(2, 5), "First\nSecond\nThird"));
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
}
