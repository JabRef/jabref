package net.sf.jabref.model.groups;

import java.util.Optional;

import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ExplicitGroupTest {


    @Test
     public void testToStringSimple() {
        ExplicitGroup group = new ExplicitGroup("myExplicitGroup", GroupHierarchyType.INDEPENDENT, ',');
        assertEquals("ExplicitGroup:myExplicitGroup;0;", group.toString());
    }

    @Test
    public void toStringDoesNotWriteAssignedEntries() {
        ExplicitGroup group = new ExplicitGroup("myExplicitGroup", GroupHierarchyType.INCLUDING, ',');
        group.add(new BibEntry());
        assertEquals("ExplicitGroup:myExplicitGroup;2;", group.toString());
    }

    @Test
    public void testEntryStorageSingleGroups() {
        BibEntry entry = new BibEntry();
        ExplicitGroup group = new ExplicitGroup("myExplicitGroup", GroupHierarchyType.INDEPENDENT, ',');
        group.add(entry);
        assertEquals(Optional.of("myExplicitGroup"), entry.getField(FieldName.GROUPS));
    }

    @Test
    public void testEntryStorageTwoGroups() {
        BibEntry entry = new BibEntry();
        ExplicitGroup group = new ExplicitGroup("myExplicitGroup", GroupHierarchyType.INDEPENDENT, ',');
        ExplicitGroup group2 = new ExplicitGroup("myExplicitGroup2", GroupHierarchyType.INDEPENDENT, ',');
        group.add(entry);
        group2.add(entry);
        assertEquals(Optional.of("myExplicitGroup, myExplicitGroup2"), entry.getField(FieldName.GROUPS));
    }

    @Test
    public void testGroupStorageAlreadyInGroup() throws Exception {
        ExplicitGroup group = new ExplicitGroup("myExplicitGroup", GroupHierarchyType.INDEPENDENT, ',');
        BibEntry entry = new BibEntry().withField(FieldName.GROUPS, "myExplicitGroup");

        group.add(entry);

        assertEquals(Optional.of("myExplicitGroup"), entry.getField(FieldName.GROUPS));
    }

}
