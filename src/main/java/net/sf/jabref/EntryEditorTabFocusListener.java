package net.sf.jabref;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/*
 * Focus listener that fires the storeFieldAction when a FieldTextArea loses
 * focus.
 */
class EntryEditorTabFocusListener implements FocusListener {

    JTextComponent c;

    DocumentListener d;
    private EntryEditorTab entryEditorTab;

    public EntryEditorTabFocusListener(final EntryEditorTab entryEditorTab) {
        this.entryEditorTab = entryEditorTab;
    }

    public void focusGained(FocusEvent e) {

        synchronized (this){
            if (c != null) {
                c.getDocument().removeDocumentListener(d);
                c = null;
                d = null;
            }

            if (e.getSource() instanceof JTextComponent) {

                c = (JTextComponent) e.getSource();
                /**
                 * [ 1553552 ] Not properly detecting changes to flag as
                 * changed
                 */
                d = new DocumentListener() {

                    void fire() {
                        if (c.isFocusOwner()) {
                            entryEditorTab.markIfModified((FieldEditor) c);
                        }
                    }

                    public void changedUpdate(DocumentEvent e) {
                        fire();
                    }

                    public void insertUpdate(DocumentEvent e) {
                        fire();
                    }

                    public void removeUpdate(DocumentEvent e) {
                        fire();
                    }
                };
                c.getDocument().addDocumentListener(d);

                /**
                 * Makes the vertical scroll panel view follow the focus
                 */
                Component cScrollPane = c.getParent().getParent();
                if (cScrollPane instanceof JScrollPane) {
                    JScrollPane componentPane = (JScrollPane) cScrollPane;
                    Component cPane = componentPane.getParent();
                    if (cPane instanceof JPanel) {
                        JPanel panel = (JPanel) cPane;
                        Rectangle bounds = componentPane.getBounds();
                        panel.scrollRectToVisible(bounds);
                    }
                }

            }
        }

        entryEditorTab.setActive((FieldEditor) e.getSource());

    }

    public void focusLost(FocusEvent e) {
synchronized (this) {
            if (c != null) {
                c.getDocument().removeDocumentListener(d);
                c = null;
                d = null;
            }
        }
        if (!e.isTemporary())
            entryEditorTab.parent.updateField(e.getSource());
    }
}
