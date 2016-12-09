package net.sf.jabref.model.groups;

import net.sf.jabref.model.entry.BibEntry;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SearchGroupTest {


    @Test
    public void testContains() {
        SearchGroup group = new SearchGroup("myExplicitGroup", GroupHierarchyType.INDEPENDENT, "review",
                true, true);
        assertEquals("SearchGroup:myExplicitGroup;0;review;1;1;", group.toString());

        BibEntry entry = new BibEntry();
        assertFalse(group.contains(entry));

        entry.addKeyword("review", ',');
        assertTrue(group.contains(entry));
    }

    @Test
    public void testToStringSimple() {
        SearchGroup group = new SearchGroup("myExplicitGroup", GroupHierarchyType.INDEPENDENT, "author=harrer",
                true, true);
        assertEquals("SearchGroup:myExplicitGroup;0;author=harrer;1;1;", group.toString());
    }

    @Test
    public void testToStringComplex() {
        SearchGroup group = new SearchGroup("myExplicitGroup", GroupHierarchyType.INCLUDING, "author=\"harrer\"", true, false
        );
        assertEquals("SearchGroup:myExplicitGroup;2;author=\"harrer\";1;0;", group.toString());
    }

}
