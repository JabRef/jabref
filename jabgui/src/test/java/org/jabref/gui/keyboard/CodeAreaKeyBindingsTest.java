package org.jabref.gui.keyboard;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.NavigationActions;
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

        verify(codeArea).lineStart(NavigationActions.SelectionPolicy.CLEAR);
        assertTrue(event.isConsumed());
    }

    @Test
    void commandRightMovesToLineEnd() {
        CodeArea codeArea = mock(CodeArea.class);
        KeyEvent event = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.RIGHT, false, false, false, true);

        CodeAreaKeyBindings.handleMacCursorMovementShortcuts(codeArea, event, true);

        verify(codeArea).lineEnd(NavigationActions.SelectionPolicy.CLEAR);
        assertTrue(event.isConsumed());
    }

    @Test
    void optionLeftMovesWordBackward() {
        CodeArea codeArea = mock(CodeArea.class);
        KeyEvent event = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.LEFT, false, false, true, false);

        CodeAreaKeyBindings.handleMacCursorMovementShortcuts(codeArea, event, true);

        verify(codeArea).wordBreaksBackwards(2, NavigationActions.SelectionPolicy.CLEAR);
        assertTrue(event.isConsumed());
    }

    @Test
    void optionRightMovesWordForward() {
        CodeArea codeArea = mock(CodeArea.class);
        KeyEvent event = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.RIGHT, false, false, true, false);

        CodeAreaKeyBindings.handleMacCursorMovementShortcuts(codeArea, event, true);

        verify(codeArea).wordBreaksForwards(2, NavigationActions.SelectionPolicy.CLEAR);
        assertTrue(event.isConsumed());
    }

    @Test
    void shiftCommandLeftExtendsSelectionToLineStart() {
        CodeArea codeArea = mock(CodeArea.class);
        KeyEvent event = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.LEFT, true, false, false, true);

        CodeAreaKeyBindings.handleMacCursorMovementShortcuts(codeArea, event, true);

        verify(codeArea).lineStart(NavigationActions.SelectionPolicy.EXTEND);
        assertTrue(event.isConsumed());
    }

    @Test
    void shiftCommandRightExtendsSelectionToLineEnd() {
        CodeArea codeArea = mock(CodeArea.class);
        KeyEvent event = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.RIGHT, true, false, false, true);

        CodeAreaKeyBindings.handleMacCursorMovementShortcuts(codeArea, event, true);

        verify(codeArea).lineEnd(NavigationActions.SelectionPolicy.EXTEND);
        assertTrue(event.isConsumed());
    }

    @Test
    void commandUpMovesToDocumentStart() {
        CodeArea codeArea = mock(CodeArea.class);
        KeyEvent event = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.UP, false, false, false, true);

        CodeAreaKeyBindings.handleMacCursorMovementShortcuts(codeArea, event, true);

        verify(codeArea).start(NavigationActions.SelectionPolicy.CLEAR);
        assertTrue(event.isConsumed());
    }

    @Test
    void commandDownMovesToDocumentEnd() {
        CodeArea codeArea = mock(CodeArea.class);
        KeyEvent event = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.DOWN, false, false, false, true);

        CodeAreaKeyBindings.handleMacCursorMovementShortcuts(codeArea, event, true);

        verify(codeArea).end(NavigationActions.SelectionPolicy.CLEAR);
        assertTrue(event.isConsumed());
    }

    @Test
    void optionUpMovesToParagraphStart() {
        CodeArea codeArea = mock(CodeArea.class);
        KeyEvent event = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.UP, false, false, true, false);

        CodeAreaKeyBindings.handleMacCursorMovementShortcuts(codeArea, event, true);

        verify(codeArea).paragraphStart(NavigationActions.SelectionPolicy.CLEAR);
        assertTrue(event.isConsumed());
    }

    @Test
    void optionDownMovesToParagraphEnd() {
        CodeArea codeArea = mock(CodeArea.class);
        KeyEvent event = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.DOWN, false, false, true, false);

        CodeAreaKeyBindings.handleMacCursorMovementShortcuts(codeArea, event, true);

        verify(codeArea).paragraphEnd(NavigationActions.SelectionPolicy.CLEAR);
        assertTrue(event.isConsumed());
    }

    @Test
    void shiftOptionUpExtendsSelectionToParagraphStart() {
        CodeArea codeArea = mock(CodeArea.class);
        KeyEvent event = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.UP, true, false, true, false);

        CodeAreaKeyBindings.handleMacCursorMovementShortcuts(codeArea, event, true);

        verify(codeArea).paragraphStart(NavigationActions.SelectionPolicy.EXTEND);
        assertTrue(event.isConsumed());
    }

    @Test
    void shiftOptionDownExtendsSelectionToParagraphEnd() {
        CodeArea codeArea = mock(CodeArea.class);
        KeyEvent event = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.DOWN, true, false, true, false);

        CodeAreaKeyBindings.handleMacCursorMovementShortcuts(codeArea, event, true);

        verify(codeArea).paragraphEnd(NavigationActions.SelectionPolicy.EXTEND);
        assertTrue(event.isConsumed());
    }

    @Test
    void shiftCommandUpExtendsSelectionToDocumentStart() {
        CodeArea codeArea = mock(CodeArea.class);
        KeyEvent event = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.UP, true, false, false, true);

        CodeAreaKeyBindings.handleMacCursorMovementShortcuts(codeArea, event, true);

        verify(codeArea).start(NavigationActions.SelectionPolicy.EXTEND);
        assertTrue(event.isConsumed());
    }

    @Test
    void shiftCommandDownExtendsSelectionToDocumentEnd() {
        CodeArea codeArea = mock(CodeArea.class);
        KeyEvent event = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.DOWN, true, false, false, true);

        CodeAreaKeyBindings.handleMacCursorMovementShortcuts(codeArea, event, true);

        verify(codeArea).end(NavigationActions.SelectionPolicy.EXTEND);
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
