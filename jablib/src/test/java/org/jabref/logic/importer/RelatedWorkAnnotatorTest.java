package org.jabref.logic.importer;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class RelatedWorkAnnotatorTest {

    @Test
    void appendsToEmpty() {
        BibEntry e = new BibEntry();
        e.setCitationKey("X");

        RelatedWorkAnnotator.appendSummaryToEntry(
                e,
                "koppor",
                "LunaOstos_2024",
                "Colombia is a middle-income country"
        );

        Field commentField = FieldFactory.parseField("comment-koppor");
        String v = e.getField(commentField).orElse("");

        assertTrue(v.startsWith("[LunaOstos_2024]: Colombia is a middle-income country"));
        assertTrue(v.endsWith("."));
    }

    @Test
    void appendsWithBlankLine() {
        BibEntry e = new BibEntry();
        e.setCitationKey("X");

        Field commentField = FieldFactory.parseField("comment-koppor");
        e.setField(commentField, "Existing text.");

        RelatedWorkAnnotator.appendSummaryToEntry(
                e,
                "koppor",
                "LunaOstos_2024",
                "New sentence"
        );

        String v = e.getField(commentField).orElse("");
        assertTrue(v.contains("Existing text.\n\n[LunaOstos_2024]: New sentence."));
    }
}
