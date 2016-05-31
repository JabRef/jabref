package net.sf.jabref.logic.msbib;

import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibtexEntryTypes;

import org.junit.Test;

import static org.junit.Assert.*;

public class MSBibEntryTest {
    @Test
    public void defaultType() {
        BibEntry entry = new BibEntry("unknowntype");

        assertEquals("Misc", new MSBibEntry(entry).getMSBibSourceType(entry));
        assertEquals(BibtexEntryTypes.MISC, new MSBibEntry(entry).mapMSBibToBibtexType("unknowntype"));
    }
}