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

/**
 * Integration-style test for RelatedWorkHarvester.
 * Verifies that summaries are appended correctly to {@code comment-<username>} fields.
 */
public class RelatedWorkHarvesterTest {

    @Test
    void harvestAndAnnotateAppendsSummaries() {
        // --- Arrange ---
        String text = """
            1.3 Related work: environmental and social assessments of chocolate production
            Existing environmental LCAs include Italian chocolate production (Vesce et al., 2016),
            and Italian dark chocolate production from cradle-to-grave (Bianchi et al. 2021).
            """;

        // Create sample bibliography
        BibEntry vesce = new BibEntry();
        vesce.setCitationKey("Vesce2016");
        vesce.setField(StandardField.AUTHOR, "Vesce, A.");
        vesce.setField(StandardField.YEAR, "2016");

        BibEntry bianchi = new BibEntry();
        bianchi.setCitationKey("Bianchi2021");
        bianchi.setField(StandardField.AUTHOR, "Bianchi, L.");
        bianchi.setField(StandardField.YEAR, "2021");

        List<BibEntry> bibliography = new ArrayList<>();
        bibliography.add(vesce);
        bibliography.add(bianchi);

        // Track updates made by the harvester
        List<BibEntry> updated = new ArrayList<>();

        RelatedWorkExtractor extractor = new HeuristicRelatedWorkExtractor();
        RelatedWorkHarvester harvester = new RelatedWorkHarvester(extractor);

        // --- Act ---
        harvester.harvestAndAnnotate(
                "koppor",
                "LunaOstos_2024",
                text,
                bibliography,
                updated::add // record updates
        );

        // --- Assert ---
        assertEquals(2, updated.size());

        Field commentField = FieldFactory.parseField("comment-koppor");
        for (BibEntry entry : updated) {
            String value = entry.getField(commentField).orElse("");
            assertTrue(value.startsWith("[LunaOstos_2024]:"));
            assertTrue(value.length() > 30);
        }

        String vesceComment = vesce.getField(commentField).orElse("");
        String bianchiComment = bianchi.getField(commentField).orElse("");

        assertTrue(!vesceComment.equals(bianchiComment));
        assertTrue(vesceComment.contains("Vesce"));
        assertTrue(bianchiComment.contains("Bianchi"));
    }

    @Test
    void appendsWhenFieldAlreadyHasContent() {
        // --- Arrange ---
        Field commentField = FieldFactory.parseField("comment-koppor");

        BibEntry existing = new BibEntry();
        existing.setCitationKey("Vesce2016");
        existing.setField(StandardField.AUTHOR, "Vesce, A.");
        existing.setField(StandardField.YEAR, "2016");
        existing.setField(commentField, "[OldPaper]: Old summary.");

        List<BibEntry> bibliography = List.of(existing);

        String text = "1.3 Related work\nExisting LCAs include Italian chocolate production (Vesce et al., 2016).";

        RelatedWorkExtractor extractor = new HeuristicRelatedWorkExtractor();
        RelatedWorkHarvester harvester = new RelatedWorkHarvester(extractor);

        List<BibEntry> updated = new ArrayList<>();

        // --- Act ---
        harvester.harvestAndAnnotate("koppor", "NewPaper", text, bibliography, updated::add);

        // --- Assert ---
        String comment = existing.getField(commentField).orElse("");
        assertTrue(comment.contains("[OldPaper]: Old summary."));
        assertTrue(comment.contains("[NewPaper]:"));
        assertTrue(comment.contains("\n\n")); // blank line separator
    }
}
