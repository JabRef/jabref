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
import java.awt.Dimension;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.GUIGlobals;
import net.sf.jabref.gui.autocompleter.AutoCompleteListener;
import net.sf.jabref.gui.fieldeditors.contextmenu.FieldTextMenu;
import net.sf.jabref.model.entry.EntryUtil;

/**
 * An implementation of the FieldEditor backed by a JTextArea.
 * Used for multi-line input, currently all BibTexFields except Bibtex key!
 */
public class TextArea extends JTextAreaWithHighlighting implements FieldEditor {

    private final JScrollPane scrollPane;

    private final FieldNameLabel label;

    private String fieldName;

    private AutoCompleteListener autoCompleteListener;


    public TextArea(String fieldName, String content) {
        super(content);


        updateFont();

        // Add the global focus listener, so a menu item can see if this field
        // was focused when an action was called.
        addFocusListener(Globals.focusListener);
        addFocusListener(new FieldEditorFocusListener());
        scrollPane = new JScrollPane(this, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setMinimumSize(new Dimension(200, 1));

        setLineWrap(true);
        setWrapStyleWord(true);
        this.fieldName = fieldName;

        label = new FieldNameLabel(' ' + EntryUtil.capitalizeFirst(this.fieldName) + ' ');
        setBackground(GUIGlobals.validFieldBackgroundColor);
        setForeground(GUIGlobals.editorTextColor);


        FieldTextMenu popMenu = new FieldTextMenu(this);
        this.addMouseListener(popMenu);
        label.addMouseListener(popMenu);
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String newName) {
        fieldName = newName;
    }

    @Override
    public JLabel getLabel() {
        return label;
    }

    @Override
    public void setLabelColor(Color color) {
        label.setForeground(color);
    }

    @Override
    public JComponent getPane() {
        return scrollPane;
    }

    @Override
    public JComponent getTextComponent() {
        return this;
    }

    @Override
    public void setActiveBackgroundColor() {
        setBackground(GUIGlobals.activeBackground);
    }

    @Override
    public void setValidBackgroundColor() {
        setBackground(GUIGlobals.validFieldBackgroundColor);
    }

    @Override
    public void setInvalidBackgroundColor() {
        setBackground(GUIGlobals.invalidFieldBackgroundColor);
    }

    @Override
    public void updateFontColor() {
        setForeground(GUIGlobals.editorTextColor);
    }

    @Override
    public void updateFont() {
        setFont(GUIGlobals.currentFont);
    }

    @Override
    public void paste(String textToInsert) {
        replaceSelection(textToInsert);
    }

    @Override
    public void undo() {
        // Nothing
    }

    @Override
    public void redo() {
        // Nothing
    }

    @Override
    public void setAutoCompleteListener(AutoCompleteListener listener) {
        autoCompleteListener = listener;
    }

    @Override
    public void clearAutoCompleteSuggestion() {
        if (autoCompleteListener != null) {
            autoCompleteListener.clearCurrentSuggestion(this);
        }
    }
}
