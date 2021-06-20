package org.jabref.migrations;

import java.util.List;
import java.util.stream.Stream;

import org.jabref.logic.importer.ParserResult;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpecialFieldsToSeparateFieldsTest {

    @ParameterizedTest
    @MethodSource("provideKeywordFieldPairs")
    public void migrateToCorrectField(SpecialField field, String fieldInKeyword, BibEntry expected) {
        BibEntry entry = new BibEntry().withField(StandardField.KEYWORDS, fieldInKeyword);

        new SpecialFieldsToSeparateFields(',').performMigration(new ParserResult(List.of(entry)));

        assertEquals(expected, entry);
    }

    @Test
    public void noKewordToMigrate() {
        BibEntry entry = new BibEntry().withField(StandardField.AUTHOR, "JabRef")
                                       .withField(StandardField.KEYWORDS, "tdd");
        BibEntry expected = new BibEntry().withField(StandardField.AUTHOR, "JabRef")
                                          .withField(StandardField.KEYWORDS, "tdd");

        new SpecialFieldsToSeparateFields(',').performMigration(new ParserResult(List.of(entry)));

        assertEquals(expected, entry);
    }

    @Test
    public void migrateMultipleSpecialFields() {
        BibEntry entry = new BibEntry().withField(StandardField.AUTHOR, "JabRef")
                                       .withField(StandardField.KEYWORDS, "printed, prio1");
        BibEntry expected = new BibEntry().withField(StandardField.AUTHOR, "JabRef")
                                          .withField(SpecialField.PRINTED, "printed")
                                          .withField(SpecialField.PRIORITY, "prio1");

        new SpecialFieldsToSeparateFields(',').performMigration(new ParserResult(List.of(entry)));

        assertEquals(expected, entry);
    }

    @Test
    public void migrateSpecialFieldsMixedWithKeyword() {
        BibEntry entry = new BibEntry().withField(StandardField.AUTHOR, "JabRef")
                                       .withField(StandardField.KEYWORDS, "tdd, prio1, SE");

        BibEntry expected = new BibEntry().withField(StandardField.AUTHOR, "JabRef")
                                          .withField(StandardField.KEYWORDS, "tdd, SE")
                                          .withField(SpecialField.PRIORITY, "prio1");

        new SpecialFieldsToSeparateFields(',').performMigration(new ParserResult(List.of(entry)));

        assertEquals(expected, entry);
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

