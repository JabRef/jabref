package org.jabref.gui.keyboard;

import java.util.Optional;
import java.util.stream.Stream;

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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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
    void killLineUsesSharedLineDeletion() {
        TextArea area = new TextArea(MULTILINE_TEXT);
        area.positionCaret(16);

        applyBinding(area, KeyBinding.EDITOR_KILL_LINE);

        assertEquals("First line\nSecon\nThird line", area.getText());
        assertEquals(16, area.getCaretPosition());
    }

    @Test
    void killLineCanBeUndoneInTextField() {
        TextField field = new TextField("search query");
        field.positionCaret(7);

        applyBinding(field, KeyBinding.EDITOR_KILL_LINE);

        assertEquals("search ", field.getText());
        assertEquals(7, field.getCaretPosition());
        field.undo();
        assertEquals("search query", field.getText());
    }

    @ParameterizedTest
    @MethodSource("navigationTestData")
    void navigationRespectsLineAndDocumentBoundaries(boolean multiline, String text, int caretPosition, KeyBinding binding, int expectedPosition) {
        TextInputControl control = multiline ? new TextArea(text) : new TextField(text);
        control.positionCaret(caretPosition);

        applyBinding(control, binding);

        assertEquals(expectedPosition, control.getCaretPosition());
        assertEquals(text, control.getText());
    }

    private static Stream<Arguments> navigationTestData() {
        return Stream.concat(Stream.of(true, false).flatMap(multiline -> Stream.of(
                        Arguments.of(multiline, "", 0, KeyBinding.EDITOR_BEGINNING, 0),
                        Arguments.of(multiline, "", 0, KeyBinding.EDITOR_END, 0),
                        Arguments.of(multiline, "", 0, KeyBinding.EDITOR_BEGINNING_DOC, 0),
                        Arguments.of(multiline, "", 0, KeyBinding.EDITOR_END_DOC, 0),
                        Arguments.of(multiline, "Example text", 7, KeyBinding.EDITOR_BEGINNING, 0),
                        Arguments.of(multiline, "Example text", 3, KeyBinding.EDITOR_END, 12))),
                Stream.of(
                        Arguments.of(true, MULTILINE_TEXT, 5, KeyBinding.EDITOR_BEGINNING, 0),
                        Arguments.of(true, MULTILINE_TEXT, 5, KeyBinding.EDITOR_END, 10),
                        Arguments.of(true, MULTILINE_TEXT, 10, KeyBinding.EDITOR_BEGINNING, 0),
                        Arguments.of(true, MULTILINE_TEXT, 10, KeyBinding.EDITOR_END, 10),
                        Arguments.of(true, MULTILINE_TEXT, 11, KeyBinding.EDITOR_BEGINNING, 11),
                        Arguments.of(true, MULTILINE_TEXT, 11, KeyBinding.EDITOR_END, 22),
                        Arguments.of(true, MULTILINE_TEXT, 16, KeyBinding.EDITOR_BEGINNING, 11),
                        Arguments.of(true, MULTILINE_TEXT, 16, KeyBinding.EDITOR_END, 22),
                        Arguments.of(true, MULTILINE_TEXT, 16, KeyBinding.EDITOR_BEGINNING_DOC, 0),
                        Arguments.of(true, MULTILINE_TEXT, 16, KeyBinding.EDITOR_END_DOC, 33),
                        Arguments.of(true, MULTILINE_TEXT, 28, KeyBinding.EDITOR_UP, 16),
                        Arguments.of(true, MULTILINE_TEXT, 5, KeyBinding.EDITOR_UP, 0),
                        Arguments.of(true, MULTILINE_TEXT, 5, KeyBinding.EDITOR_DOWN, 16),
                        Arguments.of(true, MULTILINE_TEXT, 20, KeyBinding.EDITOR_DOWN, 32),
                        Arguments.of(true, MULTILINE_TEXT, 28, KeyBinding.EDITOR_DOWN, 33),
                        Arguments.of(true, "Long line\nx", 6, KeyBinding.EDITOR_DOWN, 11),
                        Arguments.of(true, MULTILINE_TEXT, 28, KeyBinding.EDITOR_BEGINNING, 23),
                        Arguments.of(true, MULTILINE_TEXT, 28, KeyBinding.EDITOR_END, 33),
                        Arguments.of(true, "First\n", 6, KeyBinding.EDITOR_BEGINNING, 6),
                        Arguments.of(true, "First\n", 6, KeyBinding.EDITOR_END, 6)));
    }
}
