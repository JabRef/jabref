/*
 Copyright (C) 2003 Nizar N. Batada, Morten O. Alver

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
import net.sf.jabref.SearchTextListener;

import java.awt.*;
import java.util.ArrayList;
import java.util.regex.Pattern;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.UndoableEditListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
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
public class FieldTextArea extends JTextArea implements FieldEditor, SearchTextListener {

	Dimension PREFERRED_SIZE;

	JScrollPane sp;

	FieldNameLabel label;

	String fieldName;

	final static Pattern bull = Pattern.compile("\\s*[-\\*]+.*");

	final static Pattern indent = Pattern.compile("\\s+.*");

	private AutoCompleteListener autoCompleteListener = null;

	// protected UndoManager undo = new UndoManager();

	//text to highlight (like from searches) 
	private ArrayList<String> textToHighlight;
	
	/**
	 * Highlight words in the Textarea
	 * 
	 * @param words to highlight
	 */
	private void highLight(ArrayList<String> words) {
		// highlight all characters that appear in charsToHighlight
		Highlighter h = getHighlighter();
		// myTa.set
		h.removeAllHighlights();

		if (words == null || words.size() == 0) {
			return;
		}
		String content = getText().toUpperCase();
		if (content.equals(""))
			return;
		
		for(String word: words){
		
		String text = word.toUpperCase();

		int index = 0;
		while (true) {
			int startposition = content.indexOf(text, index);
			if (startposition == -1)
				break;

			try {
//				System.out.println("highlight @ " + startposition);
				h.addHighlight(startposition, startposition + text.length(),
						DefaultHighlighter.DefaultPainter);
			} catch (BadLocationException ble) {
			}
			index = startposition + 1;
		}	
	  }
	}
	
	@Override
	public void setText(String t) {
		super.setText(t);
		highLight(textToHighlight);
	}

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

	@Override
	public void searchText(ArrayList<String> words) {
		// words have to be stored in class variable as 
		// setText() makes use of them
		textToHighlight = words;
		
		highLight(words);
		
	}
}
