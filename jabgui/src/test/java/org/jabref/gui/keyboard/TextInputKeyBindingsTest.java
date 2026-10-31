package org.jabref.gui.keyboard;

import java.util.Optional;
import java.util.function.Supplier;
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
        // [utest->req~ux.text-editing.line-boundaries~1]
    void killLinePreservesFollowingLines() {
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
    @MethodSource("killLineTestData")
        // [utest->req~ux.text-editing.line-boundaries~1]
    void killLinePreservesBoundariesAndCanBeUndone(String text, int caretPosition, String expectedText) {
        TextArea area = new TextArea(text);
        area.positionCaret(caretPosition);

        applyBinding(area, KeyBinding.EDITOR_KILL_LINE);

        assertEquals(expectedText, area.getText());
        assertEquals(caretPosition, area.getCaretPosition());
        area.undo();
        assertEquals(text, area.getText());
    }

    private static Stream<Arguments> killLineTestData() {
        return Stream.of(
                Arguments.of("", 0, ""),
                Arguments.of("First\nSecond\nThird", 5, "FirstSecond\nThird"),
                Arguments.of("First\nSecond\nThird", 6, "First\n\nThird"),
                Arguments.of("First\nSecond\nThird", 18, "First\nSecond\nThird"),
                Arguments.of("First\n", 5, "First"),
                Arguments.of("First\n", 6, "First\n"));
    }

    @ParameterizedTest
    @MethodSource("nullTextTestData")
    void shortcutsAcceptNullText(Supplier<TextInputControl> controlFactory, KeyBinding binding) {
        TextInputControl control = controlFactory.get();
        control.setText(null);

        applyBinding(control, binding);

        assertEquals(0, control.getCaretPosition());
        assertEquals("", control.getText(0, control.getLength()));
    }

    private static Stream<Arguments> nullTextTestData() {
        return Stream.of(KeyBinding.EDITOR_BEGINNING, KeyBinding.EDITOR_END,
                             KeyBinding.EDITOR_BEGINNING_DOC, KeyBinding.EDITOR_END_DOC,
                             KeyBinding.EDITOR_UP, KeyBinding.EDITOR_DOWN, KeyBinding.EDITOR_KILL_LINE)
                     .flatMap(binding -> Stream.<Supplier<TextInputControl>>of(TextArea::new, TextField::new)
                                               .map(controlFactory -> Arguments.of(controlFactory, binding)));
    }

    @ParameterizedTest
    @MethodSource({"textFieldNavigationTestData", "textAreaNavigationTestData"})
        // [utest->req~ux.text-editing.line-boundaries~1]
    void navigationRespectsLineAndDocumentBoundaries(Supplier<TextInputControl> controlFactory, String text, int caretPosition, KeyBinding binding, int expectedPosition) {
        TextInputControl control = controlFactory.get();
        control.setText(text);
        control.positionCaret(caretPosition);

        applyBinding(control, binding);

        assertEquals(expectedPosition, control.getCaretPosition());
        assertEquals(text, control.getText());
    }

    private static Stream<Arguments> textFieldNavigationTestData() {
        Supplier<TextInputControl> controlFactory = TextField::new;
        return Stream.of(
                Arguments.of(controlFactory, "", 0, KeyBinding.EDITOR_BEGINNING, 0),
                Arguments.of(controlFactory, "", 0, KeyBinding.EDITOR_END, 0),
                Arguments.of(controlFactory, "", 0, KeyBinding.EDITOR_BEGINNING_DOC, 0),
                Arguments.of(controlFactory, "", 0, KeyBinding.EDITOR_END_DOC, 0),
                Arguments.of(controlFactory, "Example text", 7, KeyBinding.EDITOR_BEGINNING, 0),
                Arguments.of(controlFactory, "Example text", 3, KeyBinding.EDITOR_END, 12),
                Arguments.of(controlFactory, "", 0, KeyBinding.EDITOR_UP, 0),
                Arguments.of(controlFactory, "", 0, KeyBinding.EDITOR_DOWN, 0),
                Arguments.of(controlFactory, "Example text", 7, KeyBinding.EDITOR_UP, 0),
                Arguments.of(controlFactory, "Example text", 3, KeyBinding.EDITOR_DOWN, 12));
    }

    private static Stream<Arguments> textAreaNavigationTestData() {
        Supplier<TextInputControl> controlFactory = TextArea::new;
        return Stream.of(
                Arguments.of(controlFactory, "", 0, KeyBinding.EDITOR_BEGINNING, 0),
                Arguments.of(controlFactory, "", 0, KeyBinding.EDITOR_END, 0),
                Arguments.of(controlFactory, "", 0, KeyBinding.EDITOR_BEGINNING_DOC, 0),
                Arguments.of(controlFactory, "", 0, KeyBinding.EDITOR_END_DOC, 0),
                Arguments.of(controlFactory, "", 0, KeyBinding.EDITOR_UP, 0),
                Arguments.of(controlFactory, "", 0, KeyBinding.EDITOR_DOWN, 0),
                Arguments.of(controlFactory, "Example text", 7, KeyBinding.EDITOR_BEGINNING, 0),
                Arguments.of(controlFactory, "Example text", 3, KeyBinding.EDITOR_END, 12),
                Arguments.of(controlFactory, "Example text", 7, KeyBinding.EDITOR_UP, 0),
                Arguments.of(controlFactory, "Example text", 3, KeyBinding.EDITOR_DOWN, 12),
                Arguments.of(controlFactory, MULTILINE_TEXT, 5, KeyBinding.EDITOR_BEGINNING, 0),
                Arguments.of(controlFactory, MULTILINE_TEXT, 5, KeyBinding.EDITOR_END, 10),
                Arguments.of(controlFactory, MULTILINE_TEXT, 10, KeyBinding.EDITOR_BEGINNING, 0),
                Arguments.of(controlFactory, MULTILINE_TEXT, 10, KeyBinding.EDITOR_END, 10),
                Arguments.of(controlFactory, MULTILINE_TEXT, 11, KeyBinding.EDITOR_BEGINNING, 11),
                Arguments.of(controlFactory, MULTILINE_TEXT, 11, KeyBinding.EDITOR_END, 22),
                Arguments.of(controlFactory, MULTILINE_TEXT, 16, KeyBinding.EDITOR_BEGINNING, 11),
                Arguments.of(controlFactory, MULTILINE_TEXT, 16, KeyBinding.EDITOR_END, 22),
                Arguments.of(controlFactory, MULTILINE_TEXT, 16, KeyBinding.EDITOR_BEGINNING_DOC, 0),
                Arguments.of(controlFactory, MULTILINE_TEXT, 16, KeyBinding.EDITOR_END_DOC, 33),
                Arguments.of(controlFactory, MULTILINE_TEXT, 28, KeyBinding.EDITOR_UP, 16),
                Arguments.of(controlFactory, MULTILINE_TEXT, 5, KeyBinding.EDITOR_UP, 0),
                Arguments.of(controlFactory, MULTILINE_TEXT, 5, KeyBinding.EDITOR_DOWN, 16),
                Arguments.of(controlFactory, MULTILINE_TEXT, 20, KeyBinding.EDITOR_DOWN, 32),
                Arguments.of(controlFactory, MULTILINE_TEXT, 28, KeyBinding.EDITOR_DOWN, 33),
                Arguments.of(controlFactory, "Long line\nx", 6, KeyBinding.EDITOR_DOWN, 11),
                Arguments.of(controlFactory, MULTILINE_TEXT, 28, KeyBinding.EDITOR_BEGINNING, 23),
                Arguments.of(controlFactory, MULTILINE_TEXT, 28, KeyBinding.EDITOR_END, 33),
                Arguments.of(controlFactory, "First\n", 6, KeyBinding.EDITOR_BEGINNING, 6),
                Arguments.of(controlFactory, "First\n", 6, KeyBinding.EDITOR_END, 6),
                Arguments.of(controlFactory, "First\n", 6, KeyBinding.EDITOR_UP, 0),
                Arguments.of(controlFactory, "First\n", 5, KeyBinding.EDITOR_DOWN, 6),
                Arguments.of(controlFactory, MULTILINE_TEXT, 11, KeyBinding.EDITOR_UP, 0),
                Arguments.of(controlFactory, MULTILINE_TEXT, 10, KeyBinding.EDITOR_DOWN, 21));
    }
}
