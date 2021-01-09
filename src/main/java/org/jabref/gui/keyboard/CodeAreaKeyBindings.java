package org.jabref.gui.keyboard;

import javafx.scene.input.KeyEvent;

import org.jabref.logic.util.strings.StringManipulator;
import org.jabref.model.util.ResultingStringState;

import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.NavigationActions;

public class CodeAreaKeyBindings {

    public static void call(CodeArea codeArea, KeyEvent event, KeyBindingRepository keyBindingRepository) {
        keyBindingRepository.mapToKeyBinding(event).ifPresent(binding -> {
            switch (binding) {
                case EDITOR_DELETE -> {
                    codeArea.deleteNextChar();
                    event.consume();
                }
                case EDITOR_BACKWARD -> {
                    codeArea.previousChar(NavigationActions.SelectionPolicy.CLEAR);
                    event.consume();
                }
                case EDITOR_FORWARD -> {
                    codeArea.nextChar(NavigationActions.SelectionPolicy.CLEAR);
                    event.consume();
                }
                case EDITOR_WORD_BACKWARD -> {
                    codeArea.wordBreaksBackwards(2, NavigationActions.SelectionPolicy.CLEAR);
                    event.consume();
                }
                case EDITOR_WORD_FORWARD -> {
                    codeArea.wordBreaksForwards(2, NavigationActions.SelectionPolicy.CLEAR);
                    event.consume();
                }
                case EDITOR_BEGINNING_DOC -> {
                    codeArea.start(NavigationActions.SelectionPolicy.CLEAR);
                    event.consume();
                }
                case EDITOR_UP -> {
                    codeArea.paragraphStart(NavigationActions.SelectionPolicy.CLEAR);
                    event.consume();
                }
                case EDITOR_BEGINNING -> {
                    codeArea.lineStart(NavigationActions.SelectionPolicy.CLEAR);
                    event.consume();
                }
                case EDITOR_END_DOC -> {
                    codeArea.end(NavigationActions.SelectionPolicy.CLEAR);
                    event.consume();
                }
                case EDITOR_DOWN -> {
                    codeArea.paragraphEnd(NavigationActions.SelectionPolicy.CLEAR);
                    event.consume();
                }
                case EDITOR_END -> {
                    codeArea.lineEnd(NavigationActions.SelectionPolicy.CLEAR);
                    event.consume();
                }
                case EDITOR_CAPITALIZE -> {
                    int pos = codeArea.getCaretPosition();
                    String text = codeArea.getText(0, codeArea.getText().length());
                    ResultingStringState res = StringManipulator.capitalize(pos, text);
                    codeArea.replaceText(res.text);
                    codeArea.displaceCaret(res.caretPosition);
                    event.consume();
                }
                case EDITOR_LOWERCASE -> {
                    int pos = codeArea.getCaretPosition();
                    String text = codeArea.getText(0, codeArea.getText().length());
                    ResultingStringState res = StringManipulator.lowercase(pos, text);
                    codeArea.replaceText(res.text);
                    codeArea.displaceCaret(res.caretPosition);
                    event.consume();
                }
                case EDITOR_UPPERCASE -> {
                    int pos = codeArea.getCaretPosition();
                    String text = codeArea.getText(0, codeArea.getText().length());
                    ResultingStringState res = StringManipulator.uppercase(pos, text);
                    codeArea.clear();
                    codeArea.replaceText(res.text);
                    codeArea.displaceCaret(res.caretPosition);
                    event.consume();
                }
                case EDITOR_KILL_LINE -> {
                    int pos = codeArea.getCaretPosition();
                    codeArea.replaceText(codeArea.getText(0, pos));
                    codeArea.displaceCaret(pos);
                    event.consume();
                }
                case EDITOR_KILL_WORD -> {
                    int pos = codeArea.getCaretPosition();
                    String text = codeArea.getText(0, codeArea.getText().length());
                    ResultingStringState res = StringManipulator.killWord(pos, text);
                    codeArea.replaceText(res.text);
                    codeArea.displaceCaret(res.caretPosition);
                    event.consume();
                }
                case EDITOR_KILL_WORD_BACKWARD -> {
                    int pos = codeArea.getCaretPosition();
                    String text = codeArea.getText(0, codeArea.getText().length());
                    ResultingStringState res = StringManipulator.backwardKillWord(pos, text);
                    codeArea.replaceText(res.text);
                    codeArea.displaceCaret(res.caretPosition);
                    event.consume();
                }
            }
        });
    }
}

