package org.jabref.gui.keyboard;

import java.util.Optional;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import org.jabref.gui.testutils.JavaFxExtension;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@NullMarked
@ExtendWith(JavaFxExtension.class)
class TextInputKeyBindingsTest {

    private static final String MULTILINE_TEXT = "First line\nSecond line\nThird line";

    private void applyBinding(TextInputControl control, KeyBinding binding) {
        Scene scene = mock(Scene.class);
        KeyBindingRepository repository = mock(KeyBindingRepository.class);

        ReadOnlyObjectWrapper<Node> focusOwner = new ReadOnlyObjectWrapper<>(control);

        when(scene.focusOwnerProperty())
                .thenReturn(focusOwner.getReadOnlyProperty());

        when(repository.mapToKeyBinding(any(KeyEvent.class)))
                .thenReturn(Optional.of(binding));

        KeyEvent event = new KeyEvent(
                KeyEvent.KEY_PRESSED,
                "",
                "",
                KeyCode.A,
                false,
                true,
                false,
                false);

        TextInputKeyBindings.call(scene, event, repository);

        assertTrue(event.isConsumed());
    }

    @Test
    void beginningMovesToCurrentLineStart() {
        TextArea area = new TextArea(MULTILINE_TEXT);
        area.positionCaret(16);

        applyBinding(area, KeyBinding.EDITOR_BEGINNING);

        assertEquals(11, area.getCaretPosition());
        assertEquals(MULTILINE_TEXT, area.getText());
    }

    @Test
    void endMovesToCurrentLineEnd() {
        TextArea area = new TextArea(MULTILINE_TEXT);
        area.positionCaret(16);

        applyBinding(area, KeyBinding.EDITOR_END);

        assertEquals(22, area.getCaretPosition());
        assertEquals(MULTILINE_TEXT, area.getText());
    }

    @Test
    void beginningDocMovesToDocumentStart() {
        TextArea area = new TextArea(MULTILINE_TEXT);
        area.positionCaret(16);

        applyBinding(area, KeyBinding.EDITOR_BEGINNING_DOC);

        assertEquals(0, area.getCaretPosition());
    }

    @Test
    void endDocMovesToDocumentEnd() {
        TextArea area = new TextArea(MULTILINE_TEXT);
        area.positionCaret(16);

        applyBinding(area, KeyBinding.EDITOR_END_DOC);

        assertEquals(MULTILINE_TEXT.length(), area.getCaretPosition());
    }

    @Test
    void killLinePreservesFollowingLines() {
        TextArea area = new TextArea(MULTILINE_TEXT);
        area.positionCaret(16);

        applyBinding(area, KeyBinding.EDITOR_KILL_LINE);

        assertEquals(
                "First line\nSecon\nThird line",
                area.getText());
        assertEquals(16, area.getCaretPosition());
    }

    @Test
    void killLineAtLineEndRemovesNewline() {
        TextArea area = new TextArea(MULTILINE_TEXT);
        area.positionCaret(22);

        applyBinding(area, KeyBinding.EDITOR_KILL_LINE);

        assertEquals(
                "First line\nSecond lineThird line",
                area.getText());
    }

    @Test
    void killLineAtDocumentEndDoesNothing() {
        TextArea area = new TextArea(MULTILINE_TEXT);
        area.positionCaret(MULTILINE_TEXT.length());

        applyBinding(area, KeyBinding.EDITOR_KILL_LINE);

        assertEquals(MULTILINE_TEXT, area.getText());
    }

    @Test
    void beginningWorksInSingleLineTextField() {
        TextField field = new TextField("Example text");
        field.positionCaret(7);

        applyBinding(field, KeyBinding.EDITOR_BEGINNING);

        assertEquals(0, field.getCaretPosition());
    }

    @Test
    void endWorksInSingleLineTextField() {
        TextField field = new TextField("Example text");
        field.positionCaret(3);

        applyBinding(field, KeyBinding.EDITOR_END);

        assertEquals(field.getLength(), field.getCaretPosition());
    }

    @Test
    void killLineInSingleLineTextField() {
        TextField field = new TextField("Example text");
        field.positionCaret(7);

        applyBinding(field, KeyBinding.EDITOR_KILL_LINE);

        assertEquals("Example", field.getText());
    }
}
