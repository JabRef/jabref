/*  Copyright (C) 2003-2015 JabRef contributors.
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

import java.util.Optional;
import java.util.regex.Pattern;

import javax.swing.text.Highlighter;
import javax.swing.text.Highlighter.Highlight;

import net.sf.jabref.gui.fieldeditors.TextArea;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SearchQueryHighlightListenerTest {

    @Before
    public void setUp() {
        Globals.prefs = JabRefPreferences.getInstance();
    }

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
}
