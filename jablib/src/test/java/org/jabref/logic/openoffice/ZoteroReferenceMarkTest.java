package org.jabref.logic.openoffice;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Pattern;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.openoffice.style.CitationType;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZoteroReferenceMarkTest {
    private static final Gson GSON = new Gson();
    private static final Pattern CITATION_ID = Pattern.compile("[a-z][a-z0-9]{7}");
    private static final Pattern RANDOM_SUFFIX = Pattern.compile("RND[a-z][a-z0-9]{9}");

    @Test
    void buildReferenceMarkCreatesZoteroReferenceMarkWithEmbeddedItemData() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Smith2020")
                .withField(StandardField.AUTHOR, "Smith, Jane")
                .withField(StandardField.TITLE, "Reference mark compatibility")
                .withField(StandardField.DATE, "2020-05-03");
        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext(new BibDatabase(List.of(entry)));

        ZoteroReferenceMark referenceMark = ZoteroReferenceMark.buildReferenceMark(
                List.of(entry),
                List.of("Smith2020"),
                1,
                CitationType.AUTHORYEAR_INTEXT,
                bibDatabaseContext,
                new BibEntryTypesManager(),
                Map.of());
        String referenceMarkName = referenceMark.name();

        assertTrue(referenceMarkName.startsWith("ZOTERO_ITEM CSL_CITATION {"));
        assertTrue(referenceMarkName.indexOf("\"schema\"") < referenceMarkName.indexOf("\"citationID\""));

        ZoteroCitationData citation = GSON.fromJson(ZoteroReferenceMark.getCSLJson(referenceMarkName), ZoteroCitationData.class);
        ZoteroCitationData.CitationItemData citationItem = citation.citationItems.getFirst();
        JsonObject citationJson = getCitationJson(referenceMarkName);
        JsonObject citationItemJson = citationJson.getAsJsonArray("citationItems").get(0).getAsJsonObject();
        assertFalse(citationJson.has("properties"));
        assertEquals(ZoteroCitationData.CSL_CITATION_SCHEMA, citation.schema);
        assertEquals(8, citation.citationId.length());
        assertTrue(CITATION_ID.matcher(citation.citationId).matches());
        assertEquals(Boolean.TRUE, citationItem.suppressAuthor);
        assertFalse(citationItemJson.has("jabref-citation-number"));
        assertEquals(List.of("http://www.jabref.org/Smith2020"), citationItem.uris);
        JsonArray dateParts = citationItemJson.getAsJsonObject("itemData")
                                              .getAsJsonObject("issued")
                                              .getAsJsonArray("date-parts")
                                              .get(0)
                                              .getAsJsonArray();
        assertTrue(dateParts.get(0).getAsJsonPrimitive().isString());
        assertEquals("2020", dateParts.get(0).getAsString());
        assertTrue(dateParts.get(1).getAsJsonPrimitive().isNumber());
        assertEquals(5, dateParts.get(1).getAsInt());
        assertTrue(dateParts.get(2).getAsJsonPrimitive().isNumber());
        assertEquals(3, dateParts.get(2).getAsInt());

        String uniqueId = referenceMark.uniqueId();
        assertEquals(new ZoteroReferenceMark(
                referenceMarkName,
                List.of("Smith2020"),
                uniqueId,
                CitationType.AUTHORYEAR_INTEXT), referenceMark);
        assertEquals(13, uniqueId.length());
        assertEquals("RND", uniqueId.substring(0, 3));
        assertTrue(RANDOM_SUFFIX.matcher(uniqueId).matches());

        BibEntry parsedEntry = ZoteroCitationMarkParser.parseCslCitationJson(
                ZoteroReferenceMark.getCSLJson(referenceMarkName)).getFirst();
        assertEquals(Optional.of("Smith2020"), parsedEntry.getCitationKey());
        assertEquals(StandardEntryType.Article, parsedEntry.getType());
        assertEquals(Optional.of("Reference mark compatibility"), parsedEntry.getField(StandardField.TITLE));
        assertEquals(Optional.of("2020"), parsedEntry.getField(StandardField.YEAR));
    }

    @Test
    void buildReferenceMarkOmitsEmptyItemDataFields() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Smith2020")
                .withField(StandardField.TITLE, "Reference mark compatibility");
        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext(new BibDatabase(List.of(entry)));

        String referenceMarkName = ZoteroReferenceMark.buildReferenceMark(
                List.of(entry),
                List.of("Smith2020"),
                1,
                CitationType.AUTHORYEAR_PAR,
                bibDatabaseContext,
                new BibEntryTypesManager(),
                Map.of()).name();

        JsonObject itemData = getCitationJson(referenceMarkName).getAsJsonArray("citationItems").get(0).getAsJsonObject()
                                                                .getAsJsonObject("itemData");

        assertEquals("1", itemData.get("id").getAsString());
        assertEquals("Smith2020", itemData.get("citation-key").getAsString());
        assertEquals("article-journal", itemData.get("type").getAsString());
        assertEquals("Reference mark compatibility", itemData.get("title").getAsString());
        assertFalse(itemData.has("abstract"));
        assertFalse(itemData.has("author"));
        assertFalse(itemData.has("issued"));
        assertFalse(itemData.has("volume"));
    }

    @Test
    void buildReferenceMarkCreatesStableUriForSameCitationKey() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Smith2020")
                .withField(StandardField.TITLE, "Reference mark compatibility");
        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext(new BibDatabase(List.of(entry)));

        String firstReferenceMarkName = ZoteroReferenceMark.buildReferenceMark(
                List.of(entry),
                List.of("Smith2020"),
                1,
                CitationType.AUTHORYEAR_PAR,
                bibDatabaseContext,
                new BibEntryTypesManager(),
                Map.of()).name();
        String secondReferenceMarkName = ZoteroReferenceMark.buildReferenceMark(
                List.of(entry),
                List.of("Smith2020"),
                1,
                CitationType.AUTHORYEAR_PAR,
                bibDatabaseContext,
                new BibEntryTypesManager(),
                Map.of()).name();

        String firstUri = getCitationJson(firstReferenceMarkName).getAsJsonArray("citationItems").get(0).getAsJsonObject()
                                                                 .getAsJsonArray("uris").get(0).getAsString();
        String secondUri = getCitationJson(secondReferenceMarkName).getAsJsonArray("citationItems").get(0).getAsJsonObject()
                                                                   .getAsJsonArray("uris").get(0).getAsString();

        assertEquals(firstUri, secondUri);
        assertNotEquals(getCitationJson(firstReferenceMarkName).get("citationID"), getCitationJson(secondReferenceMarkName).get("citationID"));
        assertNotEquals(ZoteroReferenceMark.parse(firstReferenceMarkName).orElseThrow().uniqueId(), ZoteroReferenceMark.parse(secondReferenceMarkName).orElseThrow().uniqueId());
    }

    @Test
    void buildReferenceMarkCopiesLinkedZoteroUriForCitationKey() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Smith2020")
                .withField(StandardField.TITLE, "Reference mark compatibility");
        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext(new BibDatabase(List.of(entry)));

        String referenceMarkName = ZoteroReferenceMark.buildReferenceMark(
                List.of(entry),
                List.of("Smith2020"),
                1,
                CitationType.AUTHORYEAR_PAR,
                bibDatabaseContext,
                new BibEntryTypesManager(),
                Map.of("Smith2020", "http://zotero.org/users/123/items/ABCD1234")).name();

        JsonObject citationItem = getCitationJson(referenceMarkName).getAsJsonArray("citationItems").get(0).getAsJsonObject();

        assertEquals("http://zotero.org/users/123/items/ABCD1234", citationItem.getAsJsonArray("uris").get(0).getAsString());
        assertEquals("http://www.jabref.org/Smith2020", citationItem.getAsJsonArray("uris").get(1).getAsString());
    }

    @Test
    void buildReferenceMarkCreatesIncrementingItemIds() {
        BibEntry firstEntry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Smith2020")
                .withField(StandardField.TITLE, "First reference");
        BibEntry secondEntry = new BibEntry(StandardEntryType.Book)
                .withCitationKey("Jones2021")
                .withField(StandardField.TITLE, "Second reference");
        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext(new BibDatabase(List.of(firstEntry, secondEntry)));

        String referenceMarkName = ZoteroReferenceMark.buildReferenceMark(
                List.of(firstEntry, secondEntry),
                List.of("Smith2020", "Jones2021"),
                1,
                CitationType.AUTHORYEAR_PAR,
                bibDatabaseContext,
                new BibEntryTypesManager(),
                Map.of()).name();

        JsonObject firstCitationItem = getCitationJson(referenceMarkName).getAsJsonArray("citationItems").get(0).getAsJsonObject();
        JsonObject secondCitationItem = getCitationJson(referenceMarkName).getAsJsonArray("citationItems").get(1).getAsJsonObject();

        assertEquals("1", firstCitationItem.get("id").getAsString());
        assertEquals("1", firstCitationItem.getAsJsonObject("itemData").get("id").getAsString());
        assertEquals("Smith2020", firstCitationItem.getAsJsonObject("itemData").get("citation-key").getAsString());
        assertEquals("2", secondCitationItem.get("id").getAsString());
        assertEquals("2", secondCitationItem.getAsJsonObject("itemData").get("id").getAsString());
        assertEquals("Jones2021", secondCitationItem.getAsJsonObject("itemData").get("citation-key").getAsString());
    }

    @Test
    void buildReferenceMarkStartsItemIdsAtGivenFirstItemId() {
        BibEntry firstEntry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Smith2020")
                .withField(StandardField.TITLE, "First reference");
        BibEntry secondEntry = new BibEntry(StandardEntryType.Book)
                .withCitationKey("Jones2021")
                .withField(StandardField.TITLE, "Second reference");
        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext(new BibDatabase(List.of(firstEntry, secondEntry)));

        String referenceMarkName = ZoteroReferenceMark.buildReferenceMark(
                List.of(firstEntry, secondEntry),
                List.of("Smith2020", "Jones2021"),
                3,
                CitationType.AUTHORYEAR_PAR,
                bibDatabaseContext,
                new BibEntryTypesManager(),
                Map.of()).name();

        JsonObject firstCitationItem = getCitationJson(referenceMarkName).getAsJsonArray("citationItems").get(0).getAsJsonObject();
        JsonObject secondCitationItem = getCitationJson(referenceMarkName).getAsJsonArray("citationItems").get(1).getAsJsonObject();

        assertEquals("3", firstCitationItem.get("id").getAsString());
        assertEquals("3", firstCitationItem.getAsJsonObject("itemData").get("id").getAsString());
        assertEquals("4", secondCitationItem.get("id").getAsString());
        assertEquals("4", secondCitationItem.getAsJsonObject("itemData").get("id").getAsString());
    }

    @Test
    void parseKeepsNumericCitationKeyWhenCitationKeyIsExplicit() {
        String referenceMarkName = """
                ZOTERO_ITEM CSL_CITATION {
                  "citationItems": [
                    {
                      "id": "123",
                      "itemData": {
                        "id": "123",
                        "citation-key": "123",
                        "type": "book"
                      }
                    }
                  ]
                } RNDabc123DEF4
                """.trim();

        ZoteroReferenceMark referenceMark = ZoteroReferenceMark.parse(referenceMarkName).orElseThrow();

        assertEquals(List.of("123"), referenceMark.citationKeys());
    }

    @Test
    void parsePrefersJabRefUriCitationKeyOverZoteroItemId() {
        String referenceMarkName = """
                ZOTERO_ITEM CSL_CITATION {
                  "citationItems": [
                    {
                      "id": 600,
                      "uris": [
                        "http://zotero.org/users/123/items/ABCD1234",
                        "http://www.jabref.org/Smith2020"
                      ],
                      "itemData": {
                        "id": "600",
                        "type": "book"
                      }
                    }
                  ]
                } RNDabc123DEF4
                """.trim();

        ZoteroReferenceMark referenceMark = ZoteroReferenceMark.parse(referenceMarkName).orElseThrow();

        assertEquals(List.of("Smith2020"), referenceMark.citationKeys());
    }

    @Test
    void extractZoteroUriByCitationKey() {
        String referenceMarkName = """
                ZOTERO_ITEM CSL_CITATION {
                  "citationItems": [
                    {
                      "id": 600,
                      "uris": [
                        "http://zotero.org/users/123/items/ABCD1234",
                        "http://www.jabref.org/Smith2020"
                      ],
                      "itemData": {
                        "id": "600",
                        "type": "book"
                      }
                    }
                  ]
                } RNDabc123DEF4
                """.trim();

        Map<String, String> zoteroUriByCitationKey = ZoteroReferenceMark.extractZoteroUriByCitationKey(referenceMarkName);

        assertEquals(Map.of("Smith2020", "http://zotero.org/users/123/items/ABCD1234"), zoteroUriByCitationKey);
    }

    @Test
    void extractZoteroUriByCitationKeyReturnsEmptyMapForJabRefReferenceMark() {
        String referenceMarkName = "JABREF_Smith2020 CID_1 uniqueId1 NORMAL";

        Map<String, String> zoteroUriByCitationKey = ZoteroReferenceMark.extractZoteroUriByCitationKey(referenceMarkName);

        assertEquals(Map.of(), zoteroUriByCitationKey);
    }

    @Test
    void getMaxItemIdFindsHighestExistingItemId() {
        String referenceMarkName = """
                ZOTERO_ITEM CSL_CITATION {
                  "citationItems": [
                    {
                      "id": "3",
                      "itemData": {
                        "id": "3",
                        "citation-key": "Smith2020",
                        "type": "book"
                      }
                    },
                    {
                      "id": "4",
                      "itemData": {
                        "id": "4",
                        "citation-key": "Jones2021",
                        "type": "book"
                      }
                    }
                  ]
                } RNDabc123DEF4
                """.trim();

        assertEquals(4, ZoteroReferenceMark.getMaxItemId(referenceMarkName));
    }

    @Test
    void buildReferenceMarkNormalCitationOmitsSuppressAuthor() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Smith2020")
                .withField(StandardField.TITLE, "Reference mark compatibility");
        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext(new BibDatabase(List.of(entry)));

        String referenceMarkName = ZoteroReferenceMark.buildReferenceMark(
                List.of(entry),
                List.of("Smith2020"),
                1,
                CitationType.AUTHORYEAR_PAR,
                bibDatabaseContext,
                new BibEntryTypesManager(),
                Map.of()).name();

        JsonObject citationItem = getCitationJson(referenceMarkName).getAsJsonArray("citationItems").get(0).getAsJsonObject();

        assertFalse(citationItem.has("suppress-author"));
    }

    @Test
    void updateCitationTypeToNormalRemovesSuppressAuthor() {
        String referenceMarkName = """
                ZOTERO_ITEM CSL_CITATION {
                  "citationItems": [
                    {
                      "id": "Smith2020",
                      "suppress-author": true,
                      "itemData": {
                        "id": "Smith2020",
                        "citation-key": "Smith2020",
                        "type": "book"
                      }
                    }
                  ]
                } RNDabc123DEF4
                """.trim();

        String updatedReferenceMarkName = ZoteroReferenceMark.updateCitationType(referenceMarkName, CitationType.AUTHORYEAR_PAR);
        ZoteroReferenceMark referenceMark = ZoteroReferenceMark.parse(updatedReferenceMarkName).orElseThrow();
        JsonObject citation = getCitationJson(updatedReferenceMarkName);
        JsonObject citationItem = citation.getAsJsonArray("citationItems").get(0).getAsJsonObject();

        assertFalse(citation.has("properties"));
        assertFalse(citationItem.has("suppress-author"));
        assertEquals(new ZoteroReferenceMark(
                updatedReferenceMarkName,
                List.of("Smith2020"),
                "RNDabc123DEF4",
                CitationType.AUTHORYEAR_PAR), referenceMark);
    }

    @ParameterizedTest
    @CsvSource({"0, 0", "00000, 0", "000001, 1", "2147483646, 2147483646", "2147483647, 2147483647", "0002147483647, 2147483647"})
    void boundedItemIdsAcceptLeadingZerosAndIntegerBoundaries(String id, int expected) {
        assertEquals(OptionalInt.of(expected), ZoteroReferenceMark.boundedItemId(id));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "opaque-id", "+1", "-1", "2147483648", "0002147483648", "9999999999999999999999999999999999999999"})
    void foreignItemIdsOutsideTheBoundedDecimalPolicyRemainOpaque(String id) {
        assertEquals(OptionalInt.empty(), ZoteroReferenceMark.boundedItemId(id));
    }

    @Test
    void hugeAndLeadingZeroNativeItemIdsArePreservedWhenFindingTheMaximum() {
        String foreignId = "9999999999999999999999999999999999999999";
        String boundedId = "0".repeat(10_000) + Integer.MAX_VALUE;
        JsonObject item = new JsonObject();
        item.addProperty("id", foreignId);
        JsonObject itemData = new JsonObject();
        itemData.addProperty("id", boundedId);
        item.add("itemData", itemData);
        JsonArray items = new JsonArray();
        items.add(item);
        JsonObject citation = new JsonObject();
        citation.add("citationItems", items);
        String name = ZoteroReferenceMark.PREFIX + citation;
        assertEquals(Integer.MAX_VALUE, ZoteroReferenceMark.getMaxItemId(name));
        JsonObject original = getCitationJson(name).getAsJsonArray("citationItems").get(0).getAsJsonObject();
        assertEquals(foreignId, original.get("id").getAsString());
        assertEquals(boundedId, original.getAsJsonObject("itemData").get("id").getAsString());
    }

    private static JsonObject getCitationJson(String referenceMarkName) {
        return JsonParser.parseString(ZoteroReferenceMark.getCSLJson(referenceMarkName)).getAsJsonObject();
    }
}
