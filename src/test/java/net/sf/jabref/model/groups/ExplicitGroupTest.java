package net.sf.jabref.model.groups;

import java.util.Optional;

import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ExplicitGroupTest {

    private ExplicitGroup group;
    private ExplicitGroup group2;

    private BibEntry emptyEntry;

    @Before
    public void setUp() {
        group = new ExplicitGroup("myExplicitGroup", GroupHierarchyType.INDEPENDENT, ',');
        group2 = new ExplicitGroup("myExplicitGroup2", GroupHierarchyType.INCLUDING, ',');
        emptyEntry = new BibEntry();
    }

    @Test
     public void testToStringSimple() {
        assertEquals("ExplicitGroup:myExplicitGroup;0;", group.toString());
    }

    @Test
    public void toStringDoesNotWriteAssignedEntries() {
        group.add(emptyEntry);

        assertEquals("ExplicitGroup:myExplicitGroup;0;", group.toString());
    }

    @Test
    public void addSingleGroupToBibEntrySuccessfullyIfEmptyBefore() {
        group.add(emptyEntry);

        assertEquals(Optional.of("myExplicitGroup"), emptyEntry.getField(FieldName.GROUPS));
    }

    @Test
    public void addTwoGroupsToBibEntrySuccessfully() {
        group.add(emptyEntry);
        group2.add(emptyEntry);

        assertEquals(Optional.of("myExplicitGroup, myExplicitGroup2"), emptyEntry.getField(FieldName.GROUPS));
    }

    @Test
    public void noDuplicateStoredIfAlreadyInGroup() throws Exception {
        emptyEntry.setField(FieldName.GROUPS, "myExplicitGroup");
        group.add(emptyEntry);

        assertEquals(Optional.of("myExplicitGroup"), emptyEntry.getField(FieldName.GROUPS));
    }

}
