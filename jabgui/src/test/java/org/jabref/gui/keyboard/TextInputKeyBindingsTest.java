package org.jabref.gui.keyboard;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.ListChangeListener;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.skin.TextAreaSkin;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Window;

import org.jabref.gui.testutils.JavaFxExtension;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@NullMarked
@ExtendWith(JavaFxExtension.class)
class TextInputKeyBindingsTest {

    private static final String MULTILINE_TEXT = "First line\nSecond line\nThird line";

    private static TextArea layoutTextAreaWithCaretAt(String text, int caretPosition) {
        TextArea area = new TextArea(text);
        StackPane root = new StackPane(area);
        new Scene(root, 400, 200);
        root.applyCss();
        root.layout();
        area.positionCaret(caretPosition);
        root.layout();
        return area;
    }

    private static String numberedLines(int count) {
        return IntStream.rangeClosed(1, count).mapToObj(i -> "Line " + i).collect(Collectors.joining("\n"));
    }

    @Test
        // [utest->req~macos.text-editing-shortcuts~1]
    void centerCaretScrollsCaretLineToVerticalCenter() {
        JavaFxExtension.invokeAndWait(() -> {
            String text = numberedLines(60);
            TextArea area = layoutTextAreaWithCaretAt(text, text.indexOf("Line 30"));
            TextAreaSkin skin = (TextAreaSkin) area.getSkin();
            double lineHeight = skin.getCaretBounds().getHeight();

            assertTrue(TextInputKeyBindings.centerCaret(area));
            area.getParent().layout();

            assertEquals(area.getHeight() / 2, skin.getCaretBounds().getCenterY(), lineHeight);
            assertEquals(text.indexOf("Line 30"), area.getCaretPosition());
        });
    }

    @Test
        // [utest->req~macos.text-editing-shortcuts~1]
    void centerCaretDoesNotScrollAboveTextStart() {
        JavaFxExtension.invokeAndWait(() -> {
            TextArea area = layoutTextAreaWithCaretAt(numberedLines(60), 0);

            assertTrue(TextInputKeyBindings.centerCaret(area));

            assertEquals(0, area.getScrollTop());
        });
    }

    @Test
    void centerCaretUsesViewportWhenHorizontalScrollbarIsVisible() {
        JavaFxExtension.invokeAndWait(() -> {
            String text = IntStream.rangeClosed(1, 60)
                                   .mapToObj(i -> "Line " + i + " " + "wide text ".repeat(30))
                                   .collect(Collectors.joining("\n"));
            TextArea area = layoutTextAreaWithCaretAt(text, text.indexOf("Line 30 "));
            Node viewport = area.lookup(".scroll-pane .viewport");
            double viewportCenter = area.sceneToLocal(viewport.localToScene(viewport.getBoundsInLocal())).getCenterY();
            TextAreaSkin skin = (TextAreaSkin) area.getSkin();
            assertTrue(viewport.getBoundsInLocal().getHeight() < area.getHeight() - 5);

            assertTrue(TextInputKeyBindings.centerCaret(area));
            area.getParent().layout();

            assertEquals(viewportCenter, skin.getCaretBounds().getCenterY(), 1.0);
        });
    }

    @Test
    void centerCaretIgnoresSingleLineTextField() {
        TextField textField = new TextField("abc");

        assertFalse(TextInputKeyBindings.centerCaret(textField));
    }

    @Test
    @EnabledOnOs(OS.MAC)
    void optionBackspaceDeletesPreviousWordInTextArea() {
        JavaFxExtension.invokeAndWait(() -> {
            TextArea area = new TextArea("hello world");
            Stage stage = new Stage();
            stage.setScene(new Scene(area));
            stage.show();
            area.requestFocus();
            area.positionCaret(11);
            Event.fireEvent(area, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.BACK_SPACE, false, false, true, false));
            assertEquals("hello ", area.getText());
            stage.hide();
        });
    }

    @Test
    void transposeShortcutWorksInReopenedInputDialog() {
        JavaFxExtension.invokeAndWait(() -> {
            Stage mainWindow = new Stage();
            KeyBindingRepository repository = new KeyBindingRepository();
            repository.put(KeyBinding.EDITOR_TRANSPOSE_CHARACTERS, "ctrl+T");
            ListChangeListener<Window> listener = TextInputKeyBindings.registerSecondaryWindows(mainWindow, repository);
            TextInputDialog dialog = new TextInputDialog("abcd");
            try {
                dialog.show();
                dialog.hide();
                dialog.show();
                TextField editor = dialog.getEditor();
                editor.requestFocus();
                editor.positionCaret(2);
                Event.fireEvent(editor, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.T, false, true, false, false));
                assertEquals("acbd", editor.getText());
                assertEquals(3, editor.getCaretPosition());
            } finally {
                dialog.hide();
                Window.getWindows().removeListener(listener);
            }
        });
    }

    @Test
        // [utest->req~macos.text-editing-shortcuts~1]
    void transposeCharactersAroundCaret() {
        TextField textField = new TextField("abcd");
        textField.positionCaret(2);

        TextInputKeyBindings.transposeCharacters(textField);

        assertEquals("acbd", textField.getText());
        assertEquals(3, textField.getCaretPosition());
    }

    @ParameterizedTest
    @MethodSource("transposeBoundaryData")
    void transposeMatchesMacOsAtLineBoundaries(String text, int caret, String expected, int expectedCaret) {
        TextArea area = new TextArea(text);
        area.positionCaret(caret);
        TextInputKeyBindings.transposeCharacters(area);
        assertEquals(expected, area.getText());
        assertEquals(expectedCaret, area.getCaretPosition());
    }

    private static Stream<Arguments> transposeBoundaryData() {
        return Stream.of(
                Arguments.of("teh", 3, "the", 3), // spellchecker:disable-line
                Arguments.of("a\u0301b", 2, "ba\u0301", 3),
                Arguments.of("a\u0301b", 3, "ba\u0301", 3),
                Arguments.of("ab\n\ncd", 3, "a\nb\ncd", 3),
                Arguments.of("ab\ncd", 2, "ba\ncd", 2),
                Arguments.of("a\nb", 1, "a\nb", 1),
                Arguments.of("ab\ncd", 3, "abc\nd", 4));
    }

    @Test
    void readOnlyTextAreaIgnoresEditingShortcuts() {
        TextArea area = new TextArea("abcd");
        area.setEditable(false);
        area.positionCaret(2);
        Scene scene = mock(Scene.class);
        KeyBindingRepository repository = mock(KeyBindingRepository.class);
        when(scene.focusOwnerProperty()).thenReturn(new ReadOnlyObjectWrapper<Node>(area).getReadOnlyProperty());
        for (KeyBinding binding : new KeyBinding[] {KeyBinding.EDITOR_TRANSPOSE_CHARACTERS, KeyBinding.EDITOR_KILL_LINE, KeyBinding.EDITOR_CAPITALIZE}) {
            when(repository.mapToKeyBinding(any(KeyEvent.class))).thenReturn(Optional.of(binding));
            KeyEvent event = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.T, false, true, false, false);
            TextInputKeyBindings.call(scene, event, repository);
            assertFalse(event.isConsumed());
            assertEquals("abcd", area.getText());
            assertEquals(2, area.getCaretPosition());
        }
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
    @MethodSource("verticalNavigationBoundaryData")
        // [utest->req~ux.text-editing.visual-line-navigation~1]
    void verticalNavigationHandlesShortAndEmptyLines(KeyBinding binding, int start, int expected) {
        JavaFxExtension.invokeAndWait(() -> {
            TextArea area = new TextArea("abcdefghij\nx\n\nlastline");
            StackPane root = new StackPane(area);
            new Scene(root, 400, 200);
            root.applyCss();
            root.layout();
            area.positionCaret(start);
            root.requestLayout();
            root.layout();

            applyBinding(area, binding);

            assertEquals(expected, area.getCaretPosition());
            if (start == expected) {
                root.requestLayout();
                root.layout();
                applyBinding(area, binding);
                assertEquals(expected, area.getCaretPosition());
            }
        });
    }

    private static Stream<Arguments> verticalNavigationBoundaryData() {
        return Stream.of(
                Arguments.of(KeyBinding.EDITOR_DOWN, 8, 12),
                Arguments.of(KeyBinding.EDITOR_DOWN, 12, 13),
                Arguments.of(KeyBinding.EDITOR_DOWN, 13, 14),
                Arguments.of(KeyBinding.EDITOR_UP, 13, 11),
                Arguments.of(KeyBinding.EDITOR_UP, 0, 0),
                Arguments.of(KeyBinding.EDITOR_DOWN, 22, 22));
    }

    @Test
        // [utest->req~ux.text-editing.visual-line-navigation~1]
    void verticalNavigationMovesAcrossWrappedVisualLines() {
        JavaFxExtension.invokeAndWait(() -> {
            TextArea area = new TextArea("A long paragraph that wraps over multiple visible lines without any newline characters. More words to ensure wrapping.");
            area.setWrapText(true);
            StackPane root = new StackPane(area);
            new Scene(root, 150, 200);
            root.applyCss();
            root.layout();
            area.positionCaret(0);

            applyBinding(area, KeyBinding.EDITOR_DOWN);
            int nextLine = area.getCaretPosition();
            assertTrue(nextLine > 0 && nextLine < area.getLength());
            root.requestLayout();
            root.layout();

            applyBinding(area, KeyBinding.EDITOR_UP);
            assertEquals(0, area.getCaretPosition());
        });
    }

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
        // [utest->req~ux.text-editing.line-boundaries~2]
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
        // [utest->req~ux.text-editing.line-boundaries~2]
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
        // [utest->req~ux.text-editing.line-boundaries~2]
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
