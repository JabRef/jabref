package org.jabref.model.groups;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutomaticPersonsGroupTest {
    private static Set<GroupTreeNode> createPersonSubGroupFrom(String... lastNames) {
        return Arrays.stream(lastNames).distinct()
                     .map(lastName ->
                             new LastNameGroup(lastName, GroupHierarchyType.INDEPENDENT, StandardField.AUTHOR, lastName))
                     .map(GroupTreeNode::new)
                     .collect(Collectors.toSet());
    }

    @Test
    void createSubgroupsFromCommaSeparatedLastNames() {
        BibEntry bibEntry = new BibEntry().withField(StandardField.AUTHOR, "Turing, Alan and Hopper, Grace");
        var subgroups = new AutomaticPersonsGroup("", GroupHierarchyType.INDEPENDENT, StandardField.AUTHOR).createSubgroups(bibEntry);
        var expectedSubgroups = createPersonSubGroupFrom("Turing", "Hopper");
        assertEquals(expectedSubgroups, subgroups);
    }

    @Test
    void createSubgroupsContainingCommaSeparatedLastNames() {
        BibEntry bibEntry = new BibEntry().withField(StandardField.AUTHOR, "Turing, Alan and Hopper, Grace");
        BibEntry turingEntry = new BibEntry().withField(StandardField.AUTHOR, "Turing, Alan");
        BibEntry hopperEntry = new BibEntry().withField(StandardField.AUTHOR, "Hopper, Grace");
        var subgroups = new AutomaticPersonsGroup("", GroupHierarchyType.INDEPENDENT, StandardField.AUTHOR).createSubgroups(bibEntry);
        var expectedSubgroups = createPersonSubGroupFrom("Turing", "Hopper");
        assertEquals(expectedSubgroups, subgroups);
    }

    @Test
    void createSubgroupContainingLatexAndUnicodeLastNames() {
        BibEntry bibEntry = new BibEntry().withField(StandardField.AUTHOR, "Kurt G{\\\"{o}}del");
        BibEntry godelEntry = new BibEntry().withField(StandardField.AUTHOR, "Kurt Gödel");
        var subgroup = new AutomaticPersonsGroup("", GroupHierarchyType.INDEPENDENT, StandardField.AUTHOR).createSubgroups(bibEntry);
        for (GroupTreeNode group : subgroup) { // There should only be one subgroup
            assertTrue(group.matches(godelEntry));
        }
    }
}
