package org.jabref.logic.openoffice;

import java.util.List;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ZoteroCitationLinkerTest {
    private static final String ZOTERO_URI = "http://zotero.org/users/123/items/ABCD1234";
    private static final BibEntryTypesManager ENTRY_TYPES_MANAGER = new BibEntryTypesManager();

    @Test
    void addJabRefUriToZoteroCitationWithUniqueDuplicate() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Smith2020")
                .withField(StandardField.DOI, "10.123/example");
        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext(new BibDatabase(List.of(entry)));

        String updatedReferenceMarkName = ZoteroCitationLinker.linkReferenceMark(
                zoteroReferenceMarkName("10.123/example"),
                bibDatabaseContext,
                ENTRY_TYPES_MANAGER);

        JsonObject citation = getCitationJson(updatedReferenceMarkName);
        JsonObject citationItem = citation.getAsJsonArray("citationItems").get(0).getAsJsonObject();
        JsonArray uris = citationItem.getAsJsonArray("uris");
        assertEquals(ZOTERO_URI, uris.get(0).getAsString());
        assertEquals("http://www.jabref.org/Smith2020", uris.get(1).getAsString());

        JsonObject itemData = citationItem.getAsJsonObject("itemData");
        assertEquals("https://github.com/citation-style-language/schema/raw/master/csl-citation.json", citation.get("schema").getAsString());
        assertEquals("keep-me", citation.getAsJsonObject("properties").get("plainCitation").getAsString());
        assertEquals("Original archive", itemData.get("archive-place").getAsString());
    }

    @Test
    void keepZoteroCitationUnchangedWithoutDuplicate() {
        String referenceMarkName = zoteroReferenceMarkName("10.123/example");
        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext(new BibDatabase());

        String updatedReferenceMarkName = ZoteroCitationLinker.linkReferenceMark(
                referenceMarkName,
                bibDatabaseContext,
                ENTRY_TYPES_MANAGER);

        assertEquals(referenceMarkName, updatedReferenceMarkName);
    }

    @Test
    void keepZoteroCitationUnchangedWithExistingJabRefUri() {
        String referenceMarkName = zoteroReferenceMarkNameWithJabRefUri("10.123/example", "Smith2020");
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Smith2020")
                .withField(StandardField.DOI, "10.123/example");
        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext(new BibDatabase(List.of(entry)));

        String updatedReferenceMarkName = ZoteroCitationLinker.linkReferenceMark(
                referenceMarkName,
                bibDatabaseContext,
                ENTRY_TYPES_MANAGER);

        assertEquals(referenceMarkName, updatedReferenceMarkName);
    }

    private static String zoteroReferenceMarkName(String doi) {
        return """
                ZOTERO_ITEM CSL_CITATION {
                  "citationID": "abc123",
                  "properties": {
                    "plainCitation": "keep-me"
                  },
                  "citationItems": [
                    {
                      "id": 600,
                      "uris": ["%s"],
                      "itemData": {
                        "type": "article-journal",
                        "DOI": "%s",
                        "title": "Reference mark compatibility",
                        "archive-place": "Original archive"
                      }
                    }
                  ],
                  "schema": "https://github.com/citation-style-language/schema/raw/master/csl-citation.json"
                } RNDabc123DEF4
                """.formatted(ZOTERO_URI, doi).trim();
    }

    private static String zoteroReferenceMarkNameWithJabRefUri(String doi, String citationKey) {
        return """
                ZOTERO_ITEM CSL_CITATION {
                  "citationID": "abc123",
                  "properties": {
                    "plainCitation": "keep-me"
                  },
                  "citationItems": [
                    {
                      "id": 600,
                      "uris": ["%s", "http://www.jabref.org/%s"],
                      "itemData": {
                        "type": "article-journal",
                        "DOI": "%s",
                        "title": "Reference mark compatibility",
                        "archive-place": "Original archive"
                      }
                    }
                  ],
                  "schema": "https://github.com/citation-style-language/schema/raw/master/csl-citation.json"
                } RNDabc123DEF4
                """.formatted(ZOTERO_URI, citationKey, doi).trim();
    }

    private static JsonObject getCitationJson(String referenceMarkName) {
        return JsonParser.parseString(ZoteroReferenceMark.getCSLJson(referenceMarkName)).getAsJsonObject();
    }
}
