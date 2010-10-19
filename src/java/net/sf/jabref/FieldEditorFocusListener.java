package net.sf.jabref;

import javax.swing.*;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;


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
        if (event.getSource() instanceof FieldEditor)
            ((FieldEditor)event.getSource()).setActiveBackgroundColor();
        else
            ((JComponent)event.getSource()).setBackground(GUIGlobals.activeBackground);
    }


    public void focusLost(FocusEvent event) {
        if (event.getSource() instanceof FieldEditor)
            ((FieldEditor)event.getSource()).setValidBackgroundColor();
        else
            ((JComponent)event.getSource()).setBackground(GUIGlobals.validFieldBackgroundColor);
    }

}
