package net.sf.jabref.gui;

import javax.swing.text.JTextComponent;
import javax.swing.text.BadLocationException;

import net.sf.jabref.autocompleter.AbstractAutoCompleter;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.FocusListener;
import java.awt.event.FocusEvent;

/**
 * Created by Morten O. Alver, 16 Feb. 2007
 */
public class AutoCompleteListener extends KeyAdapter implements FocusListener {


    AbstractAutoCompleter completer;
    protected String toSetIn = null,
            lastBeginning = null;
    protected int lastCaretPosition = -1;
    protected Object[] lastCompletions = null;
    protected int lastShownCompletion = 0;

    // This field is set if the focus listener should call another focus listener
    // after finishing. This is needed because the autocomplete listener must
    // run before the focus listener responsible for storing the current edit.
    protected FocusListener nextFocusListener = null;

    // These variables keep track of the situation from time to time.

    public AutoCompleteListener(AbstractAutoCompleter completer) {
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

    public void keyPressed(KeyEvent e) {
    	if ((toSetIn != null) && (e.getKeyCode() == KeyEvent.VK_ENTER)) {
            JTextComponent comp = (JTextComponent) e.getSource();
            int end = comp.getSelectionEnd();
            comp.select(end, end);
            e.consume();
            return;
        }
        // Cycle through alternative completions when user presses PGUP/PGDN:
        else if ((e.getKeyCode() == KeyEvent.VK_PAGE_DOWN) && (lastCompletions != null)) {
            cycle((JTextComponent) e.getSource(), 1);
            e.consume();
        }
        else if ((e.getKeyCode() == KeyEvent.VK_PAGE_UP) && (lastCompletions != null)) {
            cycle((JTextComponent) e.getSource(), -1);
            e.consume();
        }
    }

    private void cycle(JTextComponent comp, int increment) {
        lastShownCompletion += increment;
        if (lastShownCompletion >= lastCompletions.length)
            lastShownCompletion = 0;
        else if (lastShownCompletion < 0)
            lastShownCompletion = lastCompletions.length-1;
        String sno = (String)(lastCompletions[lastShownCompletion]);
        toSetIn = sno.substring(lastBeginning.length()-1);
        StringBuffer alltext = new StringBuffer(comp.getText());
        int deletedChars = comp.getSelectionEnd() - comp.getSelectionStart();
        alltext.delete(comp.getSelectionStart(), comp.getSelectionEnd());
        int cp = comp.getCaretPosition() - deletedChars;
        alltext.insert(cp, toSetIn.substring(1));
        //Util.pr(alltext.toString());
        comp.setText(alltext.toString());
        comp.setCaretPosition(cp+toSetIn.length()-1);
        comp.select(cp, cp + sno.length() - lastBeginning.length());
        lastCaretPosition = comp.getCaretPosition();
        //System.out.println("ToSetIn: '"+toSetIn+"'");
    }

    public void keyTyped(KeyEvent e) {        
        char ch = e.getKeyChar();
        if (Character.isLetter(ch)) {
            JTextComponent comp = (JTextComponent) e.getSource();
            
            if ((toSetIn != null) && (toSetIn.length() > 1) &&
                    (ch == toSetIn.charAt(1))) {
                // User continues on the word that was suggested.
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
                        Object lastCompletion = lastCompletions[i];
                        //System.out.println("Completion["+i+"] = "+lastCompletion);
                        if (((String)lastCompletion).endsWith(toSetIn)) {
                            lastShownCompletion = i;
                            break;
                        }

                    }
                    //System.out.println("Index now: "+lastShownCompletion);
                    if (toSetIn.length() < 2)
                        toSetIn = null;
                    return;
                }
            }

            if ((toSetIn != null) && ((toSetIn.length() <= 1) ||
                    (ch != toSetIn.charAt(1)))) {
                // User discontinues the word that was suggested.
                lastBeginning = lastBeginning + ch;
                Object[] completed = findCompletions(lastBeginning, comp);
                if ((completed != null) && (completed.length > 0)) {
                    lastShownCompletion = 0;
                    lastCompletions = completed;
                    String sno = (String) (completed[0]);
                    int lastLen = toSetIn.length() - 1;
                    toSetIn = sno.substring(lastBeginning.length() - 1);
                    String text = comp.getText();
                    //Util.pr(""+lastLen);
                    comp.setText(text.substring(0, lastCaretPosition - lastLen)
                            + toSetIn
                            + text.substring(lastCaretPosition));
                    comp.select(lastCaretPosition + 1 - lastLen,
                            lastCaretPosition + toSetIn.length() - lastLen);

                    lastCaretPosition = comp.getCaretPosition();
                    e.consume();
                    return;
                } else {
                    toSetIn = null;
                    return;
                }
            }


            StringBuffer currentword = getCurrentWord(comp);
            if (currentword == null)
                return;
            currentword.append(ch);

            Object[] completed = findCompletions(currentword.toString(), comp);

            int no = 0; // We use the first word in the array of completions.
            if ((completed != null) && (completed.length > 0)) {
                lastShownCompletion = 0;
                lastCompletions = completed;
                String sno = (String) (completed[no]);
                toSetIn = sno.substring(currentword.length() - 1);
                //Util.pr("AutoCompListener: Found "+completed[0]);
                StringBuffer alltext = new StringBuffer(comp.getText());
                int cp = comp.getCaretPosition();
                alltext.insert(cp, toSetIn);
                //Util.pr(alltext.toString());
                comp.setText(alltext.toString());
                comp.setCaretPosition(cp);
                comp.select(cp + 1, cp + 1 + sno.length() - currentword.length());
                e.consume();
                lastCaretPosition = comp.getCaretPosition();
                lastBeginning = currentword.toString();
                return;
            }
        }
        //Util.pr("#hm");
        toSetIn = null;
        lastCompletions = null;

    }

    protected Object[] findCompletions(String beginning, JTextComponent comp) {        
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
                        && !Character.isWhitespace(comp.getText().charAt(comp.getCaretPosition())))
                    return null;
                boolean found = false;
                int piv = upToCaret.length() - 1;
                while (!found && (piv >= 0)) {
                    if (Character.isWhitespace(upToCaret.charAt(piv)))
                        found = true;
                    else piv--;
                }
                //if (piv < 0)
                //piv = 0;
                res.append(upToCaret.substring(piv + 1));
            }
            // For fields such as "journal" it is more reasonable to try to complete on the entire
            // text field content, so we skip the searching and keep the entire part up to the caret:
            else res.append(upToCaret);
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
        if (lastCompletions != null) {
            JTextComponent comp = (JTextComponent)event.getSource();
            clearCurrentSuggestion(comp);
        }
        if (nextFocusListener != null)
            nextFocusListener.focusLost(event);
    }

    public void clearCurrentSuggestion(JTextComponent comp) {
         if (lastCompletions != null) {
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
