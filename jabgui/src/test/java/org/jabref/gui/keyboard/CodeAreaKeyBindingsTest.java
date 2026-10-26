package org.jabref.gui.keyboard;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import org.jabref.gui.testutils.JavaFxExtension;

import jfx.incubator.scene.control.richtext.CodeArea;
import jfx.incubator.scene.control.richtext.TextPos;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(JavaFxExtension.class)
@NullMarked
        // [utest->req~macos.text-editing-shortcuts~1]
class CodeAreaKeyBindingsTest {

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
