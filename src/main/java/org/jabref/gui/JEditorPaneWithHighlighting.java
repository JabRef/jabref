package org.jabref.gui;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JEditorPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import javax.swing.text.LayeredHighlighter.LayerPainter;

import org.jabref.logic.search.SearchQueryHighlightListener;

import com.jcabi.log.Logger;

public class JEditorPaneWithHighlighting extends JEditorPane implements SearchQueryHighlightListener {


    public void highlightPattern(Optional<Pattern> highlightPattern) {
        Highlighter highlighter = getHighlighter();
        highlighter.removeAllHighlights();

        if ((highlightPattern == null) || !highlightPattern.isPresent()) {
            return;
        }

        String text = getDocumentText();

        Matcher matcher = highlightPattern.get().matcher(text);
        LayerPainter painter = DefaultHighlighter.DefaultPainter;
        while (matcher.find()) {
            try {
                highlighter.addHighlight(matcher.start(), matcher.end(), painter);
            } catch (BadLocationException ble) {
                // should not occur if matcher works right
                Logger.warn(this, "Highlighting not possible, bad location", ble);
            }
        }
    }

    private String getDocumentText() {
        Document doc = getDocument();
        String text;
        try {
            text = doc.getText(0, doc.getLength());
        } catch (Exception e) {
            Logger.error(this, "Error while getting document text");
            text = "";
        }
        return text;
    }
}
