package org.jabref.logic.importer.relatedwork;

import java.util.ArrayList;
import java.util.List;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RelatedWorkHarvesterTest {
    private BibEntry entry(String key, String author, String year) {
        BibEntry b = new BibEntry();
        b.setCitationKey(key);
        b.setField(StandardField.AUTHOR, author);
        b.setField(StandardField.YEAR, year);
        return b;
    }

    @Test
    void harvestEndToEnd() {
        String text = """
                1.4 Related work
                Population estimates vary across sources (CIA, 2021). See also (Nash 2022).
                """;

        List<BibEntry> lib = new ArrayList<>();
        lib.add(entry("Agency2021", "Central Intelligence Agency", "2021"));
        lib.add(entry("Nash2022", "Nash, T.", "2022"));

        HeuristicRelatedWorkExtractor ex = new HeuristicRelatedWorkExtractor();
        RelatedWorkHarvester harvester = new RelatedWorkHarvester(ex);

        // Add/update function with explicit braces (Checkstyle NeedBraces)
        int updated = harvester.harvestAndAnnotateCount(
                "koppor",
                "LunaOstos_2024",
                text,
                lib,
                b -> {
                    if (!lib.contains(b)) {
                        lib.add(b);
                    }
                }
        );

        assertEquals(2, updated);

        Field commentField = FieldFactory.parseField("comment-koppor");
        boolean agencyAnnotated = lib.stream().anyMatch(b ->
                "Agency2021".equals(b.getCitationKey().orElse(""))
                        && b.getField(commentField).orElse("").contains("[LunaOstos_2024]:")
        );
        boolean nashAnnotated = lib.stream().anyMatch(b ->
                "Nash2022".equals(b.getCitationKey().orElse(""))
                        && b.getField(commentField).orElse("").contains("[LunaOstos_2024]:")
        );

        assertTrue(agencyAnnotated);
        assertTrue(nashAnnotated);
    }
}
