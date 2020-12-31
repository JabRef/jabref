package org.jabref.model.groups;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.strings.LatexToUnicodeAdapter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AutomaticPersonsGroupTest {
    private static Set<GroupTreeNode> createPersonSubGroupFrom(String... lastNames) {
        return Arrays.stream(lastNames)
                     .distinct()
                     .map(LatexToUnicodeAdapter::format)
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
        var subgroups = new AutomaticPersonsGroup("", GroupHierarchyType.INDEPENDENT, StandardField.AUTHOR).createSubgroups(bibEntry);
        var expectedSubgroups = createPersonSubGroupFrom("Turing", "Hopper");
        assertEquals(expectedSubgroups, subgroups);
    }

    @Test
    void createSubgroupFromLatexAndCheckForUnicodeLastName() {
        BibEntry bibEntry = new BibEntry().withField(StandardField.AUTHOR, "Kurt G{\\\"{o}}del");
        var subgroup = new AutomaticPersonsGroup("", GroupHierarchyType.INDEPENDENT, StandardField.AUTHOR).createSubgroups(bibEntry);
        var expectedSubgroup = createPersonSubGroupFrom("Gödel");
        assertEquals(expectedSubgroup, subgroup);
    }

    @Test
    void createSubgroupFromUnicodeAndCheckForLatexLastName() {
        BibEntry bibEntry = new BibEntry().withField(StandardField.AUTHOR, "Kurt Gödel");
        var subgroup = new AutomaticPersonsGroup("", GroupHierarchyType.INDEPENDENT, StandardField.AUTHOR).createSubgroups(bibEntry);
        var expectedSubgroup = createPersonSubGroupFrom("G{\\\"{o}}del");
        assertEquals(expectedSubgroup, subgroup);
    }
}
