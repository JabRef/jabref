package org.jabref;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class SearchQueryHighlightListenerTest {

    //@Mock
    //private ProtectedTermsLoader loader;


    @Before
    public void setUp() {
        //Globals.prefs = JabRefPreferences.getInstance();
        //Globals.protectedTermsLoader = loader;
        //when(loader.getProtectedTermsLists()).thenReturn(Collections.emptyList());
    }

    @Test
    public void dummyTest() {
        assertTrue(true);
    }

    /*
    // TODO: Reenable these tests and remove dummyTest
    @Test
    public void testHighlighting() {

        String content = "Test Word Content";
        String contentToHighlight1 = "Word";
        String contentToHighlight2 = "Content";

        TextArea ta = new TextArea("", content);

        Highlighter highlighter = ta.getHighlighter();
        Highlight[] highlight = highlighter.getHighlights();

        //there is no area to highlight!
        Assert.assertEquals("Expected no highlighting area ", 0, highlight.length);

        ta.highlightPattern(Optional.of(Pattern.compile("Word")));

        highlighter = ta.getHighlighter();
        highlight = highlighter.getHighlights();

        //there is one area to highlight!
        Assert.assertEquals("Expected one highlighting area ", 1, highlight.length);
        //start of ... Word
        Assert.assertEquals(content.indexOf(contentToHighlight1), highlight[0].getStartOffset());

        //end of ... word
        Assert.assertEquals(content.indexOf(contentToHighlight1) + contentToHighlight1.length(),
                highlight[0].getEndOffset());

        //add another word "content" and refresh highlighting
        ta.highlightPattern(Optional.of(Pattern.compile("(Word|Content)")));
        highlighter = ta.getHighlighter();
        highlight = highlighter.getHighlights();

        //there are two areas to highlight!
        Assert.assertEquals("Expected two highlighting areas ", 2, highlight.length);

        //start of ... content
        Assert.assertEquals(content.indexOf(contentToHighlight2), highlight[1].getStartOffset());

        //end of ... content
        Assert.assertEquals(content.indexOf(contentToHighlight2) + contentToHighlight2.length(),
                highlight[1].getEndOffset());

        //remove everything and check if highlighting is vanished
        ta.highlightPattern(Optional.empty());
        highlighter = ta.getHighlighter();
        highlight = highlighter.getHighlights();

        //there should be none areas to highlight!
        Assert.assertEquals("Expected no highlighting area ", 0, highlight.length);
    }

    @Test
    public void testHighlightingContentIndependence() {
        String content = "Test Word Content";
        TextArea ta = new TextArea("", content);
        String textOne = ta.getText();

        ta.highlightPattern(Optional.of((Pattern.compile("Word"))));

        String textTwo = ta.getText();
        Assert.assertEquals("Highlighting may not change content", textOne, textTwo);

        //set up empty arraylist and inform the fieldtextarea
        ta.highlightPattern(Optional.empty());

        String textThree = ta.getText();
        Assert.assertEquals("Highlighting may not change content", textOne, textThree);
    }

    @Test
    public void testHighlightingInvalidParameter() {
        String content = "Test Word Content";

        TextArea ta = new TextArea("", content);

        //should not matter at all
        ta.highlightPattern(null);
    }

    */
}
