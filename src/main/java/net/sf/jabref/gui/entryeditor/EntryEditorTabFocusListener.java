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
package net.sf.jabref.gui.entryeditor;

import net.sf.jabref.gui.fieldeditors.FieldEditor;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/*
 * Focus listener that fires the storeFieldAction when a TextArea loses
 * focus.
 */
class EntryEditorTabFocusListener implements FocusListener {

    private JTextComponent textComponent;

    private DocumentListener documentListener;

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
                /**
                 * [ 1553552 ] Not properly detecting changes to flag as
                 * changed
                 */
                documentListener = new DocumentListener() {

                    void fire() {
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

                /**
                 * Makes the vertical scroll panel view follow the focus
                 */
                Component scrollPane = textComponent.getParent().getParent();
                if (scrollPane instanceof JScrollPane) {
                    JScrollPane componentPane = (JScrollPane) scrollPane;
                    Component cPane = componentPane.getParent();
                    if (cPane instanceof JPanel) {
                        JPanel panel = (JPanel) cPane;
                        Rectangle bounds = componentPane.getBounds();
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
