package org.jabref.gui.fieldeditors;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JComponent;

import org.jabref.gui.GUIGlobals;

/**
 * Focus listener that changes the color of the text area when it has focus.
 */
public class FieldEditorFocusListener implements FocusListener {
    @Override
    public void focusGained(FocusEvent event) {
        if (event.getSource() instanceof FieldEditor) {
            ((FieldEditor) event.getSource()).setActiveBackgroundColor();
        } else {
            ((JComponent) event.getSource()).setBackground(GUIGlobals.activeBackgroundColor);
        }
    }

    @Override
    public void focusLost(FocusEvent event) {
        if (!(event.getSource() instanceof FieldEditor)) {
            ((JComponent) event.getSource()).setBackground(GUIGlobals.validFieldBackgroundColor);
        }
    }
}
