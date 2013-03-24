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
import net.sf.jabref.SearchTextListener;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.regex.Pattern;

import javax.swing.*;
import javax.swing.event.UndoableEditListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.UndoManager;
import javax.swing.undo.CannotUndoException;

/**
 * An implementation of the FieldEditor backed by a JTextArea. Used for
 * multi-line input.
 * 
 * @author $Author$
 * @version $Revision$ ($Date$)
 * 
 */
public class FieldTextArea extends JTextAreaWithHighlighting implements FieldEditor {

	Dimension PREFERRED_SIZE;

	JScrollPane sp;

	FieldNameLabel label;

	String fieldName;

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
		sp = new JScrollPane(this, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		sp.setMinimumSize(new Dimension(200, 1));

		setLineWrap(true);
		setWrapStyleWord(true);
		fieldName = fieldName_;

		label = new FieldNameLabel(" " + Util.nCase(fieldName) + " ");
		setBackground(GUIGlobals.validFieldBackgroundColor);
		setForeground(GUIGlobals.editorTextColor);

		// setFont(new Font("Times", Font.PLAIN, 10));

		FieldTextMenu popMenu = new FieldTextMenu(this);
		this.addMouseListener(popMenu);
		label.addMouseListener(popMenu);

    }

	public Dimension getPreferredScrollableViewportSize() {
		return getPreferredSize();
	}

	/*
	 * public void paint(Graphics g) { Graphics2D g2 = (Graphics2D) g; if
	 * (antialias) g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
	 * RenderingHints.VALUE_ANTIALIAS_ON); super.paint(g2); }
	 */

    public String getFieldName() {
		return fieldName;
	}

	public void setFieldName(String newName) {
		fieldName = newName;
	}

	public JLabel getLabel() {
		return label;
	}

	public void setLabelColor(Color c) {
		label.setForeground(c);
	}

	public JComponent getPane() {
		return sp;
	}

	public JComponent getTextComponent() {
		return this;
	}

	public void setActiveBackgroundColor() {
		setBackground(GUIGlobals.activeBackground);
	}

	public void setValidBackgroundColor() {
		setBackground(GUIGlobals.validFieldBackgroundColor);
	}

	public void setInvalidBackgroundColor() {
		setBackground(GUIGlobals.invalidFieldBackgroundColor);
	}

	public void updateFontColor() {
		setForeground(GUIGlobals.editorTextColor);
	}

	public void updateFont() {
		setFont(GUIGlobals.CURRENTFONT);
	}

	public void paste(String textToInsert) {
		int sel = getSelectionEnd() - getSelectionStart();
		if (sel > 0) // selected text available
			replaceSelection(textToInsert);
		else {
			int cPos = this.getCaretPosition();
			this.insert(textToInsert, cPos);
		}
	}

	public boolean hasUndoInformation() {
		return false;// undo.canUndo();
	}

	public void undo() {
		/*
		 * try { if (undo.canUndo()) { undo.undo(); } } catch
		 * (CannotUndoException e) { }
		 */

	}

	public boolean hasRedoInformation() {
		return false;// undo.canRedo();
	}

	public void redo() {
		/*
		 * try { if (undo.canRedo()) { undo.redo(); } } catch
		 * (CannotUndoException e) { }
		 */

	}

	public void addUndoableEditListener(UndoableEditListener listener) {
		getDocument().addUndoableEditListener(listener);
	}

	public void setAutoCompleteListener(AutoCompleteListener listener) {
		autoCompleteListener = listener;
	}

	public void clearAutoCompleteSuggestion() {
		if (autoCompleteListener != null) {
			autoCompleteListener.clearCurrentSuggestion(this);
		}
	}
}
