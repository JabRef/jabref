package org.jabref.logic.openoffice;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.model.entry.BibEntry;
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

    String zoteroCitation = "ZOTERO_ITEM CSL_CITATION " + JOURNAL_ARTICLE_CSL_JSON + " test1234";

    @Test
    void parseJournalArticle() {
        List<BibEntry> entries = ZoteroCitationMarkParser.parse(zoteroCitation);
        BibEntry entry = entries.getFirst();

        assertEquals(StandardEntryType.Article, entry.getType());
        assertEquals(Optional.of("Zotero-587"), entry.getCitationKey());
    }

    @ParameterizedTest
    @MethodSource
    void parseJournalArticleField(StandardField field, String expectedValue) {
        List<BibEntry> entries = ZoteroCitationMarkParser.parse(zoteroCitation);
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
    void parseCSLJSON(String itemData, StandardEntryType expectedEntryType, Map<StandardField, String> expectedFields) {
        List<BibEntry> entries = ZoteroCitationMarkParser.parse("""
                ZOTERO_ITEM CSL_CITATION {"citationItems":[{"id":600,"itemData":{%s}}]} test1234
                """.formatted(itemData));
        BibEntry entry = entries.getFirst();

        assertEquals(expectedEntryType, entry.getType());
        for (Map.Entry<StandardField, String> expectedField : expectedFields.entrySet()) {
            assertEquals(Optional.of(expectedField.getValue()), entry.getField(expectedField.getKey()));
        }
    }

    private static Stream<Arguments> parseCSLJSON() {
        return Stream.of(
                Arguments.of(
                        """
                                "type":"article-magazine",
                                "title":"Magazine article"
                                """,
                        StandardEntryType.Article,
                        Map.of(
                                StandardField.TITLE, "Magazine article"
                        )
                ),
                Arguments.of(
                        """
                                "type":"article-newspaper",
                                "title":"Newspaper article"
                                """,
                        StandardEntryType.Article,
                        Map.of(
                                StandardField.TITLE, "Newspaper article"
                        )
                ),
                Arguments.of(
                        """
                                "type":"book",
                                "title":"Book title",
                                "publisher":"Book Press",
                                "publisher-place":"Berlin",
                                "ISBN":"978-1-23",
                                "edition":"2",
                                "collection-title":"Book series",
                                "volume":"4"
                                """,
                        StandardEntryType.Book,
                        Map.of(
                                StandardField.TITLE, "Book title",
                                StandardField.PUBLISHER, "Book Press",
                                StandardField.LOCATION, "Berlin",
                                StandardField.ISBN, "978-1-23",
                                StandardField.EDITION, "2",
                                StandardField.SERIES, "Book series",
                                StandardField.VOLUME, "4"
                        )
                ),
                Arguments.of(
                        """
                                "type":"chapter",
                                "title":"Chapter title",
                                "container-title":"Edited book",
                                "publisher":"Chapter Press",
                                "publisher-place":"London",
                                "page":"10-20",
                                "ISBN":"978-4-56"
                                """,
                        StandardEntryType.InCollection,
                        Map.of(
                                StandardField.TITLE, "Chapter title",
                                StandardField.BOOKTITLE, "Edited book",
                                StandardField.PUBLISHER, "Chapter Press",
                                StandardField.LOCATION, "London",
                                StandardField.PAGES, "10-20",
                                StandardField.ISBN, "978-4-56"
                        )
                ),
                Arguments.of(
                        """
                                "type":"paper-conference",
                                "title":"Conference paper",
                                "container-title":"Proceedings title",
                                "event-title":"Conference event",
                                "event-place":"Paris",
                                "publisher":"ACM",
                                "page":"1-8",
                                "ISBN":"978-7-89"
                                """,
                        StandardEntryType.InProceedings,
                        Map.of(
                                StandardField.TITLE, "Conference paper",
                                StandardField.BOOKTITLE, "Proceedings title",
                                StandardField.EVENTTITLE, "Conference event",
                                StandardField.LOCATION, "Paris",
                                StandardField.PUBLISHER, "ACM",
                                StandardField.PAGES, "1-8",
                                StandardField.ISBN, "978-7-89"
                        )
                ),
                Arguments.of(
                        """
                                "type":"webpage",
                                "title":"Web page",
                                "URL":"https://example.org",
                                "publisher":"Example Org"
                                """,
                        StandardEntryType.Online,
                        Map.of(
                                StandardField.TITLE, "Web page",
                                StandardField.URL, "https://example.org",
                                StandardField.ORGANIZATION, "Example Org"
                        )
                ),
                Arguments.of(
                        """
                                "type":"thesis",
                                "title":"Thesis title",
                                "publisher":"Test University",
                                "publisher-place":"Amsterdam",
                                "genre":"PhD thesis"
                                """,
                        StandardEntryType.Thesis,
                        Map.of(
                                StandardField.TITLE, "Thesis title",
                                StandardField.INSTITUTION, "Test University",
                                StandardField.LOCATION, "Amsterdam",
                                StandardField.TYPE, "PhD thesis"
                        )
                ),
                Arguments.of(
                        """
                                "type":"report",
                                "title":"Report title",
                                "publisher":"Research Institute",
                                "publisher-place":"Delft",
                                "genre":"Technical report",
                                "issue":"R-1",
                                "page":"1-30"
                                """,
                        StandardEntryType.Report,
                        Map.of(
                                StandardField.TITLE, "Report title",
                                StandardField.INSTITUTION, "Research Institute",
                                StandardField.LOCATION, "Delft",
                                StandardField.TYPE, "Technical report",
                                StandardField.NUMBER, "R-1",
                                StandardField.PAGES, "1-30"
                        )
                )
        );
    }
}
