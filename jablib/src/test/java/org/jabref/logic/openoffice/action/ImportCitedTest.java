package org.jabref.logic.openoffice.action;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.logic.openoffice.ZoteroReferenceMark;
import org.jabref.logic.openoffice.oocsltext.CSLCitationType;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [utest->feat~import.libreoffice.cited-entries~1]
@NullMarked
class ImportCitedTest {

    private static final String ZOTERO_ONLY_CITATION_JSON = """
            {
              "citationID": "a1b2c3d4",
              "properties": {
                "formattedCitation": "(Smith, 2024)",
                "plainCitation": "(Smith, 2024)",
                "noteIndex": 0
              },
              "citationItems": [
                {
                  "id": 587,
                  "uris": ["http://zotero.org/users/123/items/ABCD1234"],
                  "itemData": {
                    "id": 587,
                    "type": "article-journal",
                    "title": "Native Zotero article",
                    "container-title": "Journal of reference management",
                    "volume": "12",
                    "issue": "3",
                    "page": "45-67",
                    "DOI": "10.1234/native.2024",
                    "author": [{"family": "Smith", "given": "Jane"}],
                    "issued": {"date-parts": [[2024, 3, 15]]}
                  },
                  "locator": "46",
                  "label": "page"
                }
              ],
              "schema": "https://github.com/citation-style-language/schema/raw/master/csl-citation.json"
            }
            """;

    @Test
    void ignoresNonZoteroReferenceMarks() {
        List<BibEntry> citedEntries = ImportCited.collectCitedEntries(
                List.of("JABREF_Smith2020 CID_1 abcd1234 NORMAL", "JR_cite1_1_Smith2020", "CSL_CITATION " + ZOTERO_ONLY_CITATION_JSON + " RNDabcdefghij"));

        assertEquals(List.of(), citedEntries);
    }

    @Test
    void keepsCitationKeyOfJabRefUri() {
        List<BibEntry> citedEntries = ImportCited.collectCitedEntries(List.of(citeWithJabRef(smithEntry())));

        assertEquals(List.of(Optional.of("Smith2020")), citationKeysOf(citedEntries));
    }

    @Test
    void clearsZoteroItemIdPlaceholderOfZoteroOnlyEntry() {
        List<BibEntry> citedEntries = ImportCited.collectCitedEntries(List.of(citeWithZotero("RNDabcdefghij")));

        assertEquals(List.of(Optional.empty()), citationKeysOf(citedEntries));
        assertEquals(List.of(Optional.of("Cited from Zotero")), titlesOf(citedEntries));
    }

    @Test
    void collectsNativeZoteroCitationJsonWithoutAssigningCitationKey() {
        List<BibEntry> citedEntries = ImportCited.collectCitedEntries(
                List.of(ZoteroReferenceMark.PREFIX + ZOTERO_ONLY_CITATION_JSON + " RNDabcdefghij"));

        assertEquals(List.of(Optional.empty()), citationKeysOf(citedEntries));
        assertEquals(List.of(Optional.of("Native Zotero article")), titlesOf(citedEntries));
        BibEntry entry = citedEntries.getFirst();
        assertEquals(Optional.of("Smith, Jane"), entry.getField(StandardField.AUTHOR));
        assertEquals(Optional.of("Journal of reference management"), entry.getField(StandardField.JOURNALTITLE));
        assertEquals(Optional.of("10.1234/native.2024"), entry.getField(StandardField.DOI));
    }

    @Test
    void keepsNativeKeylessZoteroItemAlongsideExplicitSmith2024Item() {
        BibEntry explicitKeyEntry = smithEntry().withCitationKey("Smith2024");

        List<BibEntry> citedEntries = ImportCited.collectCitedEntries(
                List.of(ZoteroReferenceMark.PREFIX + ZOTERO_ONLY_CITATION_JSON + " RNDabcdefghij",
                        citeWithJabRef(explicitKeyEntry)));

        assertEquals(List.of(Optional.empty(), Optional.of("Smith2024")), citationKeysOf(citedEntries));
        assertEquals(List.of(Optional.of("Native Zotero article"), Optional.of("Reference mark compatibility")), titlesOf(citedEntries));
    }

    @Test
    void collectsEveryEntryOfCitationGroup() {
        List<BibEntry> citedEntries = ImportCited.collectCitedEntries(List.of(citeWithJabRef(smithEntry(), jonesEntry())));

        assertEquals(List.of(Optional.of("Smith2020"), Optional.of("Jones2021")), citationKeysOf(citedEntries));
    }

    @Test
    void collectsEntryCitedTwiceOnce() {
        // The entry was edited between the two citations, so both reference marks name "Smith2020", but carry different data
        BibEntry editedSmithEntry = smithEntry().withField(StandardField.TITLE, "Reference mark compatibility, revised");

        List<BibEntry> citedEntries = ImportCited.collectCitedEntries(
                List.of(citeWithJabRef(smithEntry()), citeWithJabRef(2, jonesEntry()), citeWithJabRef(editedSmithEntry)));

        assertEquals(List.of(Optional.of("Reference mark compatibility"), Optional.of("Citations in word processors")), titlesOf(citedEntries));
    }

    @Test
    void collectsZoteroOnlyEntryCitedTwiceOnce() {
        List<BibEntry> citedEntries = ImportCited.collectCitedEntries(
                List.of(citeWithZotero("587", "Cited from Zotero", "RNDabcdefghij"),
                        citeWithZotero("587", "Cited from Zotero", "RNDklmnopqrst")));

        assertEquals(List.of(Optional.of("Cited from Zotero")), titlesOf(citedEntries));
    }

    @Test
    void keepsFirstDataWhenKeylessZoteroItemMetadataChanges() {
        List<BibEntry> citedEntries = ImportCited.collectCitedEntries(
                List.of(citeWithZotero("587", "Original title", "RNDabcdefghij"),
                        citeWithZotero("587", "Updated title", "RNDklmnopqrst")));

        assertEquals(List.of(Optional.of("Original title")), titlesOf(citedEntries));
    }

    @Test
    void keepsDistinctKeylessZoteroItemsWithIdenticalMetadata() {
        List<BibEntry> citedEntries = ImportCited.collectCitedEntries(
                List.of(citeWithZotero("587", "Same title", "RNDabcdefghij"),
                        citeWithZotero("588", "Same title", "RNDklmnopqrst")));

        assertEquals(List.of(Optional.of("Same title"), Optional.of("Same title")), titlesOf(citedEntries));
        assertEquals(List.of(Optional.empty(), Optional.empty()), citationKeysOf(citedEntries));
    }

    @Test
    void keepsKeylessZoteroItemsWithSameItemIdFromDifferentLibraries() {
        // Two co-authors cite from their own Zotero libraries, where the item ids are only unique per library
        List<BibEntry> citedEntries = ImportCited.collectCitedEntries(
                List.of(citeWithZotero("587", List.of("http://zotero.org/users/123/items/ABCD1234"), "From library of user 123", "RNDabcdefghij"),
                        citeWithZotero("587", List.of("http://zotero.org/users/456/items/WXYZ9876"), "From library of user 456", "RNDklmnopqrst")));

        assertEquals(List.of(Optional.of("From library of user 123"), Optional.of("From library of user 456")), titlesOf(citedEntries));
    }

    @Test
    void collectsKeylessZoteroItemWithSameUriOnceEvenIfItemIdsDiffer() {
        List<BibEntry> citedEntries = ImportCited.collectCitedEntries(
                List.of(citeWithZotero("587", List.of("http://zotero.org/users/123/items/ABCD1234"), "Cited from Zotero", "RNDabcdefghij"),
                        citeWithZotero("601", List.of("http://zotero.org/users/123/items/ABCD1234"), "Cited from Zotero", "RNDklmnopqrst")));

        assertEquals(List.of(Optional.of("Cited from Zotero")), titlesOf(citedEntries));
    }

    @Test
    void keepsDistinctZoteroUrisWithSameExplicitCitationKey() {
        List<BibEntry> citedEntries = ImportCited.collectCitedEntries(
                List.of(citeWithZotero("587", List.of("http://zotero.org/users/123/items/ABCD1234", "http://www.jabref.org/Smith2024"), "First source", "RNDabcdefghij"),
                        citeWithZotero("587", List.of("http://zotero.org/users/456/items/WXYZ9876", "http://www.jabref.org/Smith2024"), "Unrelated source", "RNDklmnopqrst")));

        assertEquals(List.of(Optional.of("Smith2024"), Optional.of("Smith2024")), citationKeysOf(citedEntries));
        assertEquals(List.of(Optional.of("First source"), Optional.of("Unrelated source")), titlesOf(citedEntries));
    }

    @Test
    void collectsZoteroItemWithSameUriOnceEvenIfIdsAndExplicitKeysDiffer() {
        List<BibEntry> citedEntries = ImportCited.collectCitedEntries(
                List.of(citeWithZotero("587", List.of("http://zotero.org/users/123/items/ABCD1234", "http://www.jabref.org/Smith2024"), "Original title", "RNDabcdefghij"),
                        citeWithZotero("601", List.of("http://zotero.org/users/123/items/ABCD1234", "http://www.jabref.org/Smith2024Revised"), "Updated title", "RNDklmnopqrst")));

        assertEquals(List.of(Optional.of("Smith2024")), citationKeysOf(citedEntries));
        assertEquals(List.of(Optional.of("Original title")), titlesOf(citedEntries));
    }

    @Test
    void keepsLaterJabRefCitationKeyForInitiallyKeylessZoteroItem() {
        List<BibEntry> citedEntries = ImportCited.collectCitedEntries(
                List.of(ZoteroReferenceMark.PREFIX + ZOTERO_ONLY_CITATION_JSON + " RNDabcdefghij",
                        citeWithJabRef(2, jonesEntry()),
                        citeWithZotero("601", List.of("http://zotero.org/users/123/items/ABCD1234", "http://www.jabref.org/Smith2024"), "Updated title", "RNDklmnopqrst")));

        assertEquals(List.of(Optional.of("Smith2024"), Optional.of("Jones2021")), citationKeysOf(citedEntries));
        assertEquals(List.of(Optional.of("Native Zotero article"), Optional.of("Citations in word processors")), titlesOf(citedEntries));
        assertEquals(Optional.of("Smith, Jane"), citedEntries.getFirst().getField(StandardField.AUTHOR));
        assertEquals(Optional.of("10.1234/native.2024"), citedEntries.getFirst().getField(StandardField.DOI));
    }

    @Test
    void keepsFirstExplicitCitationKeyWhenLaterRepresentationIsKeyless() {
        List<BibEntry> citedEntries = ImportCited.collectCitedEntries(
                List.of(citeWithZotero("601", List.of("http://zotero.org/users/123/items/ABCD1234", "http://www.jabref.org/Smith2024"), "First title", "RNDabcdefghij"),
                        ZoteroReferenceMark.PREFIX + ZOTERO_ONLY_CITATION_JSON + " RNDklmnopqrst"));

        assertEquals(List.of(Optional.of("Smith2024")), citationKeysOf(citedEntries));
        assertEquals(List.of(Optional.of("First title")), titlesOf(citedEntries));
    }

    @Test
    void keepsLaterItemDataCitationKeyForInitiallyKeylessZoteroItem() {
        String explicitKeyMark = ZoteroReferenceMark.PREFIX + """
                {
                  "citationItems": [{
                    "id": 601,
                    "uris": ["http://zotero.org/users/123/items/ABCD1234"],
                    "itemData": {
                      "id": 601,
                      "type": "article-journal",
                      "citation-key": "Smith2024",
                      "title": "Updated title"
                    }
                  }]
                }
                """ + " RNDklmnopqrst";

        List<BibEntry> citedEntries = ImportCited.collectCitedEntries(
                List.of(ZoteroReferenceMark.PREFIX + ZOTERO_ONLY_CITATION_JSON + " RNDabcdefghij", explicitKeyMark));

        assertEquals(List.of(Optional.of("Smith2024")), citationKeysOf(citedEntries));
        assertEquals(List.of(Optional.of("Native Zotero article")), titlesOf(citedEntries));
    }

    @Test
    void collectsKeylessZoteroItemWithoutUriOnceByItemId() {
        List<BibEntry> citedEntries = ImportCited.collectCitedEntries(
                List.of(citeWithZotero("587", List.of(), "Original title", "RNDabcdefghij"),
                        citeWithZotero("587", List.of(), "Updated title", "RNDklmnopqrst")));

        assertEquals(List.of(Optional.of("Original title")), titlesOf(citedEntries));
    }

    @Test
    void collectsZoteroItemWithoutUriOnceByNormalizedItemId() {
        String referenceMark = ZoteroReferenceMark.PREFIX + """
                {
                  "citationItems": [
                    {
                      "id": 587,
                      "itemData": {
                        "id": 587,
                        "type": "article-journal",
                        "title": "Original title"
                      }
                    },
                    {
                      "id": "Zotero-587",
                      "itemData": {
                        "id": "Zotero-587",
                        "type": "article-journal",
                        "title": "Updated title"
                      }
                    }
                  ]
                }
                """ + " RNDabcdefghij";

        List<BibEntry> citedEntries = ImportCited.collectCitedEntries(List.of(referenceMark));

        assertEquals(List.of(Optional.empty()), citationKeysOf(citedEntries));
        assertEquals(List.of(Optional.of("Original title")), titlesOf(citedEntries));
    }

    @Test
    void collectsEntryWithoutItemIdentityOnceByExplicitCitationKey() {
        String referenceMark = ZoteroReferenceMark.PREFIX + """
                {
                  "citationItems": [
                    {
                      "itemData": {
                        "type": "article-journal",
                        "citation-key": "Smith2024",
                        "title": "Original title"
                      }
                    },
                    {
                      "itemData": {
                        "type": "article-journal",
                        "citation-key": "Smith2024",
                        "title": "Updated title"
                      }
                    }
                  ]
                }
                """ + " RNDabcdefghij";

        List<BibEntry> citedEntries = ImportCited.collectCitedEntries(List.of(referenceMark));

        assertEquals(List.of(Optional.of("Smith2024")), citationKeysOf(citedEntries));
        assertEquals(List.of(Optional.of("Original title")), titlesOf(citedEntries));
    }

    @Test
    void keepsUnidentifiedEntriesWithIdenticalMetadata() {
        String referenceMark = ZoteroReferenceMark.PREFIX + """
                {
                  "citationItems": [
                    {
                      "itemData": {
                        "type": "article-journal",
                        "title": "Same title"
                      }
                    },
                    {
                      "itemData": {
                        "type": "article-journal",
                        "title": "Same title"
                      }
                    }
                  ]
                }
                """ + " RNDabcdefghij";

        List<BibEntry> citedEntries = ImportCited.collectCitedEntries(List.of(referenceMark));

        assertEquals(List.of(Optional.empty(), Optional.empty()), citationKeysOf(citedEntries));
        assertEquals(List.of(Optional.of("Same title"), Optional.of("Same title")), titlesOf(citedEntries));
    }

    @Test
    void collectsUnrelatedEntryWhoseCitationKeyExistsInLibrary() {
        BibDatabase library = new BibDatabase(List.of(smithEntry()));
        BibEntry unrelatedEntry = new BibEntry(StandardEntryType.Book)
                .withCitationKey("Smith2020")
                .withField(StandardField.TITLE, "An unrelated book");

        List<BibEntry> citedEntries = ImportCited.collectCitedEntries(List.of(citeWithJabRef(unrelatedEntry)));

        assertEquals(citationKeysOf(library.getEntries()), citationKeysOf(citedEntries));
        assertEquals(List.of(Optional.of("An unrelated book")), titlesOf(citedEntries));
        assertEquals(List.of(Optional.of("Reference mark compatibility")), titlesOf(library.getEntries()));
    }

    @Test
    void ignoresReferenceMarkWithMalformedJson() {
        List<BibEntry> citedEntries = ImportCited.collectCitedEntries(
                List.of(ZoteroReferenceMark.PREFIX + "{\"citationItems\": [ RNDabcdefghij", ZoteroReferenceMark.PREFIX + "{this is not json} RNDabcdefghij"));

        assertEquals(List.of(), citedEntries);
    }

    @Test
    void continuesCollectingAfterCitationMarkWithNullStructuralValues() {
        String markWithNullValues = ZoteroReferenceMark.PREFIX + """
                {
                  "citationItems": [
                    null,
                    {
                      "id": 900,
                      "uris": [null],
                      "itemData": null
                    },
                    {
                      "id": 901,
                      "uris": null,
                      "itemData": {
                        "id": 901,
                        "type": "article-journal",
                        "title": "Null URIs"
                      }
                    }
                  ]
                }
                """ + " RNDabcdefghij";

        List<BibEntry> citedEntries = ImportCited.collectCitedEntries(List.of(markWithNullValues, citeWithZotero("RNDklmnopqrst")));

        assertEquals(List.of(Optional.of("Null URIs"), Optional.of("Cited from Zotero")), titlesOf(citedEntries));
    }

    @Test
    void collectsNothingFromDocumentWithoutReferenceMarks() {
        List<BibEntry> citedEntries = ImportCited.collectCitedEntries(List.of());

        assertEquals(List.of(), citedEntries);
    }

    private static BibEntry smithEntry() {
        return new BibEntry(StandardEntryType.Article)
                .withCitationKey("Smith2020")
                .withField(StandardField.AUTHOR, "Smith, Jane")
                .withField(StandardField.TITLE, "Reference mark compatibility");
    }

    private static BibEntry jonesEntry() {
        return new BibEntry(StandardEntryType.Book)
                .withCitationKey("Jones2021")
                .withField(StandardField.AUTHOR, "Jones, Alex")
                .withField(StandardField.TITLE, "Citations in word processors");
    }

    /// Builds the name of a Zotero-compatible reference mark as JabRef writes it when citing the given entries.
    private static String citeWithJabRef(BibEntry... entries) {
        return citeWithJabRef(1, entries);
    }

    private static String citeWithJabRef(int firstItemId, BibEntry... entries) {
        List<BibEntry> citedEntries = List.of(entries);
        List<String> citationKeys = citedEntries.stream()
                                                .map(entry -> entry.getCitationKey().orElseThrow())
                                                .toList();
        return ZoteroReferenceMark.buildReferenceMark(
                citedEntries,
                citationKeys,
                citationKeys.stream().map(_ -> 0).toList(),
                firstItemId,
                CSLCitationType.NORMAL,
                new BibDatabaseContext(new BibDatabase(citedEntries)),
                new BibEntryTypesManager(),
                Map.of()).getName();
    }

    /// Builds the name of a reference mark as Zotero writes it: the item carries a Zotero URI, but no JabRef URI and no citation key.
    private static String citeWithZotero(String randomSuffix) {
        return citeWithZotero("587", "Cited from Zotero", randomSuffix);
    }

    private static String citeWithZotero(String itemId, String title, String randomSuffix) {
        return citeWithZotero(itemId, List.of("http://zotero.org/users/123/items/" + itemId), title, randomSuffix);
    }

    private static String citeWithZotero(String itemId, List<String> uris, String title, String randomSuffix) {
        String urisJson = uris.stream()
                              .map(uri -> "\"" + uri + "\"")
                              .collect(Collectors.joining(", ", "[", "]"));
        String citationJson = """
                {"citationItems": [{"id": %s, "uris": %s,
                "itemData": {"id": %s, "type": "article-journal", "title": "%s", "author": [{"family": "Doe", "given": "John"}]}}]}
                """.formatted(itemId, urisJson, itemId, title);
        return ZoteroReferenceMark.PREFIX + citationJson + " " + randomSuffix;
    }

    private static List<Optional<String>> citationKeysOf(List<BibEntry> entries) {
        return entries.stream()
                      .map(BibEntry::getCitationKey)
                      .toList();
    }

    private static List<Optional<String>> titlesOf(List<BibEntry> entries) {
        return entries.stream()
                      .map(entry -> entry.getField(StandardField.TITLE))
                      .toList();
    }
}
