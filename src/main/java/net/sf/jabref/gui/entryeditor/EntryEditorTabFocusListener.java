package net.sf.jabref.gui.entryeditor;

import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

import net.sf.jabref.gui.fieldeditors.FieldEditor;


/**
 * Focus listener that fires the storeFieldAction when a TextArea loses focus.
 */
class EntryEditorTabFocusListener implements FocusListener {

    /** The component this DocumentListener is currently tied to */
    private JTextComponent textComponent;

    /** The listener which gets tied to each TextComponent (and removed) */
    private DocumentListener documentListener;

    /** The EntryEditorTab this FocusListener is currently tied to */
    private final EntryEditorTab entryEditorTab;


    public EntryEditorTabFocusListener(final EntryEditorTab entryEditorTab) {
        this.entryEditorTab = entryEditorTab;
    }

    @Override
    public void focusGained(FocusEvent event) {
        synchronized (this) {
            if (textComponent != null) {
                textComponent.getDocument().removeDocumentListener(documentListener);
                textComponent = null;
                documentListener = null;
            }

            if (event.getSource() instanceof JTextComponent) {
                textComponent = (JTextComponent) event.getSource();
                documentListener = new DocumentListener() {
                    private void fire() {
                        if (textComponent.isFocusOwner()) {
                            entryEditorTab.markIfModified((FieldEditor) textComponent);
                        }
                    }

                    @Override
                    public void changedUpdate(DocumentEvent e) {
                        fire();
                    }

                    @Override
                    public void insertUpdate(DocumentEvent e) {
                        fire();
                    }

                    @Override
                    public void removeUpdate(DocumentEvent e) {
                        fire();
                    }
                };
                textComponent.getDocument().addDocumentListener(documentListener);

                // Makes the vertical scroll panel view follow the focus
                Component component = textComponent.getParent().getParent();
                if (component instanceof JScrollPane) {
                    JScrollPane scrollPane = (JScrollPane) component;
                    Component scrollPaneParent = scrollPane.getParent();
                    if (scrollPaneParent instanceof JPanel) {
                        JPanel panel = (JPanel) scrollPaneParent;
                        Rectangle bounds = scrollPane.getBounds();
                        panel.scrollRectToVisible(bounds);
                    }
                }

            }
        }

        entryEditorTab.setActive((FieldEditor) event.getSource());
    }

    @Override
    public void focusLost(FocusEvent event) {
        synchronized (this) {
            if (textComponent != null) {
                textComponent.getDocument().removeDocumentListener(documentListener);
                textComponent = null;
                documentListener = null;
            }
        }
        if (!event.isTemporary()) {
            entryEditorTab.getParent().updateField(event.getSource());
        }
    }

}
