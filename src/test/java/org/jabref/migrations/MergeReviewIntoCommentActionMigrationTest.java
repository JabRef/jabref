package org.jabref.migrations;

import java.util.Collections;

import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MergeReviewIntoCommentActionMigrationTest {
    private MergeReviewIntoCommentMigration action;

    @BeforeEach
    public void setUp() {
        action = new MergeReviewIntoCommentMigration();
    }

    @Test
    public void noFields() {
        BibEntry entry = createMinimalBibEntry();
        ParserResult actualParserResult = new ParserResult(Collections.singletonList(entry));

        action.performMigration(actualParserResult);

        assertEquals(entry, actualParserResult.getDatabase().getEntryByKey("Entry1").get());
    }

    @Test
    public void reviewField() {
        BibEntry actualEntry = createMinimalBibEntry();
        actualEntry.setField(FieldName.REVIEW, "My Review");
        ParserResult actualParserResult = new ParserResult(Collections.singletonList(actualEntry));

        BibEntry expectedEntry = createMinimalBibEntry();
        expectedEntry.setField(FieldName.COMMENT, "My Review");

        action.performMigration(actualParserResult);

        assertEquals(expectedEntry, actualParserResult.getDatabase().getEntryByKey("Entry1").get());
    }

    @Test
    public void commentField() {
        BibEntry entry = createMinimalBibEntry();
        entry.setField(FieldName.COMMENT, "My Comment");
        ParserResult actualParserResult = new ParserResult(Collections.singletonList(entry));

        action.performMigration(actualParserResult);

        assertEquals(entry, actualParserResult.getDatabase().getEntryByKey("Entry1").get());
    }

    @Test
    public void multiLineReviewField() {
        String commentString = "My Review\n\nSecond Paragraph\n\nThird Paragraph";

        BibEntry actualEntry = createMinimalBibEntry();
        actualEntry.setField(FieldName.REVIEW, commentString);
        ParserResult actualParserResult = new ParserResult(Collections.singletonList(actualEntry));

        BibEntry expectedEntry = createMinimalBibEntry();
        expectedEntry.setField(FieldName.COMMENT, commentString);

        action.performMigration(actualParserResult);

        assertEquals(expectedEntry, actualParserResult.getDatabase().getEntryByKey("Entry1").get());
    }

    @Test
    @Disabled("Re-enable if the MergeReviewIntoCommentMigration.mergeCommentFieldIfPresent() does not block and wait for user input.")
    public void reviewAndCommentField() {
        BibEntry actualEntry = createMinimalBibEntry();
        actualEntry.setField(FieldName.REVIEW, "My Review");
        actualEntry.setField(FieldName.COMMENT, "My Comment");

        ParserResult actualParserResult = new ParserResult(Collections.singletonList(actualEntry));

        BibEntry expectedEntry = createMinimalBibEntry();
        expectedEntry.setField(FieldName.COMMENT, "My Comment\n" + Localization.lang("Review") + ":\nMy Review");

        action.performMigration(actualParserResult);

        assertEquals(expectedEntry, actualParserResult.getDatabase().getEntryByKey("Entry1").get());
    }

    private BibEntry createMinimalBibEntry() {
        BibEntry entry = new BibEntry();
        entry.setCiteKey("Entry1");
        entry.setField(FieldName.TITLE, "A random entry!");
        entry.setField(FieldName.AUTHOR, "JabRef DEVELOPERS");
        return entry;
    }
}
