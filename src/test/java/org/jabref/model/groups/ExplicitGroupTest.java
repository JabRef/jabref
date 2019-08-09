package org.jabref.model.groups;

import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.InternalField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExplicitGroupTest {

    private ExplicitGroup group;
    private ExplicitGroup group2;

    private BibEntry entry;

    @BeforeEach
    public void setUp() {
        group = new ExplicitGroup("myExplicitGroup", GroupHierarchyType.INDEPENDENT, ',');
        group2 = new ExplicitGroup("myExplicitGroup2", GroupHierarchyType.INCLUDING, ',');
        entry = new BibEntry();
    }

    @Test
    public void addSingleGroupToEmptyBibEntryChangesGroupsField() {
        group.add(entry);
        assertEquals(Optional.of("myExplicitGroup"), entry.getField(InternalField.GROUPS));
    }

    @Test
    public void addSingleGroupToNonemptyBibEntryAppendsToGroupsField() {
        entry.setField(InternalField.GROUPS, "some thing");
        group.add(entry);
        assertEquals(Optional.of("some thing, myExplicitGroup"), entry.getField(InternalField.GROUPS));
    }

    @Test
    public void addTwoGroupsToBibEntryChangesGroupsField() {
        group.add(entry);
        group2.add(entry);
        assertEquals(Optional.of("myExplicitGroup, myExplicitGroup2"), entry.getField(InternalField.GROUPS));
    }

    @Test
    public void addDuplicateGroupDoesNotChangeGroupsField() throws Exception {
        entry.setField(InternalField.GROUPS, "myExplicitGroup");
        group.add(entry);
        assertEquals(Optional.of("myExplicitGroup"), entry.getField(InternalField.GROUPS));
    }

    @Test
    // For https://github.com/JabRef/jabref/issues/2334
    public void removeDoesNotChangeFieldIfContainsNameAsPart() throws Exception {
        entry.setField(InternalField.GROUPS, "myExplicitGroup_alternative");
        group.remove(entry);
        assertEquals(Optional.of("myExplicitGroup_alternative"), entry.getField(InternalField.GROUPS));
    }

    @Test
    // For https://github.com/JabRef/jabref/issues/2334
    public void removeDoesNotChangeFieldIfContainsNameAsWord() throws Exception {
        entry.setField(InternalField.GROUPS, "myExplicitGroup alternative");
        group.remove(entry);

        assertEquals(Optional.of("myExplicitGroup alternative"), entry.getField(InternalField.GROUPS));
    }

    @Test
    // For https://github.com/JabRef/jabref/issues/1873
    public void containsOnlyMatchesCompletePhraseWithWhitespace() throws Exception {
        entry.setField(InternalField.GROUPS, "myExplicitGroup b");
        assertFalse(group.contains(entry));
    }

    @Test
    // For https://github.com/JabRef/jabref/issues/1873
    public void containsOnlyMatchesCompletePhraseWithSlash() throws Exception {
        entry.setField(InternalField.GROUPS, "myExplicitGroup/b");

        assertFalse(group.contains(entry));
    }

    @Test
    // For https://github.com/JabRef/jabref/issues/2394
    public void containsMatchesPhraseWithBrackets() throws Exception {
        entry.setField(InternalField.GROUPS, "[aa] Subgroup1");
        ExplicitGroup explicitGroup = new ExplicitGroup("[aa] Subgroup1", GroupHierarchyType.INCLUDING, ',');

        assertTrue(explicitGroup.contains(entry));
    }
}
