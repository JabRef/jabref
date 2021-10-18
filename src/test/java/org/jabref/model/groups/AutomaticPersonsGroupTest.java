package org.jabref.model.groups;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.*;

class AutomaticPersonsGroupTest {
    private static GroupTreeNode[] createPersonSubGroupFrom(String... lastNames) {
        return Arrays.stream(lastNames)
                     .map(lastName ->
                             new LastNameGroup(lastName, GroupHierarchyType.INDEPENDENT, StandardField.AUTHOR, lastName))
                     .map(GroupTreeNode::new)
                     .collect(Collectors.toList())
                     .toArray(GroupTreeNode[]::new);
    }

    @Test
    void createSubgroupsFromCommaSeparatedLastNames() {
        BibEntry bibEntry = new BibEntry().withField(StandardField.AUTHOR, "Turing, Alan and Hopper, Grace");
        var subgroups = new AutomaticPersonsGroup("", GroupHierarchyType.INDEPENDENT, StandardField.AUTHOR).createSubgroups(bibEntry);
        var expectedSubgroups = createPersonSubGroupFrom("Turing", "Hopper");
        assertThat(subgroups, containsInAnyOrder(expectedSubgroups));
    }

    @Test
    void createSubgroupsContainingSpaceSeparatedNames() {
        BibEntry bibEntry = new BibEntry().withField(StandardField.AUTHOR, "Alan Turing and Grace Hopper");
        var subgroups = new AutomaticPersonsGroup("", GroupHierarchyType.INDEPENDENT, StandardField.AUTHOR).createSubgroups(bibEntry);
        var expectedSubgroups = createPersonSubGroupFrom("Turing", "Hopper");
        assertThat(subgroups, containsInAnyOrder(expectedSubgroups));
    }

    @Test
    void createSubgroupFromLatex() {
        BibEntry bibEntry = new BibEntry().withField(StandardField.AUTHOR, "Kurt G{\\\"{o}}del");
        var subgroup = new AutomaticPersonsGroup("", GroupHierarchyType.INDEPENDENT, StandardField.AUTHOR).createSubgroups(bibEntry);
        var expectedSubgroup = createPersonSubGroupFrom("Gödel");
        assertThat(subgroup, contains(expectedSubgroup));
    }

    @Test
    void createSubgroupFromUnicode() {
        BibEntry bibEntry = new BibEntry().withField(StandardField.AUTHOR, "Kurt Gödel");
        var subgroup = new AutomaticPersonsGroup("", GroupHierarchyType.INDEPENDENT, StandardField.AUTHOR).createSubgroups(bibEntry);
        var expectedSubgroup = createPersonSubGroupFrom("Gödel");
        assertThat(subgroup, contains(expectedSubgroup));
    }

    @Test
    void testEqualsConditions() {
        BibEntry bibEntry = new BibEntry().withField(StandardField.AUTHOR, "Kurt Gödel");
        var subgroup = new AutomaticPersonsGroup("", GroupHierarchyType.INDEPENDENT, StandardField.AUTHOR).createSubgroups(bibEntry);
        var sameSubGroup = new AutomaticPersonsGroup("", GroupHierarchyType.INDEPENDENT, StandardField.AUTHOR).createSubgroups(bibEntry);
        var sameFieldsOfSubGroup = new AutomaticPersonsGroup("Test", GroupHierarchyType.REFINING, StandardField.AUTHOR).createSubgroups(bibEntry);

        boolean isEqualToNull = subgroup.equals(null);
        boolean isEqualWithAnotherClassObject = subgroup.equals("test");
        boolean isEqualWithMatchObject = subgroup.equals(sameSubGroup);
        boolean isEqualWithMatchFields = subgroup.equals(sameFieldsOfSubGroup);

        assertFalse(isEqualToNull);
        assertFalse(isEqualWithAnotherClassObject);
        assertTrue(isEqualWithMatchObject);
        assertTrue(isEqualWithMatchFields);
    }
}
