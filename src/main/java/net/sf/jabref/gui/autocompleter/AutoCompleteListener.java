/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.gui.autocompleter;

import javax.swing.text.JTextComponent;
import javax.swing.text.BadLocationException;

import net.sf.jabref.logic.autocompleter.AutoCompleter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.awt.event.*;
import java.util.List;
import java.util.Optional;

public class AutoCompleteListener extends KeyAdapter implements FocusListener {
    private static final Log LOGGER = LogFactory.getLog(AutoCompleteListener.class);

    private final AutoCompleter<String> completer;
    private Optional<AutoCompleteState> currentCompletion;
    private FocusListener nextFocusListener;

    public AutoCompleteListener(AutoCompleter<String> completer) {
        this.completer = completer;
        currentCompletion = Optional.empty();
    }

    /**
     * This method is used if the focus listener should call another focus listener after finishing. This is needed
     * because the autocomplete listener must run before the focus listener responsible for storing the current edit.
     *
     * @param listener The listener to call.
     */
    public void setNextFocusListener(FocusListener listener) {
        nextFocusListener = listener;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // no auto completions available
        if (!currentCompletion.isPresent()) {
            return;
        }

        AutoCompleteState completion = currentCompletion.get();
        int keyCode = e.getKeyCode();
        JTextComponent textField = (JTextComponent) e.getSource();

        if (keyCode == KeyEvent.VK_ENTER) {
            // ENTER inserts auto-completion
            insertSuggestion(textField, e);
            // reset state
            clearAutoCompletion(textField);
        } else if (keyCode == KeyEvent.VK_PAGE_DOWN) {
            // Cycle through alternative completions
            completion.previousSuggestion();
            insertSuggestion(textField, e);
        } else if (keyCode == KeyEvent.VK_PAGE_UP) {
            // Cycle through alternative completions
            completion.nextSuggestion();
            insertSuggestion(textField, e);
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        char newChar = e.getKeyChar();
        JTextComponent textField = (JTextComponent) e.getSource();

        // this case is handled at keyPressed(e)
        if (newChar == KeyEvent.VK_ENTER) {
            return;
        }

        if ((e.getModifiers() | InputEvent.SHIFT_MASK) != InputEvent.SHIFT_MASK) {
            // TODO: plain key or SHIFT + key is pressed, no handling of CTRL+key,  META+key, ...
            return;
        }

        // don't do auto completion inside words
        if (!atEndOfWord(textField)) {
            return;
        }

        if (Character.isLetter(newChar) || Character.isDigit(newChar) || Character.isWhitespace(newChar)) {
            // User continues on the word that was suggested.
            if (currentCompletion.isPresent() && currentCompletion.get().continuesSuggestion(newChar)) {
                // update currentWord inside auto-completion
                currentCompletion.get().updateState(newChar);
                // update highlighting
                insertKeyAndSuggestion(textField, e);
            } else {
                // start new completion attempt
                // delete selection if auto-completion is active
                if (currentCompletion.isPresent()) {
                    textField.replaceSelection("");
                }
                String word = getCurrentWord(textField).toString() + newChar;
                startCompletion(word, textField);
                insertKeyAndSuggestion(textField, e);
            }
        } else {
            // Some other key like backspace has been entered, state may have changed
            clearAutoCompletion(textField);
        }
    }

    private void startCompletion(String currentWord, JTextComponent textField) {
        // reset state
        clearAutoCompletion(textField);
        // try to find suggestions for current word
        List<String> suggestions = completer.complete(currentWord);

        if (suggestions == null || suggestions.isEmpty()) {
            return;
        }
        currentCompletion = Optional.of(new AutoCompleteState(currentWord, suggestions, textField.getCaretPosition()));
    }

    // DANGER: typed key will also be inserted here!
    private void insertKeyAndSuggestion(JTextComponent textField, KeyEvent e) {
        if (!currentCompletion.isPresent()) {
            return;
        }

        AutoCompleteState completion = currentCompletion.get();

        String suggestedWord = completion.getSuggestedWord();
        // remove old suggestion
        textField.replaceSelection("");
        StringBuilder fieldText = new StringBuilder(textField.getText());
        // insert suggested substring
        int caretPosition = currentCompletion.get().getRealCaretPosition();
        // DANGER: typed key will also be inserted here!
        String keyAndWord = completion.getSuggestedWord().substring(completion.getCurrentWord().length() - 1);
        fieldText.insert(caretPosition, keyAndWord);
        textField.setText(fieldText.toString());
        // highlight suggested substring
        textField.select(caretPosition + 1, (caretPosition + 1 + suggestedWord.length()) - completion.getCurrentWord().length());
        // prevent insertion of typed key
        e.consume();
    }

    private void insertSuggestion(JTextComponent textField, KeyEvent e) {
        if (!currentCompletion.isPresent()) {
            return;
        }

        AutoCompleteState completion = currentCompletion.get();

        String suggestedWord = completion.getSuggestedWord();
        // remove old suggestion
        textField.replaceSelection("");
        StringBuilder fieldText = new StringBuilder(textField.getText());
        // insert suggested substring
        int caretPosition = currentCompletion.get().getRealCaretPosition();
        String word = completion.getSuggestedWord().substring(completion.getCurrentWord().length());
        fieldText.insert(caretPosition + 1, word);
        textField.setText(fieldText.toString());
        // highlight suggested substring
        textField.select(caretPosition + 1, (caretPosition + 1 + suggestedWord.length()) - completion.getCurrentWord().length());
        // prevent usual action of ENTER/PGUP/PGDN
        e.consume();
    }

    private boolean atEndOfWord(JTextComponent textField) {
        int nextCharPosition = textField.getCaretPosition();

        // position not at the end of input
        if(nextCharPosition < textField.getText().length()) {
            char nextChar = textField.getText().charAt(nextCharPosition);
            if (!Character.isWhitespace(nextChar)) {
                return false;
            }
        }
        return true;
    }

    private StringBuffer getCurrentWord(JTextComponent textField) {
        final int caretPosition = textField.getCaretPosition();

        StringBuffer result = new StringBuffer();

        try {
            // text from the start of the field up to the current caret/cursor position
            String textBeforeCaret = textField.getText(0, caretPosition);
            // In most fields, we are only interested in the currently edited word, so we
            // seek from the textBeforeCaret backward to the closest space:
            if (!completer.isSingleUnitField()) {
                // textBeforeCaret is in the middle of the text AND last character is a whitespace
                // that means: a new word is started and there is no current word
                if (caretPosition < textField.getText().length() && Character.isWhitespace(textField.getText().charAt(caretPosition - 1))) {
                    return new StringBuffer(0);
                }

                // find word
                int piv = textBeforeCaret.length() - 1;
                while ((piv >= 0) && !Character.isWhitespace(textBeforeCaret.charAt(piv))) {
                    piv--;
                }
                // piv points to whitespace char or piv is -1
                // copy everything from the next char up to the end of "caretPosition"
                result.append(textBeforeCaret.substring(piv + 1));
            } else {
                // For fields such as "journal" it is more reasonable to try to complete on the entire
                // text field content, so we skip the searching and keep the entire part up to the textBeforeCaret:
                result.append(textBeforeCaret);
            }
        } catch (BadLocationException ignore) {
            // potentially thrown by textField.getText()
        }

        return result;
    }

    @Override
    public void focusGained(FocusEvent event) {
        if (nextFocusListener != null) {
            nextFocusListener.focusGained(event);
        }
    }

    @Override
    public void focusLost(FocusEvent event) {
        if (currentCompletion.isPresent()) {
            // comment this when debugging!
            clearAutoCompletion((JTextComponent) event.getSource());
        }
        if (nextFocusListener != null) {
            nextFocusListener.focusLost(event);
        }
    }

    public void clearAutoCompletion(JTextComponent textField) {
        if (currentCompletion.isPresent()) {
            textField.replaceSelection("");
            textField.setCaretPosition(textField.getSelectionStart());
            currentCompletion = Optional.empty();
        }
    }
}
