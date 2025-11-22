package org.jabref.logic.importer;

import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;

public class RelatedWorkAnnotator {

    public static void appendSummaryToEntry(
            BibEntry entry,
            String username,
            String citingPaperKey,
            String summarySentence
    ) {
        String fieldName = "comment-" + username;
        Field commentField = FieldFactory.parseField(fieldName);

        String cleaned = summarySentence.strip();
        if (!cleaned.endsWith(".")) {
            cleaned = cleaned + ".";
        }
        String formattedBlock = "[" + citingPaperKey + "]: " + cleaned;

        Optional<String> existing = entry.getField(commentField);
        String newValue = existing
                .map(old -> old.strip() + "\n\n" + formattedBlock)
                .orElse(formattedBlock);

        entry.setField(commentField, newValue);
    }
}
