package org.jabref.gui.keyboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.collections.ListChangeListener;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.skin.TextAreaSkin;
import javafx.scene.control.skin.TextInputControlSkin.Direction;
import javafx.scene.control.skin.TextInputControlSkin.TextUnit;
import javafx.scene.input.KeyEvent;
import javafx.stage.Window;

import org.jabref.logic.util.strings.StringManipulator;
import org.jabref.model.util.ResultingStringState;

public class TextInputKeyBindings {

    private static final Pattern GRAPHEME = Pattern.compile("\\X");

    public static ListChangeListener<Window> registerSecondaryWindows(Window mainWindow, KeyBindingRepository keyBindingRepository) {
        EventHandler<KeyEvent> handler = event -> {
            if (event.getTarget() instanceof Node node && node.getScene() != null) {
                call(node.getScene(), event, keyBindingRepository);
            }
        };
        ListChangeListener<Window> listener = change -> {
            while (change.next()) {
                for (Window window : change.getRemoved()) {
                    window.removeEventFilter(KeyEvent.KEY_PRESSED, handler);
                }
                for (Window window : change.getAddedSubList()) {
                    if (window != mainWindow) {
                        window.addEventFilter(KeyEvent.KEY_PRESSED, handler);
                    }
                }
            }
        };
        Window.getWindows().addListener(listener);
        return listener;
    }

    // [impl->req~macos.text-editing-shortcuts~1]
    // [impl->req~ux.text-editing.line-boundaries~2]
    // [impl->req~ux.text-editing.visual-line-navigation~1]
    public static void call(Scene scene, KeyEvent event, KeyBindingRepository keyBindingRepository) {
        if (scene.focusOwnerProperty().get() instanceof TextInputControl focusedTextField) {
            keyBindingRepository.mapToKeyBinding(event).ifPresent(binding -> {
                if (!focusedTextField.isEditable() && switch (binding) {
                    case EDITOR_DELETE,
                         EDITOR_INSERT_LINE_AFTER,
                         EDITOR_TRANSPOSE_CHARACTERS,
                         EDITOR_CAPITALIZE,
                         EDITOR_LOWERCASE,
                         EDITOR_UPPERCASE,
                         EDITOR_KILL_LINE,
                         EDITOR_KILL_WORD,
                         EDITOR_KILL_WORD_BACKWARD ->
                            true;
                    default ->
                            false;
                }) {
                    return;
                }
                switch (binding) {
                    case EDITOR_DELETE -> {
                        focusedTextField.deleteNextChar();
                        event.consume();
                    }
                    case EDITOR_BACKWARD -> {
                        focusedTextField.backward();
                        event.consume();
                    }
                    case EDITOR_FORWARD -> {
                        focusedTextField.forward();
                        event.consume();
                    }
                    case EDITOR_WORD_BACKWARD -> {
                        focusedTextField.previousWord();
                        event.consume();
                    }
                    case EDITOR_WORD_FORWARD -> {
                        focusedTextField.nextWord();
                        event.consume();
                    }
                    case EDITOR_BEGINNING -> {
                        int caret = focusedTextField.getCaretPosition();
                        String text = focusedTextField.getText(0, focusedTextField.getLength());
                        focusedTextField.positionCaret(text.lastIndexOf('\n', caret - 1) + 1);
                        event.consume();
                    }
                    case EDITOR_END -> {
                        int caret = focusedTextField.getCaretPosition();
                        String text = focusedTextField.getText(0, focusedTextField.getLength());
                        int lineEnd = text.indexOf('\n', caret);
                        focusedTextField.positionCaret(lineEnd == -1 ? text.length() : lineEnd);
                        event.consume();
                    }
                    case EDITOR_UP -> {
                        moveCaretUp(focusedTextField);
                        event.consume();
                    }
                    case EDITOR_BEGINNING_DOC -> {
                        focusedTextField.home();
                        event.consume();
                    }
                    case EDITOR_DOWN -> {
                        moveCaretDown(focusedTextField);
                        event.consume();
                    }
                    case EDITOR_END_DOC -> {
                        focusedTextField.end();
                        event.consume();
                    }
                    case EDITOR_CENTER_CARET -> {
                        if (centerCaret(focusedTextField)) {
                            event.consume();
                        }
                    }
                    case EDITOR_INSERT_LINE_AFTER -> {
                        if (insertLineAfterCaret(focusedTextField)) {
                            event.consume();
                        }
                    }
                    case EDITOR_TRANSPOSE_CHARACTERS -> {
                        if (focusedTextField.getSelection().getLength() == 0 && transposeCharacters(focusedTextField)) {
                            event.consume();
                        }
                    }
                    case EDITOR_CAPITALIZE -> {
                        int pos = focusedTextField.getCaretPosition();
                        String text = focusedTextField.getText(0, focusedTextField.getText().length());
                        ResultingStringState res = StringManipulator.capitalize(pos, text);
                        focusedTextField.setText(res.text);
                        focusedTextField.positionCaret(res.caretPosition);
                        event.consume();
                    }
                    case EDITOR_LOWERCASE -> {
                        int pos = focusedTextField.getCaretPosition();
                        String text = focusedTextField.getText(0, focusedTextField.getText().length());
                        ResultingStringState res = StringManipulator.lowercase(pos, text);
                        focusedTextField.setText(res.text);
                        focusedTextField.positionCaret(res.caretPosition);
                        event.consume();
                    }
                    case EDITOR_UPPERCASE -> {
                        int pos = focusedTextField.getCaretPosition();
                        String text = focusedTextField.getText(0, focusedTextField.getText().length());
                        ResultingStringState res = StringManipulator.uppercase(pos, text);
                        focusedTextField.setText(res.text);
                        focusedTextField.positionCaret(res.caretPosition);
                        event.consume();
                    }
                    case EDITOR_KILL_LINE -> {
                        int pos = focusedTextField.getCaretPosition();
                        String text = focusedTextField.getText(0, focusedTextField.getLength());
                        int lineEnd = text.indexOf('\n', pos);
                        if (lineEnd == -1) {
                            lineEnd = text.length();
                        } else if (lineEnd == pos) {
                            lineEnd++;
                        }
                        focusedTextField.deleteText(pos, lineEnd);
                        event.consume();
                    }
                    case EDITOR_KILL_WORD -> {
                        int pos = focusedTextField.getCaretPosition();
                        String text = focusedTextField.getText(0, focusedTextField.getText().length());
                        ResultingStringState res = StringManipulator.killWord(pos, text);
                        focusedTextField.setText(res.text);
                        focusedTextField.positionCaret(res.caretPosition);
                        event.consume();
                    }
                    case EDITOR_KILL_WORD_BACKWARD -> {
                        int pos = focusedTextField.getCaretPosition();
                        String text = focusedTextField.getText(0, focusedTextField.getText().length());
                        ResultingStringState res = StringManipulator.backwardKillWord(pos, text);
                        focusedTextField.setText(res.text);
                        focusedTextField.positionCaret(res.caretPosition);
                        event.consume();
                    }
                    // CLEAR_SEARCH is handled at org.jabref.gui.search.SearchTextField
                }
            });
        }
    }

    /// macOS Ctrl+L: scrolls a multi-line [TextArea] so that the caret line is vertically centered, without moving the caret.
    /// At the beginning and the end of the text, the scrolling stops at the text boundary.
    ///
    /// @return true if the text input is a text area whose scroll position was adjusted
    static boolean centerCaret(TextInputControl textInput) {
        if (!(textInput instanceof TextArea textArea)
                || !(textArea.getSkin() instanceof TextAreaSkin skin)
                || !(textArea.lookup(".scroll-pane") instanceof ScrollPane scrollPane)
                || (scrollPane.getContent() == null)) {
            return false;
        }
        Node viewport = scrollPane.lookup(".viewport");
        if (viewport == null) {
            return false;
        }
        double scrollTopMax = Math.max(0, scrollPane.getContent().getLayoutBounds().getHeight() - scrollPane.getViewportBounds().getHeight());
        double viewportCenter = textArea.sceneToLocal(viewport.localToScene(viewport.getBoundsInLocal())).getCenterY();
        // Caret bounds are relative to the text area, so they already reflect the current scroll position
        double scrollTop = textArea.getScrollTop() + skin.getCaretBounds().getCenterY() - viewportCenter;
        textArea.setScrollTop(Math.clamp(scrollTop, 0, scrollTopMax));
        return true;
    }

    /// macOS Ctrl+O: inserts a line break at the caret without moving the caret.
    /// Only editable text areas are supported, because single-line fields cannot contain line breaks.
    ///
    /// @return true if a line break was inserted
    static boolean insertLineAfterCaret(TextInputControl textInput) {
        if (!(textInput instanceof TextArea) || !textInput.isEditable()) {
            return false;
        }
        int caret = textInput.getCaretPosition();
        textInput.insertText(caret, "\n");
        textInput.positionCaret(caret);
        return true;
    }

    static boolean transposeCharacters(TextInputControl textInput) {
        if (!textInput.isEditable()) {
            return false;
        }
        String text = textInput.getText();
        Optional<int[]> offsets = transposeOffsets(text, textInput.getCaretPosition());
        if (offsets.isEmpty()) {
            return false;
        }
        int[] range = offsets.orElseThrow();
        String replacement = text.substring(range[1], range[2]) + text.substring(range[0], range[1]);
        textInput.replaceText(range[0], range[2], replacement);
        textInput.positionCaret(range[2]);
        return true;
    }

    static Optional<int[]> transposeOffsets(String text, int caret) {
        List<Integer> boundaries = new ArrayList<>();
        boundaries.add(0);
        Matcher matcher = GRAPHEME.matcher(text);
        while (matcher.find()) {
            boundaries.add(matcher.end());
        }
        int index = boundaries.indexOf(caret);
        if (index <= 0) {
            return Optional.empty();
        }
        boolean atLineEnd = caret < text.length() && text.charAt(caret) == '\n';
        if (caret == text.length() || atLineEnd) {
            index--;
            if (index == 0 || (atLineEnd && text.charAt(boundaries.get(index) - 1) == '\n')) {
                return Optional.empty();
            }
        }
        return Optional.of(new int[] {boundaries.get(index - 1), boundaries.get(index), boundaries.get(index + 1)});
    }

    private static void moveCaretUp(TextInputControl control) {
        if (control.getSkin() instanceof TextAreaSkin skin) {
            skin.moveCaret(TextUnit.LINE, Direction.UP, false);
            return;
        }
        int caret = control.getCaretPosition();
        String text = control.getText(0, control.getLength());
        int lineStart = text.lastIndexOf('\n', caret - 1) + 1;
        if (lineStart == 0) {
            control.positionCaret(0);
            return;
        }

        int previousLineEnd = lineStart - 1;
        int previousLineStart = text.lastIndexOf('\n', previousLineEnd - 1) + 1;
        control.positionCaret(Math.min(previousLineStart + caret - lineStart, previousLineEnd));
    }

    private static void moveCaretDown(TextInputControl control) {
        if (control.getSkin() instanceof TextAreaSkin skin) {
            skin.moveCaret(TextUnit.LINE, Direction.DOWN, false);
            return;
        }
        int caret = control.getCaretPosition();
        String text = control.getText(0, control.getLength());
        int lineStart = text.lastIndexOf('\n', caret - 1) + 1;
        int lineEnd = text.indexOf('\n', caret);
        if (lineEnd == -1) {
            control.positionCaret(text.length());
            return;
        }

        int nextLineEnd = text.indexOf('\n', lineEnd + 1);
        control.positionCaret(Math.min(lineEnd + 1 + caret - lineStart, nextLineEnd == -1 ? text.length() : nextLineEnd));
    }
}
