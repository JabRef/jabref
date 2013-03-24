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

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.*;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

public class JTextAreaWithHighlighting extends JTextArea implements SearchTextListener {

	private ArrayList<String> wordsToHighlight;

    private UndoManager undo;


	public JTextAreaWithHighlighting() {
		super();
        setupUndoRedo();
	}

	public JTextAreaWithHighlighting(String text) {
		super(text);
        setupUndoRedo();
	}

	public JTextAreaWithHighlighting(Document doc) {
		super(doc);
        setupUndoRedo();
	}

	public JTextAreaWithHighlighting(int rows, int columns) {
		super(rows, columns);
        setupUndoRedo();
	}

	public JTextAreaWithHighlighting(String text, int rows, int columns) {
		super(text, rows, columns);
        setupUndoRedo();
	}

	public JTextAreaWithHighlighting(Document doc, String text, int rows,
			int columns) {
		super(doc, text, rows, columns);
        setupUndoRedo();
	}

    protected void setupUndoRedo() {
        undo = new UndoManager();
        Document doc = getDocument();

        // Listen for undo and redo events
        doc.addUndoableEditListener(new UndoableEditListener() {
            public void undoableEditHappened(UndoableEditEvent evt) {
                undo.addEdit(evt.getEdit());
            }
        });

        // Create an undo action and add it to the text component
        getActionMap().put("Undo",
                new AbstractAction("Undo") {
                    public void actionPerformed(ActionEvent evt) {
                        try {
                            if (undo.canUndo()) {
                                undo.undo();
                            }
                        } catch (CannotUndoException e) {
                        }
                    }
                });

        // Bind the undo action to ctl-Z
        getInputMap().put(Globals.prefs.getKey("Undo"), "Undo");

        // Create a redo action and add it to the text component
        getActionMap().put("Redo",
                new AbstractAction("Redo") {
                    public void actionPerformed(ActionEvent evt) {
                        try {
                            if (undo.canRedo()) {
                                undo.redo();
                            }
                        } catch (CannotRedoException e) {
                        }
                    }
                });

        // Bind the redo action to ctl-Y
        getInputMap().put(Globals.prefs.getKey("Redo"), "Redo");
    }

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

		if (words == null || words.isEmpty() || words.get(0).isEmpty()) {
			return;
		}
		String content = getText();
		if (content.isEmpty())
			return;

		Matcher matcher = Globals.getPatternForWords(words).matcher(content);

		while (matcher.find()) {
			try {
				h.addHighlight(matcher.start(), matcher.end(), DefaultHighlighter.DefaultPainter);
			} catch (BadLocationException ble) {
				// should not occur if matcher works right
				System.out.println(ble);
			}
		}

	}

	@Override
	public void setText(String t) {
		super.setText(t);
		if (Globals.prefs.getBoolean("highLightWords")) {
			highLight(wordsToHighlight);
		}
        if (undo != null) undo.discardAllEdits();
	}

	public void searchText(ArrayList<String> words) {
		// words have to be stored in class variable as 
		// setText() makes use of them
		
		if (Globals.prefs.getBoolean("highLightWords")) {
			this.wordsToHighlight = words;
			highLight(words);
		} else {
			if (this.wordsToHighlight != null) {
				// setting of "highLightWords" seems to have changed.
				// clear all highlights and remember the clearing (by wordsToHighlight = null)
				this.wordsToHighlight = null;
				highLight(null);
			}
		}
		
	}

}
