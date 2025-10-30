package org.jabref.logic.importer;

import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RelatedWorkAnnotatorTest {

    @Test
    public void firstAppendCreatesFieldWithOneBlock() {
        BibEntry entry = new BibEntry();

        RelatedWorkAnnotator.appendSummaryToEntry(
                entry,
                "yourusername",
                "LunaOstos_2024",
                "Colombia is a middle-income country with a population of approximately 50 million."
        );

        Field commentField = FieldFactory.parseField("comment-yourusername");
        Optional<String> value = entry.getField(commentField);

        String expected =
                "[LunaOstos_2024]: Colombia is a middle-income country with a population of approximately 50 million.";

        assertEquals(expected, value.orElseThrow());
    }

    @Test
    void secondAppendAddsBlankLineAndSecondBlock() {
        BibEntry entry = new BibEntry();
        RelatedWorkAnnotator.appendSummaryToEntry(
                entry,
                "koppor",
                "LunaOstos_2024",
                "Colombia is a middle-income country with a population of approximately 50 million."
        );

        RelatedWorkAnnotator.appendSummaryToEntry(
                entry,
                "koppor",
                "CIA_2021",
                "Colombia has ~50 million people."
        );

        Optional<String> value = entry.getField(FieldFactory.parseField("comment-koppor"));

        String expected =
                "[LunaOstos_2024]: Colombia is a middle-income country with a population of approximately 50 million.\n\n" +
                        "[CIA_2021]: Colombia has ~50 million people.";

        assertEquals(expected, value.orElseThrow());
    }
}
