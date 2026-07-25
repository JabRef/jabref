package org.jabref.gui.keyboard;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import org.jabref.logic.os.OS;
import org.jabref.logic.util.strings.StringManipulator;
import org.jabref.model.util.ResultingStringState;

import jfx.incubator.scene.control.richtext.CodeArea;
import jfx.incubator.scene.control.richtext.TextPos;

public class CodeAreaKeyBindings {

    public static void call(CodeArea codeArea, KeyEvent event, KeyBindingRepository keyBindingRepository) {
        handleMacCursorMovementShortcuts(codeArea, event);
        if (event.isConsumed()) {
            return;
        }

        keyBindingRepository.mapToKeyBinding(event).ifPresent(binding -> {
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
                    codeArea.moveParagraphStart();
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
                    codeArea.moveParagraphEnd();
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
                    int pos = textPosToOffset(codeArea, codeArea.getCaretPosition());
                    String text = codeArea.getText();
                    codeArea.setText(text.substring(0, pos));
                    codeArea.select(offsetToTextPos(codeArea, pos));
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
            }
        });
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

    /// Converts a flat document offset (as used by {@link StringManipulator}) into a
    /// paragraph/column based {@link TextPos}, by walking the paragraphs of the model.
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

    /// Inverse of {@link #offsetToTextPos(CodeArea, int)}.
    private static int textPosToOffset(CodeArea codeArea, TextPos pos) {
        int offset = 0;
        for (int i = 0; i < pos.index(); i++) {
            offset += codeArea.getPlainText(i).length() + 1;
        }
        return offset + pos.offset();
    }
}
