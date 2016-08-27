package net.sf.jabref.model.groups;

import net.sf.jabref.model.ParseException;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibtexEntryTypes;
import net.sf.jabref.model.entry.IdGenerator;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ExplicitGroupTest {


    @Test
     public void testToStringSimple() throws ParseException {
        ExplicitGroup group = new ExplicitGroup("myExplicitGroup", GroupHierarchyType.INDEPENDENT, ", ");
        assertEquals("ExplicitGroup:myExplicitGroup;0;", group.toString());
    }

    @Test
    public void toStringDoesNotWriteAssignedEntries() throws ParseException {
        ExplicitGroup group = new ExplicitGroup("myExplicitGroup", GroupHierarchyType.INCLUDING, ", ");
        group.add(makeBibtexEntry());
        assertEquals("ExplicitGroup:myExplicitGroup;2;", group.toString());
    }

    public BibEntry makeBibtexEntry() {
        BibEntry e = new BibEntry(IdGenerator.next(), BibtexEntryTypes.INCOLLECTION.getName());
        e.setField("title", "Marine finfish larviculture in Europe");
        e.setField("bibtexkey", "shields01");
        e.setField("year", "2001");
        e.setField("author", "Kevin Shields");
        return e;
    }

}
