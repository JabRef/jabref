package org.jabref.gui.keyboard;

import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyEvent;

import org.jabref.logic.util.strings.StringManipulator;
import org.jabref.model.util.ResultingStringState;

public class TextInputKeyBindings {

    public static void call(Scene scene, KeyEvent event, KeyBindingRepository keyBindingRepository) {
        if (scene.focusOwnerProperty().get() instanceof TextInputControl focusedTextField) {
            keyBindingRepository.mapToKeyBinding(event).ifPresent(binding -> {
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
                        if (focusedTextField instanceof TextArea) {
                            int caret = focusedTextField.getCaretPosition();
                            String text = focusedTextField.getText();
                            int lineStart = text.lastIndexOf('\n', caret - 1) + 1;
                            focusedTextField.positionCaret(lineStart);
                        } else {
                            focusedTextField.home();
                        }
                        event.consume();
                    }
                    case EDITOR_END -> {
                        if (focusedTextField instanceof TextArea) {
                            int caret = focusedTextField.getCaretPosition();
                            String text = focusedTextField.getText();
                            int lineEnd = text.indexOf('\n', caret);
                            int targetPosition = lineEnd == -1 ? text.length() : lineEnd;
                            focusedTextField.positionCaret(targetPosition);
                        } else {
                            focusedTextField.end();
                        }
                        event.consume();
                    }
                    case EDITOR_BEGINNING_DOC -> {
                        focusedTextField.positionCaret(0);
                        event.consume();
                    }
                    case EDITOR_END_DOC -> {
                        focusedTextField.positionCaret(focusedTextField.getLength());
                        event.consume();
                    }
                    case EDITOR_UP -> {
                        focusedTextField.home();
                        event.consume();
                    }
                    case EDITOR_DOWN -> {
                        focusedTextField.end();
                        event.consume();
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
                        int caret = focusedTextField.getCaretPosition();
                        String text = focusedTextField.getText();

                        if (focusedTextField instanceof TextArea) {
                            int lineEnd = text.indexOf('\n', caret);
                            if (lineEnd == -1) {
                                lineEnd = text.length();
                            }

                            if (caret < lineEnd) {
                                focusedTextField.deleteText(caret, lineEnd);
                            } else if (lineEnd < text.length()) {
                                focusedTextField.deleteText(lineEnd, lineEnd + 1);
                            }
                        } else {
                            focusedTextField.deleteText(caret, text.length());
                        }

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
}
