package org.jabref.migrations;

import java.util.Collections;

import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MergeReviewIntoCommentActionMigrationTest {

    private MergeReviewIntoCommentMigration action;
    private BibEntry entry;
    private BibEntry expectedEntry;

    @BeforeEach
    public void setUp() {
        action = new MergeReviewIntoCommentMigration();
        entry = createMinimalBibEntry();
        expectedEntry = createMinimalBibEntry();
    }

    @Test
    public void noFields() {
        ParserResult actualParserResult = new ParserResult(Collections.singletonList(entry));

        action.performMigration(actualParserResult);

        assertEquals(entry, actualParserResult.getDatabase().getEntryByCitationKey("Entry1").get());
    }

    @Test
    public void reviewField() {
        entry.setField(StandardField.REVIEW, "My Review");
        ParserResult actualParserResult = new ParserResult(Collections.singletonList(entry));

        expectedEntry.setField(StandardField.COMMENT, "My Review");

        action.performMigration(actualParserResult);

        assertEquals(expectedEntry, actualParserResult.getDatabase().getEntryByCitationKey("Entry1").get());
    }

    @Test
    public void commentField() {
        entry.setField(StandardField.COMMENT, "My Comment");
        ParserResult actualParserResult = new ParserResult(Collections.singletonList(entry));

        action.performMigration(actualParserResult);

        assertEquals(entry, actualParserResult.getDatabase().getEntryByCitationKey("Entry1").get());
    }

    @Test
    public void multiLineReviewField() {
        String commentString = "My Review\n\nSecond Paragraph\n\nThird Paragraph";

        entry.setField(StandardField.REVIEW, commentString);
        ParserResult actualParserResult = new ParserResult(Collections.singletonList(entry));

        expectedEntry.setField(StandardField.COMMENT, commentString);

        action.performMigration(actualParserResult);

        assertEquals(expectedEntry, actualParserResult.getDatabase().getEntryByCitationKey("Entry1").get());
    }

    @Test
    @Disabled("Re-enable if the MergeReviewIntoCommentMigration.mergeCommentFieldIfPresent() does not block and wait for user input.")
    public void reviewAndCommentField() {
        entry.setField(StandardField.REVIEW, "My Review");
        entry.setField(StandardField.COMMENT, "My Comment");

        ParserResult actualParserResult = new ParserResult(Collections.singletonList(entry));

        expectedEntry.setField(StandardField.COMMENT, "My Comment\n" + Localization.lang("Review") + ":\nMy Review");

        action.performMigration(actualParserResult);

        assertEquals(expectedEntry, actualParserResult.getDatabase().getEntryByCitationKey("Entry1").get());
    }

    private BibEntry createMinimalBibEntry() {
        BibEntry entry = new BibEntry();
        entry.setCitationKey("Entry1");
        entry.setField(StandardField.TITLE, "A random entry!");
        entry.setField(StandardField.AUTHOR, "JabRef DEVELOPERS");
        return entry;
    }
}
