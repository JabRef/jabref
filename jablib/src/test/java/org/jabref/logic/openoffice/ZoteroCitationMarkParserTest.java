package org.jabref.logic.openoffice;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Month;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ZoteroCitationMarkParserTest {

    private static final String JOURNAL_ARTICLE_CSL_JSON = """
            {
              "citationItems": [
                {
                  "id": 587,
                  "itemData": {
                    "type": "article-journal",
                    "container-title": "Journal of Test",
                    "DOI": "1234567",
                    "issue": "2",
                    "page": "6-7",
                    "title": "19",
                    "volume": "1",
                    "author": [
                      {
                        "family": "Doe",
                        "given": "John"
                      },
                      {
                        "family": "Zhang",
                        "given": "Hancong"
                      }
                    ],
                    "issued": {
                      "date-parts": [
                        [
                          "2026",
                          1,
                          1
                        ]
                      ]
                    }
                  }
                }
              ]
            }
            """;

    String ZoteroCitation = "ZOTERO_ITEM CSL_CITATION " + JOURNAL_ARTICLE_CSL_JSON + " test1234";

    @Test
    void parseJournalArticle() {
        List<BibEntry> entries = ZoteroCitationMarkParser.parse(ZoteroCitation);
        BibEntry entry = entries.getFirst();

        assertEquals(StandardEntryType.Article, entry.getType());
        assertEquals(Optional.of("Zotero-587"), entry.getCitationKey());
    }

    @ParameterizedTest
    @MethodSource
    void parseJournalArticleField(StandardField field, String expectedValue) {
        List<BibEntry> entries = ZoteroCitationMarkParser.parse(ZoteroCitation);
        BibEntry entry = entries.getFirst();

        assertEquals(Optional.of(expectedValue), entry.getField(field));
    }

    private static Stream<Arguments> parseJournalArticleField() {
        return Stream.of(
                Arguments.of(StandardField.TITLE, "19"),
                Arguments.of(StandardField.AUTHOR, "Doe, John and Zhang, Hancong"),
                Arguments.of(StandardField.JOURNALTITLE, "Journal of Test"),
                Arguments.of(StandardField.VOLUME, "1"),
                Arguments.of(StandardField.NUMBER, "2"),
                Arguments.of(StandardField.PAGES, "6-7"),
                Arguments.of(StandardField.DOI, "1234567"),
                Arguments.of(StandardField.YEAR, "2026")
        );
    }

    @ParameterizedTest
    @MethodSource
    void parseDateParts(String dateParts, Optional<String> expectedYear, Optional<Month> expectedMonth, Optional<String> expectedDay) {
        final String testJSON = """
                ZOTERO_ITEM CSL_CITATION {"citationItems":[{"id":588,"itemData":{"type":"article-journal","title":"Date test","issued":{"date-parts":[[%s]]}}}]} RNDabcd3456
                """;

        List<BibEntry> entries = ZoteroCitationMarkParser.parse(testJSON.formatted(dateParts));
        BibEntry entry = entries.getFirst();

        assertEquals(expectedYear, entry.getField(StandardField.YEAR));
        assertEquals(expectedMonth, entry.getMonth());
        assertEquals(expectedDay, entry.getField(StandardField.DAY));
    }

    private static Stream<Arguments> parseDateParts() {
        return Stream.of(
                Arguments.of("8", Optional.empty(), Optional.of(Month.AUGUST), Optional.empty()),
                Arguments.of("15", Optional.empty(), Optional.empty(), Optional.of("15")),
                Arguments.of("8,15", Optional.empty(), Optional.of(Month.AUGUST), Optional.of("15")),
                Arguments.of("\"2001\",10", Optional.of("2001"), Optional.of(Month.OCTOBER), Optional.empty()),
                Arguments.of("\"2001\",15", Optional.of("2001"), Optional.empty(), Optional.of("15"))
        );
    }
}
