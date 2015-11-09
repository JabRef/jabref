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
package net.sf.jabref.gui.fieldeditors;

import java.awt.Color;
import java.awt.Container;

import javax.swing.JComponent;
import javax.swing.JLabel;

import net.sf.jabref.gui.autocompleter.AutoCompleteListener;

/**
 * FieldEditors is a common interface between the TextField and TextArea.
 */
public interface FieldEditor {

    String getFieldName();

    /*
     * Returns the component to be added to a container. Might be a JScrollPane
     * or the component itself.
     */
    JComponent getPane();

    /*
     * Returns the text component itself.
     */
    JComponent getTextComponent();

    JLabel getLabel();

    void setActiveBackgroundColor();

    void setValidBackgroundColor();

    void setInvalidBackgroundColor();

    void setLabelColor(Color color);

    void setBackground(Color color);

    void updateFontColor();

    String getText();

    /**
     * Sets the given text on the current field editor and marks this text
     * editor as modified.
     *
     * @param newText
     */
    void setText(String newText);

    void append(String text);

    Container getParent();

    void requestFocus();

    void setEnabled(boolean enabled);

    void updateFont();

    /**
     * paste text into component, it should also take some selected text into
     * account
     */
    void paste(String textToInsert);

    /**
     * normally implemented in JTextArea and JTextField
     *
     * @return
     */
    String getSelectedText();

    void undo();

    void redo();

    void setAutoCompleteListener(AutoCompleteListener listener);

    void clearAutoCompleteSuggestion();
}
