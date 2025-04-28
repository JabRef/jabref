package org.jabref.model.entry;

import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.EntryTypeFactory;
import org.jabref.model.entry.types.IEEETranEntryType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EntryTypeFactoryTest {

    @Test
    void parseEntryTypePatent() {
        EntryType patent = IEEETranEntryType.Patent;
        assertEquals(patent, EntryTypeFactory.parse("patent"));
    }
}
