package org.jabref.migrations;

import java.util.List;
import java.util.stream.Stream;

import org.jabref.logic.importer.ParserResult;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpecialFieldsToSeparateFieldsTest {

    private BibEntry entry;
    private BibEntry expectedEntry;

    @BeforeEach
    void setUp(){
      entry = new BibEntry();
      expectedEntry = new BibEntry();
    }

    @AfterEach
    void tearDown(){
      entry = null;
      expectedEntry = null;
    }

    @ParameterizedTest
    @MethodSource("provideKeywordFieldPairs")
    public void migrateToCorrectField(SpecialField field, String fieldInKeyword, BibEntry expected) {
        entry = new BibEntry().withField(StandardField.KEYWORDS, fieldInKeyword);

        new SpecialFieldsToSeparateFields(',').performMigration(new ParserResult(List.of(entry)));

        assertEquals(expected, entry);
    }

    @Test
    public void noKewordToMigrate() {
        entry.setField(StandardField.AUTHOR, "JabRef");
        entry.setField(StandardField.KEYWORDS, "tdd");
        expectedEntry.setField(StandardField.AUTHOR,"JabRef");
        expectedEntry.setField(StandardField.KEYWORDS, "tdd");

        new SpecialFieldsToSeparateFields(',').performMigration(new ParserResult(List.of(entry)));

        assertEquals(expectedEntry, entry);
    }

    @Test
    public void migrateMultipleSpecialFields() {
        entry.setField(StandardField.AUTHOR, "JabRef");
        entry.setField(StandardField.KEYWORDS, "printed, prio1");
        expectedEntry.setField(StandardField.AUTHOR, "JabRef");
        expectedEntry.setField(SpecialField.PRINTED, "printed");
        expectedEntry.setField(SpecialField.PRIORITY, "prio1");

        new SpecialFieldsToSeparateFields(',').performMigration(new ParserResult(List.of(entry)));

        assertEquals(expectedEntry, entry);
    }

    @Test
    public void migrateSpecialFieldsMixedWithKeyword() {
        entry.setField(StandardField.AUTHOR, "JabRef");
        entry.setField(StandardField.KEYWORDS, "tdd, prio1, SE");
        expectedEntry.setField(StandardField.AUTHOR, "JabRef");
        expectedEntry.setField(StandardField.KEYWORDS, "tdd, SE");
        expectedEntry.setField(SpecialField.PRIORITY, "prio1");

        new SpecialFieldsToSeparateFields(',').performMigration(new ParserResult(List.of(entry)));

        assertEquals(expectedEntry, entry);
    }

    private static Stream<Arguments> provideKeywordFieldPairs() {
        return Stream.of(
                Arguments.of(
                        SpecialField.PRINTED, "printed", new BibEntry().withField(SpecialField.PRINTED, "printed")
                ),
                Arguments.of(
                        SpecialField.PRIORITY, "prio1", new BibEntry().withField(SpecialField.PRIORITY, "prio1")
                ),
                Arguments.of(
                        SpecialField.QUALITY, "qualityAssured", new BibEntry().withField(SpecialField.QUALITY, "qualityAssured")
                ),
                Arguments.of(
                        SpecialField.RANKING, "rank2", new BibEntry().withField(SpecialField.RANKING, "rank2")
                ),
                Arguments.of(
                        SpecialField.READ_STATUS, "skimmed", new BibEntry().withField(SpecialField.READ_STATUS, "skimmed")
                ),
                Arguments.of(
                        SpecialField.RELEVANCE, "relevant", new BibEntry().withField(SpecialField.RELEVANCE, "relevant")
                )
        );
    }
}

