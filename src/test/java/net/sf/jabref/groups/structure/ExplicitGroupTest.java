package net.sf.jabref.groups.structure;

import net.sf.jabref.*;
import net.sf.jabref.model.entry.IdGenerator;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibtexEntryTypes;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ExplicitGroupTest {

    @Before
    public void setUp() {
        Globals.prefs = JabRefPreferences.getInstance();
    }

    @Test
     public void testToStringSimple() {
        ExplicitGroup group = new ExplicitGroup("myExplicitGroup", GroupHierarchyType.INDEPENDENT);
        assertEquals("ExplicitGroup:myExplicitGroup;0;", group.toString());
    }

    @Test
    public void testToStringComplex() {
        ExplicitGroup group = new ExplicitGroup("myExplicitGroup", GroupHierarchyType.INCLUDING);
        group.addEntry(makeBibtexEntry());
        assertEquals("ExplicitGroup:myExplicitGroup;2;shields01;", group.toString());
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
