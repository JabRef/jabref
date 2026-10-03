package org.jabref.model.entry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BibEntryTrimLeftTest {

    @Test
    void trimLeftRemovesLeadingWhitespaceFromParsedSerializationAndComments() {
        BibEntry entry = new BibEntry();
        // include leading and trailing whitespace
        entry.setParsedSerialization("  \t\n  Some serialization  \n\t  ");
        entry.setCommentsBeforeEntry("   % a comment   \n");

        // sanity
        assertNotNull(entry.getParsedSerialization());
        assertNotNull(entry.getUserComments());

        entry.trimLeft();

        assertEquals("Some serialization  \n\t  ", entry.getParsedSerialization());
        assertEquals("% a comment   \n", entry.getUserComments());
    }

    @Test
    void trimLeftIsIdempotent() {
        BibEntry entry = new BibEntry();
        entry.setParsedSerialization("   Text   ");
        entry.setCommentsBeforeEntry("   % c   ");

        entry.trimLeft();
        String afterFirst = entry.getParsedSerialization();
        String commentsAfterFirst = entry.getUserComments();

        entry.trimLeft();
        assertEquals(afterFirst, entry.getParsedSerialization());
        assertEquals(commentsAfterFirst, entry.getUserComments());
    }

    @Test
    void trimLeftWithEmptyStringsKeepsEmpty() {
        BibEntry entry = new BibEntry();
        entry.setParsedSerialization("");
        entry.setCommentsBeforeEntry("");

        entry.trimLeft();

        assertEquals("", entry.getParsedSerialization());
        assertEquals("", entry.getUserComments());
    }
}
