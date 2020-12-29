package org.jabref.model.groups;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutomaticPersonsGroupTest {
    @Test
    void createSubgroupsFromCommaSeparatedLastNames() {
        BibEntry bibEntry = new BibEntry().withField(StandardField.AUTHOR, "Turing, Alan and Hopper, Grace");
        var subgroups = new AutomaticPersonsGroup("", GroupHierarchyType.INDEPENDENT, StandardField.AUTHOR).createSubgroups(bibEntry);
        assertEquals(2, subgroups.size());
        for (GroupTreeNode group : subgroups) {
            String groupName = group.getGroup().getName();
            assertTrue(groupName.equals("Turing") || groupName.equals("Hopper"));
        }
    }

    @Test
    void createSubgroupsContainingCommaSeparatedLastNames() {
        BibEntry bibEntry = new BibEntry().withField(StandardField.AUTHOR, "Turing, Alan and Hopper, Grace");
        BibEntry turingEntry = new BibEntry().withField(StandardField.AUTHOR, "Turing, Alan");
        BibEntry hopperEntry = new BibEntry().withField(StandardField.AUTHOR, "Hopper, Grace");
        var subgroups = new AutomaticPersonsGroup("", GroupHierarchyType.INDEPENDENT, StandardField.AUTHOR).createSubgroups(bibEntry);
        for (GroupTreeNode group : subgroups) {
            String groupName = group.getGroup().getName();
            if (groupName.equals("Turing")) {
                assertTrue(group.matches(turingEntry));
                assertFalse(group.matches(hopperEntry));
            } else if (groupName.equals("Hopper")) {
                assertTrue(group.matches(hopperEntry));
                assertFalse(group.matches(turingEntry));
            }
        }
    }

    @Test
    void createSubgroupContainingLatexAndUnicodeLastNames() {
        BibEntry bibEntry = new BibEntry().withField(StandardField.AUTHOR, "Kurt G{\\\"{o}}del");
        BibEntry godelEntry = new BibEntry().withField(StandardField.AUTHOR, "Kurt Gödel");
        var subgroups = new AutomaticPersonsGroup("", GroupHierarchyType.INDEPENDENT, StandardField.AUTHOR).createSubgroups(bibEntry);
        for (GroupTreeNode group : subgroups) { // There should only be one subgroup
            assertTrue(group.matches(godelEntry));
        }
    }
}
