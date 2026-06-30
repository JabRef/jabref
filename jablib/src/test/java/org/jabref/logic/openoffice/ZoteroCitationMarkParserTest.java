package org.jabref.logic.openoffice;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.IEEETranEntryType;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ZoteroCitationMarkParserTest {

    private record CSLTestCase(
            String itemData,
            EntryType entryType,
            Map<Field, String> fields,
            List<Field> absentFields) {
        private CSLTestCase(String itemData, EntryType entryType, Map<Field, String> fields) {
            this(itemData, entryType, fields, List.of());
        }
    }

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
    void parseCSLJSON(CSLTestCase testCase) {
        List<BibEntry> entries = ZoteroCitationMarkParser.parse("""
                ZOTERO_ITEM CSL_CITATION {"citationItems":[{"id":600,"itemData":{%s}}]} test1234
                """.formatted(testCase.itemData()));
        BibEntry entry = entries.getFirst();

        assertEquals(testCase.entryType(), entry.getType());
        for (Map.Entry<Field, String> expectedField : testCase.fields().entrySet()) {
            assertEquals(Optional.of(expectedField.getValue()), entry.getField(expectedField.getKey()));
        }
        for (Field absentField : testCase.absentFields()) {
            assertEquals(Optional.empty(), entry.getField(absentField));
        }
    }

    private static Stream<CSLTestCase> parseCSLJSON() {
        return Stream.of(
                new CSLTestCase(
                        """
                                "type":"article-magazine",
                                "title":"Magazine article"
                                """,
                        StandardEntryType.Article,
                        Map.of(
                                StandardField.TITLE, "Magazine article"
                        )
                ),
                new CSLTestCase(
                        """
                                "type":"article-newspaper",
                                "title":"Newspaper article"
                                """,
                        StandardEntryType.Article,
                        Map.of(
                                StandardField.TITLE, "Newspaper article"
                        )
                ),
                new CSLTestCase(
                        """
                                "type":"article-journal",
                                "title":"Special issue article",
                                "issue":"Special issue"
                                """,
                        StandardEntryType.Article,
                        Map.of(
                                StandardField.TITLE, "Special issue article",
                                StandardField.ISSUE, "Special issue"
                        )
                ),
                new CSLTestCase(
                        """
                                "type":"article-journal",
                                "title":"Article with electronic id",
                                "number":"e123"
                                """,
                        StandardEntryType.Article,
                        Map.of(
                                StandardField.TITLE, "Article with electronic id",
                                StandardField.EID, "e123"
                        ),
                        List.of(StandardField.NUMBER)
                ),
                new CSLTestCase(
                        """
                                "type":"book",
                                "title":"Book title",
                                "abstract":"Book abstract",
                                "call-number":"QA1",
                                "container-title":"Collected works",
                                "keyword":"fiction,series",
                                "language":"en",
                                "number-of-pages":"320",
                                "number-of-volumes":"4",
                                "issue":"3",
                                "publisher":"Book Press",
                                "publisher-place":"Berlin",
                                "ISBN":"978-1-23",
                                "edition":"2",
                                "collection-title":"Book series",
                                "title-short":"Short book",
                                "version":"1.0",
                                "volume":"4"
                                """,
                        StandardEntryType.Book,
                        Map.ofEntries(
                                Map.entry(StandardField.TITLE, "Book title"),
                                Map.entry(StandardField.ABSTRACT, "Book abstract"),
                                Map.entry(StandardField.LIBRARY, "QA1"),
                                Map.entry(StandardField.BOOKTITLE, "Collected works"),
                                Map.entry(StandardField.KEYWORDS, "fiction,series"),
                                Map.entry(StandardField.LANGUAGE, "en"),
                                Map.entry(StandardField.PAGETOTAL, "320"),
                                Map.entry(StandardField.VOLUMES, "4"),
                                Map.entry(StandardField.PUBLISHER, "Book Press"),
                                Map.entry(StandardField.LOCATION, "Berlin"),
                                Map.entry(StandardField.ISBN, "978-1-23"),
                                Map.entry(StandardField.EDITION, "2"),
                                Map.entry(StandardField.SERIES, "Book series"),
                                Map.entry(StandardField.SHORTTITLE, "Short book"),
                                Map.entry(StandardField.VERSION, "1.0"),
                                Map.entry(StandardField.VOLUME, "4")
                        ),
                        List.of(StandardField.ISSUE, StandardField.NUMBER)
                ),
                new CSLTestCase(
                        """
                                "type":"chapter",
                                "title":"Chapter title",
                                "chapter-number":"3",
                                "container-title":"Edited book",
                                "issue":"Special Issue",
                                "publisher":"Chapter Press",
                                "publisher-place":"London",
                                "page":"10-20",
                                "ISBN":"978-4-56"
                                """,
                        StandardEntryType.InBook,
                        Map.of(
                                StandardField.TITLE, "Chapter title",
                                StandardField.CHAPTER, "3",
                                StandardField.BOOKTITLE, "Edited book",
                                StandardField.PUBLISHER, "Chapter Press",
                                StandardField.LOCATION, "London",
                                StandardField.PAGES, "10-20",
                                StandardField.ISBN, "978-4-56"
                        ),
                        List.of(StandardField.ISSUE, StandardField.NUMBER)
                ),
                new CSLTestCase(
                        """
                                "type":"paper-conference",
                                "title":"Conference paper",
                                "container-title":"Proceedings title",
                                "event-place":"Paris",
                                "publisher":"ACM",
                                "issue":"4",
                                "page":"1-8",
                                "ISBN":"978-7-89"
                                """,
                        StandardEntryType.InProceedings,
                        Map.of(
                                StandardField.TITLE, "Conference paper",
                                StandardField.BOOKTITLE, "Proceedings title",
                                StandardField.VENUE, "Paris",
                                StandardField.ORGANIZATION, "ACM",
                                StandardField.NUMBER, "4",
                                StandardField.PAGES, "1-8",
                                StandardField.ISBN, "978-7-89"
                        )
                ),
                new CSLTestCase(
                        """
                                "type":"paper-conference",
                                "title":"Conference paper",
                                "issue":"Spring"
                                """,
                        StandardEntryType.InProceedings,
                        Map.of(
                                StandardField.TITLE, "Conference paper"
                        ),
                        List.of(StandardField.ISSUE, StandardField.NUMBER)
                ),
                new CSLTestCase(
                        """
                                "type":"manuscript",
                                "title":"Manuscript title",
                                "publisher":"Archive note"
                                """,
                        StandardEntryType.Unpublished,
                        Map.of(
                                StandardField.TITLE, "Manuscript title",
                                StandardField.HOWPUBLISHED, "Archive note"
                        ),
                        List.of(StandardField.PUBLISHER)
                ),
                new CSLTestCase(
                        """
                                "type":"patent",
                                "title":"Patent title",
                                "number":"ISRN PATENT-123"
                                """,
                        IEEETranEntryType.Patent,
                        Map.of(
                                StandardField.TITLE, "Patent title",
                                StandardField.NUMBER, "ISRN PATENT-123"
                        ),
                        List.of(StandardField.ISRN)
                ),
                new CSLTestCase(
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
                new CSLTestCase(
                        """
                                "type":"thesis",
                                "title":"Thesis title",
                                "publisher":"Test University",
                                "publisher-place":"Amsterdam"
                                """,
                        StandardEntryType.Thesis,
                        Map.of(
                                StandardField.TITLE, "Thesis title",
                                StandardField.LOCATION, "Amsterdam",
                                StandardField.INSTITUTION, "Test University"
                        )
                ),
                new CSLTestCase(
                        """
                                "type":"report",
                                "title":"Report title",
                                "publisher":"Research Institute",
                                "publisher-place":"Delft",
                                "number":"R-1",
                                "page":"1-30"
                                """,
                        StandardEntryType.Report,
                        Map.of(
                                StandardField.TITLE, "Report title",
                                StandardField.INSTITUTION, "Research Institute",
                                StandardField.LOCATION, "Delft",
                                StandardField.NUMBER, "R-1",
                                StandardField.PAGES, "1-30"
                        )
                )
        );
    }
}
