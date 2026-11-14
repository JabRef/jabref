package org.jabref.gui.keyboard;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import org.jabref.logic.os.OS;
import org.jabref.logic.util.strings.StringManipulator;
import org.jabref.model.util.ResultingStringState;

import jfx.incubator.scene.control.richtext.CodeArea;
import jfx.incubator.scene.control.richtext.TextPos;

public class CodeAreaKeyBindings {

    // [impl->req~macos.text-editing-shortcuts~1]
    // [impl->req~ux.text-editing.visual-line-navigation~1]
    public static void call(CodeArea codeArea, KeyEvent event, KeyBindingRepository keyBindingRepository) {
        handleMacCursorMovementShortcuts(codeArea, event);
        if (event.isConsumed()) {
            return;
        }

        keyBindingRepository.mapToKeyBinding(event).ifPresent(binding -> {
            if (!codeArea.isEditable() && switch (binding) {
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
                    codeArea.delete();
                    event.consume();
                }
                case EDITOR_BACKWARD -> {
                    codeArea.moveLeft();
                    event.consume();
                }
                case EDITOR_FORWARD -> {
                    codeArea.moveRight();
                    event.consume();
                }
                case EDITOR_WORD_BACKWARD -> {
                    codeArea.moveWordLeft();
                    event.consume();
                }
                case EDITOR_WORD_FORWARD -> {
                    codeArea.moveWordRight();
                    event.consume();
                }
                case EDITOR_BEGINNING_DOC -> {
                    codeArea.moveDocumentStart();
                    event.consume();
                }
                case EDITOR_UP -> {
                    codeArea.moveUp();
                    event.consume();
                }
                case EDITOR_BEGINNING -> {
                    codeArea.moveLineStart();
                    event.consume();
                }
                case EDITOR_END_DOC -> {
                    codeArea.moveDocumentEnd();
                    event.consume();
                }
                case EDITOR_DOWN -> {
                    codeArea.moveDown();
                    event.consume();
                }
                case EDITOR_END -> {
                    codeArea.moveLineEnd();
                    event.consume();
                }
                case EDITOR_CAPITALIZE -> {
                    int pos = textPosToOffset(codeArea, codeArea.getCaretPosition());
                    String text = codeArea.getText();
                    ResultingStringState res = StringManipulator.capitalize(pos, text);
                    codeArea.setText(res.text);
                    codeArea.select(offsetToTextPos(codeArea, res.caretPosition));
                    event.consume();
                }
                case EDITOR_LOWERCASE -> {
                    int pos = textPosToOffset(codeArea, codeArea.getCaretPosition());
                    String text = codeArea.getText();
                    ResultingStringState res = StringManipulator.lowercase(pos, text);
                    codeArea.setText(res.text);
                    codeArea.select(offsetToTextPos(codeArea, res.caretPosition));
                    event.consume();
                }
                case EDITOR_UPPERCASE -> {
                    int pos = textPosToOffset(codeArea, codeArea.getCaretPosition());
                    String text = codeArea.getText();
                    ResultingStringState res = StringManipulator.uppercase(pos, text);
                    codeArea.clear();
                    codeArea.setText(res.text);
                    codeArea.select(offsetToTextPos(codeArea, res.caretPosition));
                    event.consume();
                }
                case EDITOR_KILL_LINE -> {
                    // [impl->req~ux.text-editing.line-boundaries~2]
                    TextPos caret = codeArea.getCaretPosition();
                    int lineEnd = codeArea.getPlainText(caret.index()).length();
                    TextPos end = TextPos.ofLeading(caret.index(), lineEnd);
                    if (caret.offset() == lineEnd && caret.index() + 1 < codeArea.getParagraphCount()) {
                        end = TextPos.ofLeading(caret.index() + 1, 0);
                    }
                    codeArea.replaceText(caret, end, "");
                    event.consume();
                }
                case EDITOR_KILL_WORD -> {
                    int pos = textPosToOffset(codeArea, codeArea.getCaretPosition());
                    String text = codeArea.getText();
                    ResultingStringState res = StringManipulator.killWord(pos, text);
                    codeArea.setText(res.text);
                    codeArea.select(offsetToTextPos(codeArea, res.caretPosition));
                    event.consume();
                }
                case EDITOR_KILL_WORD_BACKWARD -> {
                    int pos = textPosToOffset(codeArea, codeArea.getCaretPosition());
                    String text = codeArea.getText();
                    ResultingStringState res = StringManipulator.backwardKillWord(pos, text);
                    codeArea.setText(res.text);
                    codeArea.select(offsetToTextPos(codeArea, res.caretPosition));
                    event.consume();
                }
                // EDITOR_CENTER_CARET is not handled: the incubator CodeArea has no public API to scroll the viewport, see https://github.com/JabRef/jabref/issues/17771
                case EDITOR_INSERT_LINE_AFTER -> {
                    insertLineAfterCaret(codeArea);
                    event.consume();
                }
                case EDITOR_TRANSPOSE_CHARACTERS -> {
                    if (!codeArea.hasNonEmptySelection() && transposeCharacters(codeArea)) {
                        event.consume();
                    }
                }
            }
        });
    }

    /// macOS Ctrl+O: inserts a line break at the caret without moving the caret,
    /// so the caret stays at the end of the first part of the split line.
    static void insertLineAfterCaret(CodeArea codeArea) {
        TextPos caret = codeArea.getCaretPosition();
        if (caret == null || !codeArea.isEditable()) {
            return;
        }
        codeArea.replaceText(caret, caret, "\n");
        codeArea.select(caret);
    }

    static boolean transposeCharacters(CodeArea codeArea) {
        if (!codeArea.isEditable()) {
            return false;
        }
        String text = IntStream.range(0, codeArea.getParagraphCount())
                               .mapToObj(codeArea::getPlainText)
                               .collect(Collectors.joining("\n"));
        Optional<int[]> offsets = TextInputKeyBindings.transposeOffsets(text, textPosToOffset(codeArea, codeArea.getCaretPosition()));
        if (offsets.isEmpty()) {
            return false;
        }
        int[] range = offsets.orElseThrow();
        String replacement = text.substring(range[1], range[2]) + text.substring(range[0], range[1]);
        codeArea.replaceText(
                offsetToTextPos(codeArea, range[0]),
                offsetToTextPos(codeArea, range[2]),
                replacement);
        codeArea.select(offsetToTextPos(codeArea, range[2]));
        return true;
    }

    private static void handleMacCursorMovementShortcuts(CodeArea codeArea, KeyEvent event) {
        handleMacCursorMovementShortcuts(codeArea, event, OS.OS_X);
    }

    static void handleMacCursorMovementShortcuts(CodeArea codeArea, KeyEvent event, boolean isMacOs) {
        if (!isMacOs) {
            return;
        }

        KeyCode code = event.getCode();
        boolean isHorizontal = (code == KeyCode.LEFT) || (code == KeyCode.RIGHT);
        boolean isVertical = (code == KeyCode.UP) || (code == KeyCode.DOWN);
        if (!isHorizontal && !isVertical) {
            return;
        }

        boolean extendSelection = event.isShiftDown();
        boolean optionOnly = event.isAltDown() && !event.isMetaDown() && !event.isControlDown();
        boolean commandOnly = event.isMetaDown() && !event.isAltDown() && !event.isControlDown();

        if (isHorizontal) {
            if (optionOnly) {
                if (code == KeyCode.LEFT) {
                    if (extendSelection) {
                        codeArea.selectWordLeft();
                    } else {
                        codeArea.moveWordLeft();
                    }
                } else {
                    if (extendSelection) {
                        codeArea.selectWordRight();
                    } else {
                        codeArea.moveWordRight();
                    }
                }
                event.consume();
            } else if (commandOnly) {
                if (code == KeyCode.LEFT) {
                    if (extendSelection) {
                        codeArea.selectToLineStart();
                    } else {
                        codeArea.moveLineStart();
                    }
                } else {
                    if (extendSelection) {
                        codeArea.selectToLineEnd();
                    } else {
                        codeArea.moveLineEnd();
                    }
                }
                event.consume();
            }
        } else if (optionOnly) {
            if (code == KeyCode.UP) {
                if (extendSelection) {
                    codeArea.selectParagraphStart();
                } else {
                    codeArea.moveParagraphStart();
                }
            } else {
                if (extendSelection) {
                    codeArea.selectParagraphEnd();
                } else {
                    codeArea.moveParagraphEnd();
                }
            }
            event.consume();
        } else if (commandOnly) {
            if (code == KeyCode.UP) {
                if (extendSelection) {
                    codeArea.selectToDocumentStart();
                } else {
                    codeArea.moveDocumentStart();
                }
            } else {
                if (extendSelection) {
                    codeArea.selectToDocumentEnd();
                } else {
                    codeArea.moveDocumentEnd();
                }
            }
            event.consume();
        }
    }

    /// Converts a flat document offset (as used by [StringManipulator]) into a
    /// paragraph/column based [TextPos], by walking the paragraphs of the model.
    /// Assumes a single-character line separator (matches JabRef's BibTeX source usage).
    private static TextPos offsetToTextPos(CodeArea codeArea, int offset) {
        int remaining = offset;
        int paragraphCount = codeArea.getParagraphCount();
        for (int i = 0; i < paragraphCount; i++) {
            String line = codeArea.getPlainText(i);
            int lineLength = line.length();
            if (remaining <= lineLength) {
                return TextPos.ofLeading(i, remaining);
            }
            remaining -= lineLength + 1; // +1 for the line separator
        }
        return codeArea.getDocumentEnd();
    }

    /// Inverse of [#offsetToTextPos(CodeArea, int)].
    private static int textPosToOffset(CodeArea codeArea, TextPos pos) {
        int offset = 0;
        for (int i = 0; i < pos.index(); i++) {
            offset += codeArea.getPlainText(i).length() + 1;
        }
        return offset + pos.offset();
    }
}
