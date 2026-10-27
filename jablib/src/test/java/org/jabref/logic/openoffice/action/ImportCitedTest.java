package org.jabref.logic.openoffice.action;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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
            {"citationItems": [{"id": 587, "uris": ["http://zotero.org/users/123/items/ABCD1234"], \
            "itemData": {"id": 587, "type": "article-journal", "title": "Cited from Zotero", "author": [{"family": "Doe", "given": "John"}]}}]}""";

    @Test
    void ignoresNonZoteroReferenceMarks() {
        ImportCited.CitedEntries citedEntries = ImportCited.collectCitedEntries(
                List.of("JABREF_Smith2020 CID_1 abcd1234 NORMAL", "JR_cite1_1_Smith2020", "CSL_CITATION " + ZOTERO_ONLY_CITATION_JSON + " RNDabcdefghij"),
                new BibDatabase());

        assertEquals(new ImportCited.CitedEntries(List.of(), List.of()), citedEntries);
    }

    @Test
    void keepsCitationKeyOfJabRefUri() {
        ImportCited.CitedEntries citedEntries = ImportCited.collectCitedEntries(
                List.of(citeWithJabRef(smithEntry())),
                new BibDatabase());

        assertEquals(List.of(Optional.of("Smith2020")), citationKeysOf(citedEntries.newEntries()));
    }

    @Test
    void clearsZoteroItemIdPlaceholderOfZoteroOnlyEntry() {
        ImportCited.CitedEntries citedEntries = ImportCited.collectCitedEntries(
                List.of(citeWithZotero("RNDabcdefghij")),
                new BibDatabase());

        assertEquals(List.of(Optional.empty()), citationKeysOf(citedEntries.newEntries()));
        assertEquals(List.of(Optional.of("Cited from Zotero")), titlesOf(citedEntries.newEntries()));
    }

    @Test
    void collectsEveryEntryOfCitationGroup() {
        ImportCited.CitedEntries citedEntries = ImportCited.collectCitedEntries(
                List.of(citeWithJabRef(smithEntry(), jonesEntry())),
                new BibDatabase());

        assertEquals(List.of(Optional.of("Smith2020"), Optional.of("Jones2021")), citationKeysOf(citedEntries.newEntries()));
    }

    @Test
    void collectsEntryCitedTwiceOnce() {
        // The entry was edited between the two citations, so both reference marks name "Smith2020", but carry different data
        BibEntry editedSmithEntry = smithEntry().withField(StandardField.TITLE, "Reference mark compatibility, revised");

        ImportCited.CitedEntries citedEntries = ImportCited.collectCitedEntries(
                List.of(citeWithJabRef(smithEntry()), citeWithJabRef(jonesEntry()), citeWithJabRef(editedSmithEntry)),
                new BibDatabase());

        assertEquals(List.of(Optional.of("Reference mark compatibility"), Optional.of("Citations in word processors")), titlesOf(citedEntries.newEntries()));
    }

    @Test
    void collectsZoteroOnlyEntryCitedTwiceOnce() {
        ImportCited.CitedEntries citedEntries = ImportCited.collectCitedEntries(
                List.of(citeWithZotero("587", "Cited from Zotero", "RNDabcdefghij"),
                        citeWithZotero("587", "Cited from Zotero", "RNDklmnopqrst")),
                new BibDatabase());

        assertEquals(List.of(Optional.of("Cited from Zotero")), titlesOf(citedEntries.newEntries()));
    }

    @Test
    void keepsFirstDataWhenKeylessZoteroItemMetadataChanges() {
        ImportCited.CitedEntries citedEntries = ImportCited.collectCitedEntries(
                List.of(citeWithZotero("587", "Original title", "RNDabcdefghij"),
                        citeWithZotero("587", "Updated title", "RNDklmnopqrst")),
                new BibDatabase());

        assertEquals(List.of(Optional.of("Original title")), titlesOf(citedEntries.newEntries()));
    }

    @Test
    void keepsDistinctKeylessZoteroItemsWithIdenticalMetadata() {
        ImportCited.CitedEntries citedEntries = ImportCited.collectCitedEntries(
                List.of(citeWithZotero("587", "Same title", "RNDabcdefghij"),
                        citeWithZotero("588", "Same title", "RNDklmnopqrst")),
                new BibDatabase());

        assertEquals(List.of(Optional.of("Same title"), Optional.of("Same title")), titlesOf(citedEntries.newEntries()));
        assertEquals(List.of(Optional.empty(), Optional.empty()), citationKeysOf(citedEntries.newEntries()));
    }

    @Test
    void skipsEntryWhoseCitationKeyIsInLibrary() {
        BibDatabase library = new BibDatabase(List.of(smithEntry()));

        ImportCited.CitedEntries citedEntries = ImportCited.collectCitedEntries(
                List.of(citeWithJabRef(smithEntry()), citeWithZotero("RNDabcdefghij")),
                library);

        assertEquals(List.of(Optional.of("Cited from Zotero")), titlesOf(citedEntries.newEntries()));
        assertEquals(List.of(Optional.of("Smith2020")), citationKeysOf(citedEntries.entriesAlreadyInLibrary()));
    }

    @Test
    void ignoresReferenceMarkWithMalformedJson() {
        ImportCited.CitedEntries citedEntries = ImportCited.collectCitedEntries(
                List.of(ZoteroReferenceMark.PREFIX + "{\"citationItems\": [ RNDabcdefghij", ZoteroReferenceMark.PREFIX + "{this is not json} RNDabcdefghij"),
                new BibDatabase());

        assertEquals(new ImportCited.CitedEntries(List.of(), List.of()), citedEntries);
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

        ImportCited.CitedEntries citedEntries = ImportCited.collectCitedEntries(
                List.of(markWithNullValues, citeWithZotero("RNDklmnopqrst")),
                new BibDatabase());

        assertEquals(List.of(Optional.of("Null URIs"), Optional.of("Cited from Zotero")), titlesOf(citedEntries.newEntries()));
    }

    @Test
    void collectsNothingFromDocumentWithoutReferenceMarks() {
        ImportCited.CitedEntries citedEntries = ImportCited.collectCitedEntries(List.of(), new BibDatabase());

        assertEquals(new ImportCited.CitedEntries(List.of(), List.of()), citedEntries);
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
        List<BibEntry> citedEntries = List.of(entries);
        List<String> citationKeys = citedEntries.stream()
                                                .map(entry -> entry.getCitationKey().orElseThrow())
                                                .toList();
        return ZoteroReferenceMark.buildReferenceMark(
                citedEntries,
                citationKeys,
                citationKeys.stream().map(_ -> 0).toList(),
                1,
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
        String citationJson = """
                {"citationItems": [{"id": %s, "uris": ["http://zotero.org/users/123/items/%s"],
                "itemData": {"id": %s, "type": "article-journal", "title": "%s", "author": [{"family": "Doe", "given": "John"}]}}]}
                """.formatted(itemId, itemId, itemId, title);
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
