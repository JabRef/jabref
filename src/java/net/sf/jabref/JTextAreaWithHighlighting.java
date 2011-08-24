package net.sf.jabref;

import java.util.ArrayList;

import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;

public class JTextAreaWithHighlighting extends JTextArea implements SearchTextListener {

	private ArrayList<String> textToHighlight;

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
			if (content.equals(""))
				return;
			
			for(String word: words){
			
			String text = word.toUpperCase();
	
			int index = 0;
			if (!word.equals(""))
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

	@Override
	public void searchText(ArrayList<String> words) {
		// words have to be stored in class variable as 
		// setText() makes use of them
		textToHighlight = words;
		
		highLight(words);
		
	}

}