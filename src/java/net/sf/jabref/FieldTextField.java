/*
 Copyright (C) 2003 Morten O. Alver, Nizar N. Batada

 All programs in this directory and
 subdirectories are published under the GNU General Public License as
 described below.

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or (at
 your option) any later version.

 This program is distributed in the hope that it will be useful, but
 WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 USA

 Further information about the GNU GPL is available at:
 http://www.gnu.org/copyleft/gpl.ja.html

 */
package net.sf.jabref;

import net.sf.jabref.gui.AutoCompleteListener;

import java.awt.Color;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.UndoableEditListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.undo.UndoManager;
import javax.swing.undo.CannotUndoException;

public class FieldTextField extends JTextField implements FieldEditor {

	protected String fieldName;
	protected JLabel label;

    private AutoCompleteListener autoCompleteListener = null;

    //protected UndoManager undo = new UndoManager();


	public FieldTextField(String fieldName_, String content, boolean changeColorOnFocus) {
		super(content);

        // Listen for undo and redo events
        /*getDocument().addUndoableEditListener(new UndoableEditListener() {
            public void undoableEditHappened(UndoableEditEvent evt) {
                undo.addEdit(evt.getEdit());
            }
        });*/

        updateFont();

        // Add the global focus listener, so a menu item can see if this field
		// was focused when
		// an action was called.
		addFocusListener(Globals.focusListener);
		if (changeColorOnFocus)
			addFocusListener(new FieldEditorFocusListener());
		fieldName = fieldName_;
		label = new FieldNameLabel(" " + Util.nCase(fieldName) + " ");
		// label = new JLabel(" "+Util.nCase(fieldName)+" ", JLabel.CENTER);
		// label.setBorder(BorderFactory.createEtchedBorder());
		setBackground(GUIGlobals.validFieldBackground);
		// label.setOpaque(true);
		// if ((content != null) && (content.length() > 0))
		// label.setForeground(GUIGlobals.validFieldColor);
		// At construction time, the field can never have an invalid value.
		// else label.setForeground(GUIGlobals.nullFieldColor);

		FieldTextMenu popMenu = new FieldTextMenu(this);
		this.addMouseListener(popMenu);
		label.addMouseListener(popMenu);
	}

    public void append(String text) {
		setText(getText() + text);
	}

	public String getFieldName() {
		return fieldName;
	}

	public JLabel getLabel() {
		return label;
	}

	public void setLabelColor(Color c) {
		label.setForeground(c);
		throw new NullPointerException("ok");
	}

	public JComponent getPane() {
		return this;
	}

	public JComponent getTextComponent() {
		return this;

    }

    public void updateFont() {
        setFont(GUIGlobals.CURRENTFONT);
    }

    /*public void paint(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		if (antialias)
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		super.paint(g2);
	}*/

    public void paste(String textToInsert) {
		int sel = getSelectionEnd() - getSelectionStart();
		if (sel < 1) {
			int cPos = getCaretPosition();
			select(cPos, cPos);
		}
		replaceSelection(textToInsert);
	}


    public boolean hasUndoInformation() {
        return false;//undo.canUndo();
    }

    public void undo() {
        /*try {
            if (undo.canUndo()) {
                undo.undo();
            }
        } catch (CannotUndoException e) {
        }*/
    }

    public boolean hasRedoInformation() {
        return false;//undo.canRedo();
    }

    public void redo() {
        /*try {
            if (undo.canRedo()) {
                undo.redo();
            }
        } catch (CannotUndoException e) {
        }*/

    }

    public void addUndoableEditListener(UndoableEditListener listener) {
        getDocument().addUndoableEditListener(listener);
    }

    public void setAutoCompleteListener(AutoCompleteListener listener) {
        autoCompleteListener = listener;
    }

    public void clearAutoCompleteSuggestion() {
        if (autoCompleteListener != null)
            autoCompleteListener.clearCurrentSuggestion(this);
    }
}
