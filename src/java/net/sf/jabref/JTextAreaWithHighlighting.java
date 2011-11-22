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

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;

public class JTextAreaWithHighlighting extends JTextArea implements SearchTextListener {

	private ArrayList<String> wordsToHighlight;

	public JTextAreaWithHighlighting() {
		super();
	}

	public JTextAreaWithHighlighting(String text) {
		super(text);
	}

	public JTextAreaWithHighlighting(Document doc) {
		super(doc);
	}

	public JTextAreaWithHighlighting(int rows, int columns) {
		super(rows, columns);
	}

	public JTextAreaWithHighlighting(String text, int rows, int columns) {
		super(text, rows, columns);
	}

	public JTextAreaWithHighlighting(Document doc, String text, int rows,
			int columns) {
		super(doc, text, rows, columns);
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
	}

	@Override
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
