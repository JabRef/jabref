package org.jabref.migrations;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.logic.importer.ParserResult;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.SpecialFieldValue;
import org.jabref.model.entry.field.StandardField;
import org.jabref.preferences.PreferencesService;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

class SpecialFieldsToSeparateFieldsTest {

    @ParameterizedTest
    @MethodSource("provideKeywordFieldPairs")
    public void migrateToCorrectField(SpecialField expectedField, String fieldInKeyword) {
        BibEntry entry = new BibEntry().withField(StandardField.AUTHOR, "JabRef")
                .withField(StandardField.KEYWORDS, fieldInKeyword);

        new SpecialFieldsToSeparateFields(',').performMigration(new ParserResult(List.of(entry)));

        assertEquals(fieldInKeyword, entry.getField(expectedField).get());
    }

    @Test
    public void noKewordToMigrate() {
        BibEntry entry = new BibEntry().withField(StandardField.AUTHOR, "JabRef")
                                       .withField(StandardField.KEYWORDS, "tdd");
        new SpecialFieldsToSeparateFields(',').performMigration(new ParserResult(List.of(entry)));

        assertEquals("JabRef", entry.getField(StandardField.AUTHOR).get());
        assertEquals("tdd", entry.getField(StandardField.KEYWORDS).get());
    }

    @Test
    public void migrateMultipleSpecialFields() {
        BibEntry entry = new BibEntry().withField(StandardField.AUTHOR, "JabRef")
                                       .withField(StandardField.KEYWORDS, "printed, prio1");
        new SpecialFieldsToSeparateFields(',').performMigration(new ParserResult(List.of(entry)));

        assertEquals("prio1", entry.getField(SpecialField.PRIORITY).get());
        assertEquals("printed", entry.getField(SpecialField.PRINTED).get());

    }

    @Test
    public void migrateSpecialFieldsMixedWithKeyword() {
        BibEntry entry = new BibEntry().withField(StandardField.AUTHOR, "JabRef")
                                       .withField(StandardField.KEYWORDS, "tdd, prio1, SE");
        new SpecialFieldsToSeparateFields(',').performMigration(new ParserResult(List.of(entry)));

        assertEquals("prio1", entry.getField(SpecialField.PRIORITY).get());
        assertEquals("tdd, SE", entry.getField(StandardField.KEYWORDS).get());

    }

    private static Stream<Arguments> provideKeywordFieldPairs() {
        return Stream.of(
                Arguments.of(
                        SpecialField.PRINTED, "printed"
                ),
                Arguments.of(
                    SpecialField.PRIORITY, "prio1"
                ),
                Arguments.of(
                    SpecialField.QUALITY, "qualityAssured"
                ),
                Arguments.of(
                        SpecialField.RANKING, "rank2"
                ),
                Arguments.of(
                        SpecialField.READ_STATUS, "skimmed"
                ),
                Arguments.of(
                        SpecialField.RELEVANCE, "relevant"
                )
        );
    }
}

