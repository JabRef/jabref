package net.sf.jabref.groups.structure;

import org.junit.Test;

import static org.junit.Assert.*;

public class AllEntriesGroupTest {

    @Test
    public void testToString() {
        assertEquals("AllEntriesGroup:", new AllEntriesGroup().toString());
    }

}