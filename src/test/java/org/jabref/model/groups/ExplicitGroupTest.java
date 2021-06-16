package org.jabref.model.groups;

import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExplicitGroupTest {

    private ExplicitGroup group;
    private ExplicitGroup group2;

    private BibEntry entry;

    @BeforeEach
    void setUp() {
        group = new ExplicitGroup("myExplicitGroup", GroupHierarchyType.INDEPENDENT, ',');
        group2 = new ExplicitGroup("myExplicitGroup2", GroupHierarchyType.INCLUDING, ',');
        entry = new BibEntry();
    }

    @Test
    void addSingleGroupToEmptyBibEntryChangesGroupsField() {
        group.add(entry);
        assertEquals(Optional.of("myExplicitGroup"), entry.getField(StandardField.GROUPS));
    }

    @Test
    void addSingleGroupToNonemptyBibEntryAppendsToGroupsField() {
        entry.setField(StandardField.GROUPS, "some thing");
        group.add(entry);
        assertEquals(Optional.of("some thing, myExplicitGroup"), entry.getField(StandardField.GROUPS));
    }

    @Test
    void addTwoGroupsToBibEntryChangesGroupsField() {
        group.add(entry);
        group2.add(entry);
        assertEquals(Optional.of("myExplicitGroup, myExplicitGroup2"), entry.getField(StandardField.GROUPS));
    }

    @Test
    void addDuplicateGroupDoesNotChangeGroupsField() throws Exception {
        entry.setField(StandardField.GROUPS, "myExplicitGroup");
        group.add(entry);
        assertEquals(Optional.of("myExplicitGroup"), entry.getField(StandardField.GROUPS));
    }

    @Test
        // For https://github.com/JabRef/jabref/issues/2334
    void removeDoesNotChangeFieldIfContainsNameAsPart() throws Exception {
        entry.setField(StandardField.GROUPS, "myExplicitGroup_alternative");
        group.remove(entry);
        assertEquals(Optional.of("myExplicitGroup_alternative"), entry.getField(StandardField.GROUPS));
    }

    @Test
        // For https://github.com/JabRef/jabref/issues/2334
    void removeDoesNotChangeFieldIfContainsNameAsWord() throws Exception {
        entry.setField(StandardField.GROUPS, "myExplicitGroup alternative");
        group.remove(entry);

        assertEquals(Optional.of("myExplicitGroup alternative"), entry.getField(StandardField.GROUPS));
    }

    @Test
        // For https://github.com/JabRef/jabref/issues/1873
    void containsOnlyMatchesCompletePhraseWithWhitespace() throws Exception {
        entry.setField(StandardField.GROUPS, "myExplicitGroup b");
        assertFalse(group.contains(entry));
    }

    @Test
        // For https://github.com/JabRef/jabref/issues/1873
    void containsOnlyMatchesCompletePhraseWithSlash() throws Exception {
        entry.setField(StandardField.GROUPS, "myExplicitGroup/b");

        assertFalse(group.contains(entry));
    }

    @Test
        // For https://github.com/JabRef/jabref/issues/2394
    void containsMatchesPhraseWithBrackets() throws Exception {
        entry.setField(StandardField.GROUPS, "[aa] Subgroup1");
        ExplicitGroup explicitGroup = new ExplicitGroup("[aa] Subgroup1", GroupHierarchyType.INCLUDING, ',');

        assertTrue(explicitGroup.contains(entry));
    }
}
