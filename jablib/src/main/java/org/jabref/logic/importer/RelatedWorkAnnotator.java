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
        // Field name pattern requested by maintainers: comment-<username>
        String fieldName = "comment-" + username;

        // Resolve to a Field implementation (UserSpecificCommentField via FieldFactory)
        Field commentField = FieldFactory.parseField(fieldName);

        // Format the new summary block
        String formattedBlock = "[" + citingPaperKey + "]: " + summarySentence.trim();

        // Retrieve any existing content for this user-specific comment field
        Optional<String> existing = entry.getField(commentField);

        // Append the new summary, separated by a single blank line if content already exists
        String newValue = existing
                .map(old -> old.strip() + "\n\n" + formattedBlock)
                .orElse(formattedBlock);

        // Write it back into the BibEntry
        entry.setField(commentField, newValue);
    }
}
