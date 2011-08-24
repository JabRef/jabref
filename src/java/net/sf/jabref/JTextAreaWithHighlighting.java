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

		if (words == null || words.size() == 0) {
			return;
		}
		String content = getText().toUpperCase();
		if (content.isEmpty())
			return;

		Matcher matcher = Globals.getPatternForWords(words).matcher(content);

		while (matcher.find()) {
			try {
				h.addHighlight(matcher.start(), matcher.end(),
						DefaultHighlighter.DefaultPainter);
			} catch (BadLocationException ble) {
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