package org.jabref.gui.keyboard;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import jfx.incubator.scene.control.richtext.CodeArea;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(ApplicationExtension.class)
class CodeAreaKeyBindingsTest {

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
