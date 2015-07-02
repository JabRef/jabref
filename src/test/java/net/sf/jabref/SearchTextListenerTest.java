package net.sf.jabref;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.swing.text.Highlighter;
import javax.swing.text.Highlighter.Highlight;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

public class SearchTextListenerTest {

    @Before
    public void setUp() throws Exception {

        Globals.prefs = JabRefPreferences.getInstance();
        GUIGlobals.setUpIconTheme();
    }

    @Test
    public void testHighlighting() {

        String content = "Test Word Content";
        String contentToHighlight1 = "Word";
        String contentToHighlight2 = "Content";

        FieldTextArea ta = new FieldTextArea("", content);

        Highlighter highlighter = ta.getHighlighter();
        Highlight[] highlight = highlighter.getHighlights();

        //there is no area to highlight!
        Assert.assertEquals("Expected no highlighting area ", 0, highlight.length);

        //set up arraylist with "word" and inform the fieldtextarea
        ArrayList<String> wordsToHighlight = new ArrayList<String>();
        wordsToHighlight.add(contentToHighlight1);
        ta.searchText(wordsToHighlight);

        highlighter = ta.getHighlighter();
        highlight = highlighter.getHighlights();

        //there is one area to highlight!
        Assert.assertEquals("Expected one highlighting area ", 1, highlight.length);
        //start of ... Word
        Assert.assertEquals(content.indexOf(contentToHighlight1), highlight[0].getStartOffset());

        //end of ... word
        Assert.assertEquals(content.indexOf(contentToHighlight1) + contentToHighlight1.length(), highlight[0].getEndOffset());

        //add another word "content" and refresh highlighting
        wordsToHighlight.add(contentToHighlight2);
        ta.searchText(wordsToHighlight);
        highlighter = ta.getHighlighter();
        highlight = highlighter.getHighlights();

        //there are two areas to highlight!
        Assert.assertEquals("Expected two highlighting areas ", 2, highlight.length);

        //start of ... content
        Assert.assertEquals(content.indexOf(contentToHighlight2), highlight[1].getStartOffset());

        //end of ... content
        Assert.assertEquals(content.indexOf(contentToHighlight2) + contentToHighlight2.length(), highlight[1].getEndOffset());

        //remove everything and check if highlighting is vanished
        wordsToHighlight.clear();
        ta.searchText(wordsToHighlight);
        highlighter = ta.getHighlighter();
        highlight = highlighter.getHighlights();

        //there should be none areas to highlight!
        Assert.assertEquals("Expected no highlighting area ", 0, highlight.length);
    }

    @Test
    public void testHighlightingContentIndependence() {

        String content = "Test Word Content";
        String contentToHighlight1 = "Word";

        FieldTextArea ta = new FieldTextArea("", content);

        String textOne = ta.getText();

        //set up arraylist with "word" and inform the fieldtextarea
        ArrayList<String> wordsToHighlight = new ArrayList<String>();
        wordsToHighlight.add(contentToHighlight1);
        ta.searchText(wordsToHighlight);

        String textTwo = ta.getText();

        //set up empty arraylist and inform the fieldtextarea
        ArrayList<String> wordsToHighlight2 = new ArrayList<String>();
        ta.searchText(wordsToHighlight2);

        String textThree = ta.getText();

        Assert.assertEquals("Highlighting may not change content", textOne, textTwo);
        Assert.assertEquals("Highlighting may not change content", textOne, textThree);
    }

    @Test
    public void testHighlightingInvalidParameter() {

        String content = "Test Word Content";
        String contentToHighlight1 = "Word";

        FieldTextArea ta = new FieldTextArea("", content);

        //should not matter at all
        ta.searchText(null);
    }
}
