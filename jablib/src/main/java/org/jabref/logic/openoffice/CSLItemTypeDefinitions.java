package org.jabref.logic.openoffice;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.BiblatexApaEntryType;
import org.jabref.model.entry.types.BiblatexNonStandardEntryType;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.IEEETranEntryType;
import org.jabref.model.entry.types.StandardEntryType;

class CSLItemTypeDefinitions {
    /// CSL item type <-> Zotero item type mapping can be found via [zotero-schema/schema.json](https://github.com/zotero/zotero-schema/blob/62e983a2e575fe9b9a3677ad7c9772080b67a1e4/schema.json#L3219-L3324)
    /// CSL item type <-> BibLaTeX entry type can be found via [zotero-better-bibtex/biblatex.ts](https://github.com/retorquere/zotero-better-bibtex/blob/master/translators/bibtex/biblatex.ts)
    private static final Map<String, EntryType> ITEM_TYPES = Map.ofEntries(
            // preprint
            Map.entry("article", StandardEntryType.Online),
            // Journal Article
            Map.entry("article-journal", StandardEntryType.Article),
            // Magazine Article
            Map.entry("article-magazine", StandardEntryType.Article),
            // Newspaper Article
            Map.entry("article-newspaper", StandardEntryType.Article),
            // Bill
            Map.entry("bill", BiblatexApaEntryType.Legislation),
            // Book
            Map.entry("book", StandardEntryType.Book),
            // Podcast, TV Broadcast, Radio Broadcast
            Map.entry("broadcast", StandardEntryType.Misc),
            // Book Section
            Map.entry("chapter", StandardEntryType.InCollection),
            // Dataset
            Map.entry("dataset", StandardEntryType.Dataset),
            // document, attachment, note
            Map.entry("document", StandardEntryType.InReference),
            // Dictionary Entry
            Map.entry("entry-dictionary", StandardEntryType.InReference),
            // Encyclopedia Article
            Map.entry("entry-encyclopedia", StandardEntryType.InReference),
            // Artwork
            Map.entry("graphic", BiblatexNonStandardEntryType.Image),
            // Hearing
            Map.entry("hearing", BiblatexApaEntryType.Jurisdiction),
            // Interview
            Map.entry("interview", StandardEntryType.Misc),
            // Case
            Map.entry("legal_case", BiblatexApaEntryType.Jurisdiction),
            // Statute
            Map.entry("legislation", BiblatexApaEntryType.Legislation),
            // Manuscript
            Map.entry("manuscript", StandardEntryType.Unpublished),
            // Map
            Map.entry("map", StandardEntryType.Misc),
            // Film, Video Recording
            Map.entry("motion_picture", BiblatexNonStandardEntryType.Movie),
            // Conference Paper
            Map.entry("paper-conference", StandardEntryType.InProceedings),
            // Patent
            Map.entry("patent", IEEETranEntryType.Patent),
            // Letter, Email, Instant Message
            Map.entry("personal_communication", BiblatexNonStandardEntryType.Letter),
            // Forum Post
            Map.entry("post", StandardEntryType.Online),
            // Blog Post
            Map.entry("post-weblog", StandardEntryType.Online),
            // Report
            Map.entry("report", StandardEntryType.Report),
            // Computer Program
            Map.entry("software", StandardEntryType.Report),
            // Audio Recording
            Map.entry("song", BiblatexNonStandardEntryType.Music),
            // Presentation
            Map.entry("speech", StandardEntryType.Misc),
            // Standard
            Map.entry("standard", StandardEntryType.Misc),
            // Thesis
            Map.entry("thesis", StandardEntryType.Thesis),
            // Web Page
            Map.entry("webpage", StandardEntryType.Online)
    );

    private static final Map<String, StandardField> COMMON_FIELDS = Map.ofEntries(
            cslFieldToBibField("title", StandardField.TITLE),
            cslFieldToBibField("DOI", StandardField.DOI)
    );

    private static final Map<String, Map<String, StandardField>> FIELD_MAPPING = Map.ofEntries(
            cslTypeToBibType("article", Map.ofEntries(
                    cslFieldToBibField("container-title", StandardField.JOURNALTITLE),
                    cslFieldToBibField("issue", StandardField.NUMBER),
                    cslFieldToBibField("page", StandardField.PAGES),
                    cslFieldToBibField("volume", StandardField.VOLUME))),
            cslTypeToBibType("article-journal", Map.ofEntries(
                    cslFieldToBibField("container-title", StandardField.JOURNALTITLE),
                    cslFieldToBibField("issue", StandardField.NUMBER),
                    cslFieldToBibField("page", StandardField.PAGES),
                    cslFieldToBibField("volume", StandardField.VOLUME))),
            cslTypeToBibType("article-magazine", Map.ofEntries(
                    cslFieldToBibField("container-title", StandardField.JOURNALTITLE),
                    cslFieldToBibField("issue", StandardField.NUMBER),
                    cslFieldToBibField("page", StandardField.PAGES),
                    cslFieldToBibField("volume", StandardField.VOLUME))),
            cslTypeToBibType("article-newspaper", Map.ofEntries(
                    cslFieldToBibField("container-title", StandardField.JOURNALTITLE),
                    cslFieldToBibField("issue", StandardField.NUMBER),
                    cslFieldToBibField("page", StandardField.PAGES),
                    cslFieldToBibField("volume", StandardField.VOLUME))),
            cslTypeToBibType("bill", Map.ofEntries(
                    cslFieldToBibField("page", StandardField.PAGES))),
            cslTypeToBibType("book", Map.ofEntries(
                    cslFieldToBibField("collection-title", StandardField.SERIES),
                    cslFieldToBibField("edition", StandardField.EDITION),
                    cslFieldToBibField("ISBN", StandardField.ISBN),
                    cslFieldToBibField("publisher", StandardField.PUBLISHER),
                    cslFieldToBibField("publisher-place", StandardField.LOCATION),
                    cslFieldToBibField("volume", StandardField.VOLUME))),
            cslTypeToBibType("broadcast", Map.ofEntries(
                    cslFieldToBibField("publisher-place", StandardField.LOCATION),
                    cslFieldToBibField("publisher", StandardField.PUBLISHER))),
            cslTypeToBibType("chapter", Map.ofEntries(
                    cslFieldToBibField("collection-title", StandardField.SERIES),
                    cslFieldToBibField("container-title", StandardField.JOURNALTITLE),
                    cslFieldToBibField("edition", StandardField.EDITION),
                    cslFieldToBibField("page", StandardField.PAGES),
                    cslFieldToBibField("publisher", StandardField.PUBLISHER),
                    cslFieldToBibField("publisher-place", StandardField.LOCATION),
                    cslFieldToBibField("volume", StandardField.VOLUME))),
            cslTypeToBibType("dataset", Map.ofEntries(
                    cslFieldToBibField("publisher-place", StandardField.LOCATION),
                    cslFieldToBibField("publisher", StandardField.PUBLISHER))),
            cslTypeToBibType("document", Map.ofEntries(
                    cslFieldToBibField("publisher-place", StandardField.LOCATION),
                    cslFieldToBibField("publisher", StandardField.PUBLISHER))),
            cslTypeToBibType("entry-dictionary", Map.ofEntries(
                    cslFieldToBibField("publisher-place", StandardField.LOCATION),
                    cslFieldToBibField("publisher", StandardField.PUBLISHER))),
            cslTypeToBibType("entry-encyclopedia", Map.ofEntries(
                    cslFieldToBibField("publisher-place", StandardField.LOCATION),
                    cslFieldToBibField("publisher", StandardField.PUBLISHER))),
            cslTypeToBibType("graphic", Map.ofEntries(
                    cslFieldToBibField("publisher-place", StandardField.LOCATION),
                    cslFieldToBibField("publisher", StandardField.PUBLISHER))),
            cslTypeToBibType("hearing", Map.ofEntries(
                    cslFieldToBibField("publisher-place", StandardField.LOCATION),
                    cslFieldToBibField("publisher", StandardField.PUBLISHER))),
            cslTypeToBibType("interview", Map.ofEntries(
                    cslFieldToBibField("publisher-place", StandardField.LOCATION),
                    cslFieldToBibField("publisher", StandardField.PUBLISHER))),
            cslTypeToBibType("legal_case", Map.ofEntries(
                    cslFieldToBibField("publisher-place", StandardField.LOCATION),
                    cslFieldToBibField("publisher", StandardField.PUBLISHER))),
            cslTypeToBibType("legislation", Map.ofEntries(
                    cslFieldToBibField("publisher-place", StandardField.LOCATION),
                    cslFieldToBibField("publisher", StandardField.PUBLISHER))),
            cslTypeToBibType("manuscript", Map.ofEntries(
                    cslFieldToBibField("publisher-place", StandardField.LOCATION),
                    cslFieldToBibField("publisher", StandardField.PUBLISHER))),
            cslTypeToBibType("map", Map.ofEntries(
                    cslFieldToBibField("publisher-place", StandardField.LOCATION),
                    cslFieldToBibField("publisher", StandardField.PUBLISHER))),
            cslTypeToBibType("motion_picture", Map.ofEntries(
                    cslFieldToBibField("publisher-place", StandardField.LOCATION),
                    cslFieldToBibField("publisher", StandardField.PUBLISHER))),
            cslTypeToBibType("paper-conference", Map.ofEntries(
                    cslFieldToBibField("collection-title", StandardField.SERIES),
                    cslFieldToBibField("container-title", StandardField.JOURNALTITLE),
                    cslFieldToBibField("volume", StandardField.VOLUME),
                    cslFieldToBibField("event-place", StandardField.LOCATION),
                    cslFieldToBibField("page", StandardField.PAGES),
                    cslFieldToBibField("publisher", StandardField.PUBLISHER))),
            cslTypeToBibType("patent", Map.ofEntries(
                    cslFieldToBibField("number", StandardField.NUMBER))),
            cslTypeToBibType("personal_communication", Map.ofEntries(
                    cslFieldToBibField("event-place", StandardField.LOCATION))),
            cslTypeToBibType("post", Map.ofEntries(
                    cslFieldToBibField("publisher", StandardField.ORGANIZATION))),
            cslTypeToBibType("post-weblog", Map.ofEntries(
                    cslFieldToBibField("publisher", StandardField.ORGANIZATION))),
            cslTypeToBibType("report", Map.ofEntries(
                    cslFieldToBibField("page", StandardField.PAGES),
                    cslFieldToBibField("number", StandardField.NUMBER),
                    cslFieldToBibField("publisher", StandardField.INSTITUTION),
                    cslFieldToBibField("publisher-place", StandardField.LOCATION))),
            cslTypeToBibType("software", Map.ofEntries(
                    cslFieldToBibField("event-place", StandardField.LOCATION),
                    cslFieldToBibField("publisher", StandardField.ORGANIZATION))),
            cslTypeToBibType("song", Map.ofEntries(
                    cslFieldToBibField("event-place", StandardField.LOCATION),
                    cslFieldToBibField("publisher", StandardField.ORGANIZATION))),
            cslTypeToBibType("speech", Map.ofEntries(
                    cslFieldToBibField("event-place", StandardField.LOCATION),
                    cslFieldToBibField("publisher", StandardField.ORGANIZATION))),
            cslTypeToBibType("standard", Map.ofEntries(
                    cslFieldToBibField("event-place", StandardField.LOCATION),
                    cslFieldToBibField("publisher", StandardField.ORGANIZATION))),
            cslTypeToBibType("thesis", Map.ofEntries(
                    cslFieldToBibField("publisher", StandardField.INSTITUTION),
                    cslFieldToBibField("publisher-place", StandardField.LOCATION))),
            cslTypeToBibType("webpage", Map.ofEntries(
                    cslFieldToBibField("container-title", StandardField.JOURNALTITLE),
                    cslFieldToBibField("URL", StandardField.URL)))
    );

    static Optional<EntryType> getEntryType(String cslItemType) {
        return Optional.ofNullable(ITEM_TYPES.get(cslItemType));
    }

    static Map<String, StandardField> getFieldMappings(String cslItemType) {
        Map<String, StandardField> fieldMappings = new HashMap<>(COMMON_FIELDS);
        fieldMappings.putAll(FIELD_MAPPING.getOrDefault(cslItemType, Map.of()));

        return fieldMappings;
    }

    private static Map.Entry<String, StandardField> cslFieldToBibField(String cslField, StandardField bibField) {
        return Map.entry(cslField, bibField);
    }

    private static Map.Entry<String, Map<String, StandardField>> cslTypeToBibType(String cslType, Map<String, StandardField> bibTypeWithFields) {
        return Map.entry(cslType, bibTypeWithFields);
    }
}
