package org.jabref.logic.openoffice;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@NullMarked
class CitationEntryTypeMetadataTest {

    private static final String ZOTERO_JOURNAL_ARTICLE = """
            ZOTERO_ITEM CSL_CITATION {
              "citationItems": [
                {
                  "id": 587,
                  "itemData": {
                    "type": "article-journal",
                    "title": "Test article",
                    "container-title": "Journal of Test"
                  }
                }
              ]
            } test1234
            """;

    @Test
    void mergeEntryTypesStoresCitationKeyAndEntryType() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Keen2020");

        String metadata = CitationEntryTypeMetadata.mergeEntryTypes(Optional.empty(), List.of(entry));

        assertEquals(Map.of("Keen2020", "article"), CitationEntryTypeMetadata.readEntryTypes(metadata));
    }

    @Test
    void mergeEntryTypesKeepsExistingEntryTypes() {
        BibEntry entry = new BibEntry(StandardEntryType.InProceedings)
                .withCitationKey("Smith2021");
        String existingMetadata = """
                {
                  "schemaVersion": 1,
                  "citationTypeMap": {
                    "Keen2020": {
                      "jabrefEntryType": "article"
                    }
                  }
                }
                """;

        String metadata = CitationEntryTypeMetadata.mergeEntryTypes(Optional.of(existingMetadata), List.of(entry));

        assertEquals(Map.of(
                "Keen2020", "article",
                "Smith2021", "inproceedings"), CitationEntryTypeMetadata.readEntryTypes(metadata));
    }

    @Test
    void mergeEntryTypesUpdatesExistingCitationKey() {
        BibEntry entry = new BibEntry(StandardEntryType.Book)
                .withCitationKey("Keen2020");
        String existingMetadata = """
                {
                  "schemaVersion": 1,
                  "citationTypeMap": {
                    "Keen2020": {
                      "jabrefEntryType": "article"
                    }
                  }
                }
                """;

        String metadata = CitationEntryTypeMetadata.mergeEntryTypes(Optional.of(existingMetadata), List.of(entry));

        assertEquals(Map.of("Keen2020", "book"), CitationEntryTypeMetadata.readEntryTypes(metadata));
    }

    @Test
    void readEntryTypesReturnsEmptyMapForInvalidMetadata() {
        assertEquals(Map.of(), CitationEntryTypeMetadata.readEntryTypes("not json"));
    }

    @Test
    void parseZoteroEntriesUsesCslToJabRefMapping() {
        List<BibEntry> entries = CitationEntryTypeMetadata.parseZoteroEntries(
                List.of("JR_cite_1_Keen2020", ZOTERO_JOURNAL_ARTICLE),
                Map.of());
        BibEntry expectedEntry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Zotero-587")
                .withField(StandardField.TITLE, "Test article")
                .withField(StandardField.JOURNALTITLE, "Journal of Test");

        assertEquals(List.of(expectedEntry), entries);
    }

    @Test
    void parseZoteroEntriesUsesStoredEntryTypeMetadata() {
        List<BibEntry> entries = CitationEntryTypeMetadata.parseZoteroEntries(
                List.of(ZOTERO_JOURNAL_ARTICLE),
                Map.of("Zotero-587", "book"));
        BibEntry expectedEntry = new BibEntry(StandardEntryType.Book)
                .withCitationKey("Zotero-587")
                .withField(StandardField.TITLE, "Test article")
                .withField(StandardField.JOURNALTITLE, "Journal of Test");

        assertEquals(List.of(expectedEntry), entries);
    }
}
