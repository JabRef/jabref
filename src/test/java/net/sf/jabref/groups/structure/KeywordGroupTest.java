package net.sf.jabref.groups.structure;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class KeywordGroupTest {

    @Test
    public void testToString() {
        KeywordGroup group = new KeywordGroup("myExplicitGroup", "author","asdf", true, true,
                GroupHierarchyType.INDEPENDENT);
        assertEquals("KeywordGroup:myExplicitGroup;0;author;asdf;1;1;", group.toString());
    }

    @Test
    public void testToString2() {
        KeywordGroup group = new KeywordGroup("myExplicitGroup", "author","asdf", false, true,
                GroupHierarchyType.REFINING);
        assertEquals("KeywordGroup:myExplicitGroup;1;author;asdf;0;1;", group.toString());
    }

}