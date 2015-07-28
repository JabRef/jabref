/*  Copyright (C) 2003-2011 JabRef contributors.
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
package net.sf.jabref;

import net.sf.jabref.gui.AutoCompleteListener;
import net.sf.jabref.util.StringUtil;

import java.awt.*;
import java.util.regex.Pattern;

import javax.swing.*;
import javax.swing.event.UndoableEditListener;

/**
 * An implementation of the FieldEditor backed by a JTextArea. Used for
 * multi-line input.
 */
public class FieldTextArea extends JTextAreaWithHighlighting implements FieldEditor {

    Dimension PREFERRED_SIZE;

    private final JScrollPane sp;

    private final FieldNameLabel label;

    private String fieldName;

    final static Pattern bull = Pattern.compile("\\s*[-\\*]+.*");

    final static Pattern indent = Pattern.compile("\\s+.*");

    private AutoCompleteListener autoCompleteListener = null;


    // protected UndoManager undo = new UndoManager();

    public FieldTextArea(String fieldName_, String content) {
        super(content);

        // Listen for undo and redo events
        /*
         * getDocument().addUndoableEditListener(new UndoableEditListener() {
         * public void undoableEditHappened(UndoableEditEvent evt) {
         * undo.addEdit(evt.getEdit()); } });
         */

        updateFont();

        // Add the global focus listener, so a menu item can see if this field
        // was focused when an action was called.
        addFocusListener(Globals.focusListener);
        addFocusListener(new FieldEditorFocusListener());
        sp = new JScrollPane(this, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setMinimumSize(new Dimension(200, 1));

        setLineWrap(true);
        setWrapStyleWord(true);
        fieldName = fieldName_;

        label = new FieldNameLabel(' ' + StringUtil.nCase(fieldName) + ' ');
        setBackground(GUIGlobals.validFieldBackgroundColor);
        setForeground(GUIGlobals.editorTextColor);

        // setFont(new Font("Times", Font.PLAIN, 10));

        FieldTextMenu popMenu = new FieldTextMenu(this);
        this.addMouseListener(popMenu);
        label.addMouseListener(popMenu);

    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    /*
     * public void paint(Graphics g) { Graphics2D g2 = (Graphics2D) g; if
     * (antialias) g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
     * RenderingHints.VALUE_ANTIALIAS_ON); super.paint(g2); }
     */

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
    public void setLabelColor(Color c) {
        label.setForeground(c);
    }

    @Override
    public JComponent getPane() {
        return sp;
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
        setFont(GUIGlobals.CURRENTFONT);
    }

    @Override
    public void paste(String textToInsert) {
        int sel = getSelectionEnd() - getSelectionStart();
        if (sel > 0) {
            replaceSelection(textToInsert);
        } else {
            int cPos = this.getCaretPosition();
            this.insert(textToInsert, cPos);
        }
    }

    @Override
    public boolean hasUndoInformation() {
        return false;// undo.canUndo();
    }

    @Override
    public void undo() {
        /*
         * try { if (undo.canUndo()) { undo.undo(); } } catch
         * (CannotUndoException e) { }
         */

    }

    @Override
    public boolean hasRedoInformation() {
        return false;// undo.canRedo();
    }

    @Override
    public void redo() {
        /*
         * try { if (undo.canRedo()) { undo.redo(); } } catch
         * (CannotUndoException e) { }
         */

    }

    @Override
    public void addUndoableEditListener(UndoableEditListener listener) {
        getDocument().addUndoableEditListener(listener);
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
