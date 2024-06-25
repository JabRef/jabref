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
    private ExplicitGroup group3;
    private BibEntry entry;

    @BeforeEach
    void setUp() {
        group = new ExplicitGroup("myExplicitGroup", GroupHierarchyType.INDEPENDENT, ',');
        group2 = new ExplicitGroup("myExplicitGroup", GroupHierarchyType.INDEPENDENT, ',');  // Same as group
        group3 = new ExplicitGroup("anotherGroup", GroupHierarchyType.INDEPENDENT, ',');  // Different from group
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


    @Test
    void testEqualsWithSelf() {
        assertTrue(group.equals(group));
        ExplicitGroup.printCoverage();
    }

    @Test
    void testEqualsWithSameAttributes() {
        assertTrue(group.equals(group2));
        ExplicitGroup.printCoverage();
    }

    @Test
    void testEqualsWithDifferentAttributes() {
        assertFalse(group.equals(group3));
        ExplicitGroup.printCoverage();
    }

    @Test
    void testEqualsWithNull() {
        assertFalse(group.equals(null));
        ExplicitGroup.printCoverage();
    }

    @Test
    void testEqualsWithDifferentType() {
        assertFalse(group.equals(new Object()));
        ExplicitGroup.printCoverage();
    }


    @Test
    void testEqualsWithDifferentHierarchicalContext() {
        ExplicitGroup groupDiffContext = new ExplicitGroup("myExplicitGroup", GroupHierarchyType.INCLUDING, ',');
        assertFalse(group.equals(groupDiffContext));
        ExplicitGroup.printCoverage();
    }

    @Test
    void testEqualsWithDifferentIconName() {
        group.setIconName("icon1");
        ExplicitGroup groupDiffIcon = new ExplicitGroup("myExplicitGroup", GroupHierarchyType.INDEPENDENT, ',');
        groupDiffIcon.setIconName("icon2");
        assertFalse(group.equals(groupDiffIcon));
        ExplicitGroup.printCoverage();
    }

    @Test
    void testEqualsWithDifferentDescription() {
        group.setDescription("Description1");
        ExplicitGroup groupDiffDescription = new ExplicitGroup("myExplicitGroup", GroupHierarchyType.INDEPENDENT, ',');
        groupDiffDescription.setDescription("Description2");
        assertFalse(group.equals(groupDiffDescription));
        ExplicitGroup.printCoverage();
    }

    @Test
    void testEqualsWithDifferentColor() {
        group.setColor("red");
        ExplicitGroup groupDiffColor = new ExplicitGroup("myExplicitGroup", GroupHierarchyType.INDEPENDENT, ',');
        groupDiffColor.setColor("blue");
        assertFalse(group.equals(groupDiffColor));
        ExplicitGroup.printCoverage();
    }

    @Test
    void testEqualsWithDifferentExpandedState() {
        group.setExpanded(true);
        ExplicitGroup groupDiffExpanded = new ExplicitGroup("myExplicitGroup", GroupHierarchyType.INDEPENDENT, ',');
        groupDiffExpanded.setExpanded(false);
        assertFalse(group.equals(groupDiffExpanded));
        ExplicitGroup.printCoverage();
    }

    @Test
    void testEqualsWithDifferentLegacyEntryKeys() {
        group.addLegacyEntryKey("key1");
        ExplicitGroup groupDiffKeys = new ExplicitGroup("myExplicitGroup", GroupHierarchyType.INDEPENDENT, ',');
        groupDiffKeys.addLegacyEntryKey("key2");
        assertFalse(group.equals(groupDiffKeys));
        ExplicitGroup.printCoverage();
    }

}
