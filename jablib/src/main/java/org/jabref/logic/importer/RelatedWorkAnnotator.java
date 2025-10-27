package org.jabref.logic.importer;

import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;

/**
 * Enriches
 * a
 * BibEntry
 * with
 * contextual
 * summaries
 * extracted
 * from
 * a
 * paper's
 * "Related
 * Work"
 * /
 * literature
 * review
 * section.
 * For
 * each
 * reference
 * mentioned
 * in
 * the
 * related-work
 * section,
 * this
 * class
 * adds
 * a
 * descriptive
 * note
 * to
 * the
 * corresponding
 * BibEntry
 * field
 * in
 * the
 * format:
 * [CitingPaperKey]:
 * Summary
 * sentence.
 * The
 * note
 * is
 * stored
 * under
 * a
 * user-specific
 * field
 * name
 * like
 * "comment-<username>".
 * If
 * the
 * field
 * already
 * contains
 * text,
 * the
 * new
 * block
 * is
 * appended
 * after
 * a
 * blank
 * line.
 */
public class RelatedWorkAnnotator {

    /**
     * Appends
     * a
     * related-work
     * summary
     * to
     * the
     * given
     * BibEntry.
     *
     * @param entry           The
     *                        BibEntry
     *                        being
     *                        annotated
     * @param username        The
     *                        username
     *                        to
     *                        build
     *                        the
     *                        comment
     *                        field
     *                        (e.g.,
     *                        "koppor"
     *                        →
     *                        "comment-koppor")
     * @param citingPaperKey  The
     *                        citation
     *                        key
     *                        of
     *                        the
     *                        source
     *                        paper
     *                        (e.g.,
     *                        "LunaOstos_2024")
     * @param summarySentence The
     *                        extracted
     *                        related-work
     *                        summary
     *                        text
     */
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
