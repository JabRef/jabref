package net.sf.jabref.model.groups;

import java.util.Optional;

import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class KeywordGroupTest {

    private KeywordGroup keywordTestGroup;
    private KeywordGroup complexKeywordGroup;
    private BibEntry emptyEntry;

    @Before
    public void setUp() {
        keywordTestGroup = new KeywordGroup("name", "keywords", "test", false, false, GroupHierarchyType.INDEPENDENT, ',');
        complexKeywordGroup = new KeywordGroup("name", "keywords", "\\H2O", false, false, GroupHierarchyType.INDEPENDENT, ',');
        emptyEntry = new BibEntry();
    }

    @Test
    public void testToString() {
        assertEquals("KeywordGroup:name;0;keywords;test;0;0;", keywordTestGroup.toString());
    }

    @Test
    public void testToString2() {
        KeywordGroup anotherGroup = new KeywordGroup("myExplicitGroup", "author", "asdf", false, true,
                GroupHierarchyType.REFINING, ',');
        assertEquals("KeywordGroup:myExplicitGroup;1;author;asdf;0;1;", anotherGroup.toString());
    }

    @Test
    public void containsSimpleWord() {
        emptyEntry.setField("keywords", "test");

        assertTrue(keywordTestGroup.isMatch(emptyEntry));
    }

    @Test
    public void containsSimpleWordInSentence() throws Exception {
        emptyEntry.setField("keywords", "Some sentence containing test word");

        assertTrue(keywordTestGroup.isMatch(emptyEntry));
    }

    @Test
    public void containsSimpleWordCommaSeparated() throws Exception {
        emptyEntry.setField("keywords", "Some,list,containing,test,word");

        assertTrue(keywordTestGroup.isMatch(emptyEntry));
    }

    @Test
    public void containsSimpleWordSemicolonSeparated() throws Exception {
        emptyEntry.setField("keywords", "Some;list;containing;test;word");

        assertTrue(keywordTestGroup.isMatch(emptyEntry));
    }

    @Test
    public void containsComplexWord() throws Exception {
        emptyEntry.setField("keywords", "\\H2O");

        assertTrue(complexKeywordGroup.isMatch(emptyEntry));
    }

    @Test
    public void containsComplexWordInSentence() throws Exception {
        emptyEntry.setField("keywords", "Some sentence containing \\H2O word");

        assertTrue(complexKeywordGroup.isMatch(emptyEntry));
    }

    @Test
    public void containsWordWithWhitespaceInSentence() throws Exception {
        emptyEntry.setField("keywords", "Some sentence containing test word");

        assertTrue(keywordTestGroup.isMatch(emptyEntry));
    }

    @Test
    public void addGroupToBibEntrySuccessfullyIfEmptyBefore() throws Exception {
        keywordTestGroup.add(emptyEntry);

        assertEquals(Optional.of("test"), emptyEntry.getField(FieldName.KEYWORDS));
    }

    @Test
    public void addGroupToBibEntrySuccessfullyIfNotEmptyBefore() throws Exception {
        emptyEntry.setField(FieldName.KEYWORDS, "bla, blubb");
        keywordTestGroup.add(emptyEntry);

        assertEquals(Optional.of("bla, blubb, test"), emptyEntry.getField(FieldName.KEYWORDS));
    }

    @Test
    public void noDuplicateStoredIfAlreadyInGroup() throws Exception {
        emptyEntry.setField(FieldName.KEYWORDS, "test, blubb");
        keywordTestGroup.add(emptyEntry);

        assertEquals(Optional.of("test, blubb"), emptyEntry.getField(FieldName.KEYWORDS));
    }
}
