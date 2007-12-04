package net.sf.jabref.gui;

import javax.swing.text.JTextComponent;
import javax.swing.text.BadLocationException;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Created by Morten O. Alver, 16 Feb. 2007
 */
public class AutoCompleteListener extends KeyAdapter {


    AutoCompleter completer;
    protected String toSetIn = null,
            lastBeginning = null;
    protected int lastCaretPosition = -1;
    protected Object[] lastCompletions = null;
    protected int lastShownCompletion = 0;

    // These variables keep track of the situation from time to time.

    public AutoCompleteListener(AutoCompleter completer) {
        this.completer = completer;
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
        toSetIn = sno.substring(lastBeginning.length());
        StringBuffer alltext = new StringBuffer(comp.getText());
        int deletedChars = comp.getSelectionEnd() - comp.getSelectionStart();
        alltext.delete(comp.getSelectionStart(), comp.getSelectionEnd());
        int cp = comp.getCaretPosition() - deletedChars;
        alltext.insert(cp, toSetIn);
        //Util.pr(alltext.toString());
        comp.setText(alltext.toString());
        comp.setCaretPosition(cp+toSetIn.length());
        comp.select(cp, cp + sno.length() - lastBeginning.length());
        lastCaretPosition = comp.getCaretPosition();
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
                    //Util.pr(cp-toSetIn.length()+" - "+cp);
                    comp.select(cp + 1 - toSetIn.length(), cp);
                    lastBeginning = lastBeginning + ch;

                    e.consume();
                    lastCaretPosition = comp.getCaretPosition();

                    //Util.pr("'"+toSetIn+"'");

                    if (toSetIn.length() < 2)
                        toSetIn = null;
                    return;
                }
            }

            if ((toSetIn != null) && ((toSetIn.length() <= 1) ||
                    (ch != toSetIn.charAt(1)))) {
                // User discontinues the word that was suggested.
                lastBeginning = lastBeginning + ch;
                Object[] completed =
                        completer.complete(lastBeginning);
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
            Object[] completed = completer.complete(currentword.toString());
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



    protected StringBuffer getCurrentWord(JTextComponent comp) {
        StringBuffer res = new StringBuffer();
        String upToCaret;

        try {
            upToCaret = comp.getText(0, comp.getCaretPosition());
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
            //Util.pr("AutoCompListener: "+res.toString());
        } catch (BadLocationException ex) {
        }

        return res;
    }
}
