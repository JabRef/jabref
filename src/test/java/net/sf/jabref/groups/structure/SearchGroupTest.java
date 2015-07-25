package net.sf.jabref.groups.structure;

import org.junit.Test;

import static org.junit.Assert.*;

public class SearchGroupTest {

    @Test
    public void testToStringSimple() {
        SearchGroup group = new SearchGroup("myExplicitGroup", "author=harrer",
                true, true, GroupHierarchyType.INDEPENDENT);
        assertEquals("SearchGroup:myExplicitGroup;0;author=harrer;1;1;", group.toString());
    }

    @Test
    public void testToStringComplex() {
        SearchGroup group = new SearchGroup("myExplicitGroup", "author=\"harrer\"", true, false,
                GroupHierarchyType.INCLUDING);
        assertEquals("SearchGroup:myExplicitGroup;2;author=\"harrer\";1;0;", group.toString());
    }

}