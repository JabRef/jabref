package net.sf.jabref;

import java.awt.event.FocusListener;
import java.awt.event.FocusEvent;
import java.awt.*;


/**
 * Focus listener that changes the color of the text area when it has focus.
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: 18.mar.2005
 * Time: 18:20:14
 * To change this template use File | Settings | File Templates.
 */
public class FieldEditorFocusListener implements FocusListener {

    public FieldEditorFocusListener() {
    }

    public void focusGained(FocusEvent event) {
        ((Component)event.getSource()).setBackground(GUIGlobals.activeEditor);
    }


    public void focusLost(FocusEvent event) {
        ((Component)event.getSource()).setBackground(Color.white);
    }

}
