package org.jabref.gui.keyboard;

import javafx.scene.control.TextField;

import org.jabref.gui.testutils.JavaFxExtension;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

}
