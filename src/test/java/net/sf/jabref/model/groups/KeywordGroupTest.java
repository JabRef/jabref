package net.sf.jabref.model.groups;

import net.sf.jabref.model.ParseException;
import net.sf.jabref.model.entry.BibEntry;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class KeywordGroupTest {

    @Test
    public void testToString() throws ParseException {
        KeywordGroup group = new KeywordGroup("myExplicitGroup", "author", "asdf", true, true,
                GroupHierarchyType.INDEPENDENT, ", ");
        assertEquals("KeywordGroup:myExplicitGroup;0;author;asdf;1;1;", group.toString());
    }

    @Test
    public void testToString2() throws ParseException {
        KeywordGroup group = new KeywordGroup("myExplicitGroup", "author", "asdf", false, true,
                GroupHierarchyType.REFINING, ", ");
        assertEquals("KeywordGroup:myExplicitGroup;1;author;asdf;0;1;", group.toString());
    }

    @Test
    public void containsSimpleWord() throws Exception {
        KeywordGroup group = new KeywordGroup("name", "keywords", "test", false, false, GroupHierarchyType.INDEPENDENT,
                ", ");
        BibEntry entry = new BibEntry().withField("keywords", "test");

        assertTrue(group.isMatch(entry));
    }

    @Test
    public void containsSimpleWordInSentence() throws Exception {
        KeywordGroup group = new KeywordGroup("name", "keywords", "test", false, false, GroupHierarchyType.INDEPENDENT,
                ", ");
        BibEntry entry = new BibEntry().withField("keywords", "Some sentence containing test word");

        assertTrue(group.isMatch(entry));
    }

    @Test
    public void containsSimpleWordCommaSeparated() throws Exception {
        KeywordGroup group = new KeywordGroup("name", "keywords", "test", false, false, GroupHierarchyType.INDEPENDENT,
                ", ");
        BibEntry entry = new BibEntry().withField("keywords", "Some,list,containing,test,word");

        assertTrue(group.isMatch(entry));
    }

    @Test
    public void containsSimpleWordSemicolonSeparated() throws Exception {
        KeywordGroup group = new KeywordGroup("name", "keywords", "test", false, false, GroupHierarchyType.INDEPENDENT,
                ", ");
        BibEntry entry = new BibEntry().withField("keywords", "Some;list;containing;test;word");

        assertTrue(group.isMatch(entry));
    }

    @Test
    public void containsComplexWord() throws Exception {
        KeywordGroup group = new KeywordGroup("name", "keywords", "\\H2O", false, false, GroupHierarchyType.INDEPENDENT,
                ", ");
        BibEntry entry = new BibEntry().withField("keywords", "\\H2O");

        assertTrue(group.isMatch(entry));
    }

    @Test
    public void containsComplexWordInSentence() throws Exception {
        KeywordGroup group = new KeywordGroup("name", "keywords", "\\H2O", false, false, GroupHierarchyType.INDEPENDENT,
                ", ");
        BibEntry entry = new BibEntry().withField("keywords", "Some sentence containing \\H2O word");

        assertTrue(group.isMatch(entry));
    }

    @Test
    public void containsWordWithWhitespaceInSentence() throws Exception {
        KeywordGroup group = new KeywordGroup("name", "keywords", "test word", false, false,
                GroupHierarchyType.INDEPENDENT, ", ");
        BibEntry entry = new BibEntry().withField("keywords", "Some sentence containing test word");

        assertTrue(group.isMatch(entry));
    }
}
