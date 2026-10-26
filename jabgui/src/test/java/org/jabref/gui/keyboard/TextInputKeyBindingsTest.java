package org.jabref.gui.keyboard;

import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.skin.TextAreaSkin;
import javafx.scene.control.skin.TextInputControlSkin;

import org.jabref.gui.testutils.JavaFxExtension;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(JavaFxExtension.class)
@NullMarked
        // [utest->req~macos.text-editing-shortcuts~1]
class TextInputKeyBindingsTest {

    @Test
    void transposeCharactersAroundCaret() {
        TextField textField = new TextField("abcd");
        textField.positionCaret(2);

        TextInputKeyBindings.transposeCharacters(textField);

        assertEquals("acbd", textField.getText());
        assertEquals(3, textField.getCaretPosition());
    }

    @Test
    void transposeCharactersDoesNothingAtDocumentStart() {
        TextField textField = new TextField("abcd");
        textField.positionCaret(0);

        TextInputKeyBindings.transposeCharacters(textField);

        assertEquals("abcd", textField.getText());
        assertEquals(0, textField.getCaretPosition());
    }

    @Test
    void transposeCharactersKeepsSupplementaryCharactersIntact() {
        TextField textField = new TextField("acb");
        textField.positionCaret(1);

        TextInputKeyBindings.transposeCharacters(textField);

        assertEquals("cab", textField.getText());
        assertEquals(2, textField.getCaretPosition());
    }

    @Test
    void insertLineAfterCaretSplitsTextAndKeepsCaret() {
        TextArea textArea = new TextArea("abcdef");
        textArea.positionCaret(3);

        assertTrue(TextInputKeyBindings.insertLineAfterCaret(textArea));

        assertEquals("abc\ndef", textArea.getText());
        assertEquals(3, textArea.getCaretPosition());
    }

    @Test
    void insertLineAfterCaretAtEndOfTextKeepsCaret() {
        TextArea textArea = new TextArea("abc");
        textArea.positionCaret(3);

        assertTrue(TextInputKeyBindings.insertLineAfterCaret(textArea));

        assertEquals("abc\n", textArea.getText());
        assertEquals(3, textArea.getCaretPosition());
    }

    @Test
    void insertLineAfterCaretIgnoresSingleLineTextField() {
        TextField textField = new TextField("abc");
        textField.positionCaret(1);

        assertFalse(TextInputKeyBindings.insertLineAfterCaret(textField));

        assertEquals("abc", textField.getText());
        assertEquals(1, textField.getCaretPosition());
    }

    @Test
    void insertLineAfterCaretIgnoresReadOnlyTextArea() {
        TextArea textArea = new TextArea("abc");
        textArea.setEditable(false);
        textArea.positionCaret(1);

        assertFalse(TextInputKeyBindings.insertLineAfterCaret(textArea));

        assertEquals("abc", textArea.getText());
    }

    @ParameterizedTest
    @EnumSource(value = TextInputControlSkin.Direction.class, names = {"UP", "DOWN", "BEGINNING", "END"})
    void lineMovementInTextAreaUsesSkinLineNavigation(TextInputControlSkin.Direction direction) {
        TextArea textArea = mock(TextArea.class);
        TextAreaSkin skin = mock(TextAreaSkin.class);
        doReturn(skin).when(textArea).getSkin();

        TextInputKeyBindings.moveCaretByLine(textArea, direction);

        verify(skin).moveCaret(TextInputControlSkin.TextUnit.LINE, direction, false);
    }

    @ParameterizedTest
    @EnumSource(value = TextInputControlSkin.Direction.class, names = {"UP", "BEGINNING"})
    void lineMovementInTextFieldMovesToStart(TextInputControlSkin.Direction direction) {
        TextField textField = new TextField("abcd");
        textField.positionCaret(2);

        TextInputKeyBindings.moveCaretByLine(textField, direction);

        assertEquals(0, textField.getCaretPosition());
    }

    @ParameterizedTest
    @EnumSource(value = TextInputControlSkin.Direction.class, names = {"DOWN", "END"})
    void lineMovementInTextFieldMovesToEnd(TextInputControlSkin.Direction direction) {
        TextField textField = new TextField("abcd");
        textField.positionCaret(2);

        TextInputKeyBindings.moveCaretByLine(textField, direction);

        assertEquals(4, textField.getCaretPosition());
    }
}
