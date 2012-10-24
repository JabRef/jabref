/*  Copyright (C) 2003-2012 JabRef contributors.
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
package net.sf.jabref.gui;

import javax.swing.text.JTextComponent;
import javax.swing.text.BadLocationException;

import net.sf.jabref.autocompleter.AbstractAutoCompleter;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.FocusListener;
import java.awt.event.FocusEvent;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Morten O. Alver, 16 Feb. 2007
 */
public class AutoCompleteListener extends KeyAdapter implements FocusListener {

	private static Logger logger = Logger.getLogger(AutoCompleteListener.class.getName());

    AbstractAutoCompleter completer;

    // These variables keep track of the situation from time to time.
    protected String toSetIn = null; // null indicates that there are no completions available
    protected String lastBeginning = null; // the letters, the user has typed until know
    protected int lastCaretPosition = -1;
    protected String[] lastCompletions = null;
    protected int lastShownCompletion = 0;
    protected boolean consumeEnterKey = true;

    // This field is set if the focus listener should call another focus listener
    // after finishing. This is needed because the autocomplete listener must
    // run before the focus listener responsible for storing the current edit.
    protected FocusListener nextFocusListener = null;

    public AutoCompleteListener(AbstractAutoCompleter completer) {
//    	if (logger.getHandlers().length == 0) {
//	    	logger.setLevel(Level.FINEST);
//	    	ConsoleHandler ch = new ConsoleHandler();
//	    	ch.setLevel(Level.FINEST);
//	    	logger.addHandler(ch);
//    	}
        this.completer = completer;
    }

    /**
     * This method is used if the focus listener should call another focus listener
     * after finishing. This is needed because the autocomplete listener must
     * run before the focus listener responsible for storing the current edit.
     *
     * @param listener The listener to call.
     */
    public void setNextFocusListener(FocusListener listener) {
        this.nextFocusListener = listener;
    }

    /**
     * This setting determines whether the autocomplete listener should consume the Enter key
     * stroke when it leads to accepting a completion. If set to false, the JTextComponent will receive
     * the Enter key press after the completion is done. The default value if true.
     * @param t true to indicate that the Enter key should be consumed, false that it should be forwarded
     */
    public void setConsumeEnterKey(boolean t) {
        this.consumeEnterKey = t;
    }

    public void keyPressed(KeyEvent e) {
    	if ((toSetIn != null) && (e.getKeyCode() == KeyEvent.VK_ENTER)) {
            JTextComponent comp = (JTextComponent) e.getSource();

            // replace typed characters by characters from completion
            lastBeginning = lastCompletions[lastShownCompletion];

            int end = comp.getSelectionEnd();
            comp.select(end, end);
            toSetIn = null;
            if (consumeEnterKey)
                e.consume();
            return;
        }
        // Cycle through alternative completions when user presses PGUP/PGDN:
        else if ((e.getKeyCode() == KeyEvent.VK_PAGE_DOWN) && (toSetIn != null)) {
            cycle((JTextComponent) e.getSource(), 1);
            e.consume();
        }
        else if ((e.getKeyCode() == KeyEvent.VK_PAGE_UP) && (toSetIn != null)) {
            cycle((JTextComponent) e.getSource(), -1);
            e.consume();
        }
//        else if ((e.getKeyCode() == KeyEvent.VK_BACK_SPACE)) {
//        	StringBuffer currentword = getCurrentWord((JTextComponent) e.getSource());
//        	// delete last char to obey semantics of back space
//        	currentword.deleteCharAt(currentword.length()-1);
//        	doCompletion(currentword, e);
//        }
        else if (e.getKeyChar() == KeyEvent.CHAR_UNDEFINED) {
        	if (e.getKeyCode() != KeyEvent.VK_SHIFT) {
        		// shift is OK, everyhting else leads to a reset
        		resetAutoCompletion();
        	} else {
            	logger.finest("special case: shift pressed. No action.");
        	}
        } else {
        	logger.finest("special case: defined character, but not caught above");
        }
    }
    
    private void cycle(JTextComponent comp, int increment) {
    	assert(lastCompletions != null);
    	assert(lastCompletions.length > 0);
        lastShownCompletion += increment;
        if (lastShownCompletion >= lastCompletions.length)
            lastShownCompletion = 0;
        else if (lastShownCompletion < 0)
            lastShownCompletion = lastCompletions.length-1;
        String sno = lastCompletions[lastShownCompletion];
        toSetIn = sno.substring(lastBeginning.length()-1);
        
        StringBuffer alltext = new StringBuffer(comp.getText());
        
        int oldSelectionStart = comp.getSelectionStart();
        int oldSelectionEnd = comp.getSelectionEnd();
        
        // replace prefix with new prefix
        int startPos = comp.getSelectionStart() - lastBeginning.length();
        alltext.delete(startPos, oldSelectionStart);
        alltext.insert(startPos, sno.subSequence(0, lastBeginning.length()));
        
        // replace suffix with new suffix
        int deletedChars = oldSelectionEnd - oldSelectionStart;
        alltext.delete(oldSelectionStart, oldSelectionEnd);
        //int cp = oldSelectionEnd - deletedChars;
        alltext.insert(oldSelectionStart, toSetIn.substring(1));
        
        //Util.pr(alltext.toString());
        comp.setText(alltext.toString());
        //comp.setCaretPosition(cp+toSetIn.length()-1);
        comp.select(oldSelectionStart, oldSelectionStart + toSetIn.length()-1);
        lastCaretPosition = comp.getCaretPosition();
        //System.out.println("ToSetIn: '"+toSetIn+"'");
    }
    
    /**
     * If user cancels autocompletion by
     *   a) entering another letter than the completed word (and there is no other auto completion)
     *   b) space
     * the casing of the letters has to be kept
     * 
     * Global variable "lastBeginning" keeps track of typed letters.
     * We rely on this variable to reconstruct the text 
     * 
     * @param wordSeperatorTyped indicates whether the user has typed a white space character or a
     */
    private void setUnmodifiedTypedLetters(JTextComponent comp, boolean lastBeginningContainsTypedCharacter, boolean wordSeperatorTyped) {
    	if (lastBeginning == null) {
    		logger.finest("no last beginning");
    		// There was no previous input (if the user typed a word, where no autocompletion is available)
    		// Thus, there is nothing to replace
    		return;
    	}
    	logger.finest("lastBeginning: >" + lastBeginning + "<");
    	if (comp.getSelectedText() == null) {
    		// if there is no selection
    		// the user has typed the complete word, but possibly with a different casing
    		// we need a replacement
    		if (wordSeperatorTyped) {
    			logger.finest("replacing complete word");
    		} else {
    			// if user did not press a white space character (space, ...),
    			// then we do not do anything
    			return;
    		}
    	} else {
	    	logger.finest("selected text " + comp.getSelectedText()+ " will be removed");
	        // remove completion suggestion
	        comp.replaceSelection("");
    	}
        
        lastCaretPosition = comp.getCaretPosition();
        
        int endIndex = lastCaretPosition - lastBeginning.length();
        if (lastBeginningContainsTypedCharacter) {
            // the current letter is NOT contained in comp.getText(), but in lastBeginning
        	// thus lastBeginning.length() is one too large
        	endIndex++;
        }
        String text = comp.getText();
        comp.setText(text.substring(0, endIndex).
        		concat(lastBeginning).
                concat(text.substring(lastCaretPosition)));
        if (lastBeginningContainsTypedCharacter) {
            // the current letter is NOT contained in comp.getText()
        	// Thus, cursor position also did not get updated
        	lastCaretPosition++;
        }
        comp.setCaretPosition(lastCaretPosition);
        lastBeginning = null;
    }
    
    /**
     * Start a new completion attempt
     * (instead of treating a continuation of an existing word or an interrupt of the current word)
     */
    private void startCompletion(StringBuffer currentword, KeyEvent e) {
    	JTextComponent comp = (JTextComponent) e.getSource();

    	String[] completed = findCompletions(currentword.toString(), comp);
        String prefix = completer.getPrefix();
        String cWord = (prefix != null) && (prefix.length() > 0) ?
                currentword.toString().substring(prefix.length()) : currentword.toString();
    	if (logger.isLoggable(Level.FINEST)) {
    		logger.finest("startCompletion");
    		logger.finest("currentword: >" + currentword + "<");
    		logger.finest("prefix: >" + prefix + "<");
    		logger.finest("cword: >" + cWord + "<");
    	}
        int no = 0; // We use the first word in the array of completions.
        if ((completed != null) && (completed.length > 0)) {
            lastShownCompletion = 0;
            lastCompletions = completed;
            String sno = completed[no];
            
            // these two lines obey the user's input
            //toSetIn = Character.toString(ch);
            //toSetIn = toSetIn.concat(sno.substring(cWord.length()));
            // BUT we obey the completion
            toSetIn = sno.substring(cWord.length() - 1);
        	if (logger.isLoggable(Level.FINEST)) {
        		logger.finest("toSetIn: >" + toSetIn + "<");
        	}
        	
            StringBuffer alltext = new StringBuffer(comp.getText());
            int cp = comp.getCaretPosition();
            alltext.insert(cp, toSetIn);
            comp.setText(alltext.toString());
            comp.setCaretPosition(cp);
            comp.select(cp + 1, cp + 1 + sno.length() - cWord.length());
            e.consume();
            lastCaretPosition = comp.getCaretPosition();
            char ch = e.getKeyChar();
            logger.finest("Appending >" + ch + "<");
            if (cWord.length()<=1) {
            	lastBeginning = Character.toString(ch);
            } else {
            	lastBeginning = cWord.substring(0, cWord.length()-1).concat(Character.toString(ch));
            }
            return;
        }

    }

    public void keyTyped(KeyEvent e) {
    	logger.finest("key typed event caught");
        char ch = e.getKeyChar();
    	if (ch == '\n')
    		// this case is handled at keyPressed(e)
    		return;
    	
        
        if ((e.getModifiers() | KeyEvent.SHIFT_MASK) == KeyEvent.SHIFT_MASK) {
        	// plain key or SHIFT + key is pressed, no handling of CTRL+key,  META+key, ...
        if (Character.isLetter(ch) || Character.isDigit(ch) || (Character.isWhitespace(ch) && completer.isSingleUnitField())) {
            JTextComponent comp = (JTextComponent) e.getSource();

            if (logger.isLoggable(Level.FINEST)) {
            	if (toSetIn == null)
            		logger.finest("toSetIn: NULL");            	
            	else
            		logger.finest("toSetIn: >" + toSetIn + "<");
        	}
            
        	// The case-insensitive system is a bit tricky here
        	// If keyword is "TODO" and user types "tO", then this is treated as "continue" as the "O" matches the "O"
        	// If keyword is "TODO" and user types "To", then this is treated as "discont" as the "o" does NOT match the "O".

            if ((toSetIn != null) && (toSetIn.length() > 1) &&
                    (ch == toSetIn.charAt(1))) {
                // User continues on the word that was suggested.
        		logger.finest("cont");
            	
                toSetIn = toSetIn.substring(1);
                if (toSetIn.length() > 0) {
                    int cp = comp.getCaretPosition();
                    //comp.setCaretPosition(cp+1-toSetIn.);
                    //System.out.println(cp-toSetIn.length()+" - "+cp);
                    comp.select(cp + 1 - toSetIn.length(), cp);
                    lastBeginning = lastBeginning + ch;

                    e.consume();
                    lastCaretPosition = comp.getCaretPosition();

                    //System.out.println("Added char: '"+toSetIn+"'");
                    //System.out.println("LastBeginning: '"+lastBeginning+"'");

                    lastCompletions = findCompletions(lastBeginning, comp);
                    lastShownCompletion = 0;
                    for (int i = 0; i < lastCompletions.length; i++) {
                        String lastCompletion = lastCompletions[i];
                        //System.out.println("Completion["+i+"] = "+lastCompletion);
                        if (lastCompletion.endsWith(toSetIn)) {
                            lastShownCompletion = i;
                            break;
                        }

                    }
                    //System.out.println("Index now: "+lastShownCompletion);
                    if (toSetIn.length() < 2) {
                    	// User typed the last character of the autocompleted word
                    	// We have to replace the automcompletion word by the typed word.
                    	// This helps if the user presses "space" after the completion
                    	// "space" indicates that the user does NOT want the autocompletion,
                    	// but the typed word
                        String text = comp.getText();
                        comp.setText(text.substring(0, lastCaretPosition - lastBeginning.length())
                                + lastBeginning
                                + text.substring(lastCaretPosition));
                        // there is no selected text, therefore we are not updating the selection
                    	toSetIn = null;
                    }
                    return;
                }
            }

            if ((toSetIn != null) && ((toSetIn.length() <= 1) ||
                    (ch != toSetIn.charAt(1)))) {
                // User discontinues the word that was suggested.
                lastBeginning = lastBeginning + ch;
            	if (logger.isLoggable(Level.FINEST)) {
            		logger.finest("discont");
            		logger.finest("toSetIn: >" + toSetIn + "<");
            		logger.finest("lastBeginning: >" + lastBeginning +"<");
            	}
                String[] completed = findCompletions(lastBeginning, comp);
                if ((completed != null) && (completed.length > 0)) {
                    lastShownCompletion = 0;
                    lastCompletions = completed;
                    String sno = completed[0];
                    // toSetIn = string used for autocompletion last time
                    // this string has to be removed
                    // lastCaretPosition is the position of the caret after toSetIn.
                    int lastLen = toSetIn.length() - 1;
                    toSetIn = sno.substring(lastBeginning.length() - 1);
                    String text = comp.getText();
                    //Util.pr(""+lastLen);
                    //we do not use toSetIn as we want to obey the casing of "sno"
                    comp.setText(text.substring(0, lastCaretPosition - lastLen - lastBeginning.length()+1)
                            + sno
                            + text.substring(lastCaretPosition));
                    int startSelect = lastCaretPosition + 1 - lastLen;
                    int endSelect =  lastCaretPosition + toSetIn.length() - lastLen;
                    comp.select(startSelect, endSelect);

                    lastCaretPosition = comp.getCaretPosition();
                    e.consume();
                    return;
                } else {
                	setUnmodifiedTypedLetters(comp, true, false);
                	e.consume();
                    toSetIn = null;
                    return;
                }
            }

    		logger.finest("case else");

            comp.replaceSelection("");

            StringBuffer currentword = getCurrentWord(comp);
            if (currentword == null)
                currentword = new StringBuffer();
            
            // only "real characters" end up here
            assert(!Character.isISOControl(ch));
            currentword.append(ch);
            startCompletion(currentword, e);
            return;
        } else {
            if (Character.isWhitespace(ch) ) {
            	assert(!completer.isSingleUnitField());
        		logger.finest("whitespace && !singleUnitField");
            	// start a new search if end-of-field is reached

            	// replace displayed letters with typed letters
            	setUnmodifiedTypedLetters((JTextComponent) e.getSource(), false, true);
            	resetAutoCompletion();
                return;
            }
            
        	logger.finest("No letter/digit/whitespace or CHAR_UNDEFINED");
        	// replace displayed letters with typed letters 
        	setUnmodifiedTypedLetters((JTextComponent) e.getSource(), false, !Character.isISOControl(ch));
        	resetAutoCompletion();
        	return;
        }
        }
        resetAutoCompletion();
    }

    /**
     * Resets the auto completion data in a way that no leftovers are there
     */
    private void resetAutoCompletion() {
    	logger.finest("Resetting autocompletion");
        toSetIn = null;
        lastBeginning = null;
    }

	protected String[] findCompletions(String beginning, JTextComponent comp) {
        return completer.complete(beginning);
    }

    protected StringBuffer getCurrentWord(JTextComponent comp) {
        StringBuffer res = new StringBuffer();
        String upToCaret;

        try {
            upToCaret = comp.getText(0, comp.getCaretPosition());
            // We now have the text from the start of the field up to the caret position.
            // In most fields, we are only interested in the currently edited word, so we
            // seek from the caret backward to the closest space:
            if (!completer.isSingleUnitField()) {
                if ((comp.getCaretPosition() < comp.getText().length())
                        && Character.isWhitespace(comp.getText().charAt(comp.getCaretPosition()))) {
                    // caret is in the middle of the text AND current character is a whitespace
                	// that means: a new word is started and there is no current word
                    return null;
                }
            	
                int piv = upToCaret.length() - 1;
                while ((piv >= 0) && !Character.isWhitespace(upToCaret.charAt(piv))) {
                    piv--;
                }
                // priv points to whitespace char or priv is -1
                // copy everything from the next char up to the end of "upToCaret" 
                res.append(upToCaret.substring(piv+1));
            } else {
	            // For fields such as "journal" it is more reasonable to try to complete on the entire
	            // text field content, so we skip the searching and keep the entire part up to the caret:
	            res.append(upToCaret);
            }
            //Util.pr("AutoCompListener: "+res.toString());
        } catch (BadLocationException ex) {
        }

        return res;
    }

    final static int ANY_NAME = 0, FIRST_NAME = 1, LAST_NAME = 2;
    protected int findNamePositionStatus(JTextComponent comp) {
        String upToCaret;
        try {
            upToCaret = comp.getText(0, comp.getCaretPosition());
            // Clip off evertyhing up to and including the last " and " before:
            upToCaret = upToCaret.substring(upToCaret.lastIndexOf(" and ")+1);
            int commaIndex = upToCaret.indexOf(',');
            if (commaIndex < 0)
                return ANY_NAME;
            else
                return FIRST_NAME;
            
        } catch (BadLocationException ex) {
            return ANY_NAME;
        }

    }

    public void focusGained(FocusEvent event) {
        if (nextFocusListener != null)
            nextFocusListener.focusGained(event);
    }

    public void focusLost(FocusEvent event) {
        if (toSetIn != null) {
            JTextComponent comp = (JTextComponent)event.getSource();
            clearCurrentSuggestion(comp);
        }
        if (nextFocusListener != null)
            nextFocusListener.focusLost(event);
    }

    public void clearCurrentSuggestion(JTextComponent comp) {
         if (toSetIn != null) {
            int selStart = comp.getSelectionStart();
            String text = comp.getText();
            comp.setText(text.substring(0, selStart) + text.substring(comp.getSelectionEnd()));
            comp.setCaretPosition(selStart);
            lastCompletions = null;
            lastShownCompletion = 0;
            lastCaretPosition = -1;
            toSetIn = null;
        }
    }
}
