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
            withField("title", StandardField.TITLE),
            withField("DOI", StandardField.DOI)
    );

    private static final Map<String, Map<String, StandardField>> FIELD_MAPPING = Map.ofEntries(
            override("article-journal", Map.ofEntries(
                    withField("container-title", StandardField.JOURNALTITLE),
                    withField("issue", StandardField.NUMBER),
                    withField("page", StandardField.PAGES),
                    withField("volume", StandardField.VOLUME))),
            override("article-magazine", Map.ofEntries(
                    withField("container-title", StandardField.JOURNALTITLE),
                    withField("issue", StandardField.NUMBER),
                    withField("page", StandardField.PAGES),
                    withField("volume", StandardField.VOLUME))),
            override("article-newspaper", Map.ofEntries(
                    withField("container-title", StandardField.JOURNALTITLE),
                    withField("issue", StandardField.NUMBER),
                    withField("page", StandardField.PAGES),
                    withField("volume", StandardField.VOLUME))),
            override("bill", Map.ofEntries(
                    withField("page", StandardField.PAGES))),
            override("book", Map.ofEntries(
                    withField("collection-title", StandardField.SERIES),
                    withField("edition", StandardField.EDITION),
                    withField("ISBN", StandardField.ISBN),
                    withField("publisher", StandardField.PUBLISHER),
                    withField("publisher-place", StandardField.LOCATION),
                    withField("volume", StandardField.VOLUME))),
            override("broadcast", Map.ofEntries(
                    withField("publisher-place", StandardField.LOCATION),
                    withField("publisher", StandardField.PUBLISHER))),
            override("chapter", Map.ofEntries(
                    withField("collection-title", StandardField.SERIES),
                    withField("container-title", StandardField.JOURNALTITLE),
                    withField("edition", StandardField.EDITION),
                    withField("page", StandardField.PAGES),
                    withField("publisher", StandardField.PUBLISHER),
                    withField("publisher-place", StandardField.LOCATION),
                    withField("volume", StandardField.VOLUME))),
            override("dataset", Map.ofEntries(
                    withField("publisher-place", StandardField.LOCATION),
                    withField("publisher", StandardField.PUBLISHER))),
            override("entry-dictionary", Map.ofEntries(
                    withField("publisher-place", StandardField.LOCATION),
                    withField("publisher", StandardField.PUBLISHER))),
            override("entry-encyclopedia", Map.ofEntries(
                    withField("publisher-place", StandardField.LOCATION),
                    withField("publisher", StandardField.PUBLISHER))),
            override("graphic", Map.ofEntries(
                    withField("publisher-place", StandardField.LOCATION),
                    withField("publisher", StandardField.PUBLISHER))),
            override("hearing", Map.ofEntries(
                    withField("publisher-place", StandardField.LOCATION),
                    withField("publisher", StandardField.PUBLISHER))),
            override("instantMessage", Map.ofEntries(
                    withField("publisher-place", StandardField.LOCATION),
                    withField("publisher", StandardField.PUBLISHER))),
            override("interview", Map.ofEntries(
                    withField("publisher-place", StandardField.LOCATION),
                    withField("publisher", StandardField.PUBLISHER))),
            override("legal_case", Map.ofEntries(
                    withField("publisher-place", StandardField.LOCATION),
                    withField("publisher", StandardField.PUBLISHER))),
            override("legislation", Map.ofEntries(
                    withField("publisher-place", StandardField.LOCATION),
                    withField("publisher", StandardField.PUBLISHER))),
            override("manuscript", Map.ofEntries(
                    withField("publisher-place", StandardField.LOCATION),
                    withField("publisher", StandardField.PUBLISHER))),
            override("map", Map.ofEntries(
                    withField("publisher-place", StandardField.LOCATION),
                    withField("publisher", StandardField.PUBLISHER))),
            override("motion_picture", Map.ofEntries(
                    withField("publisher-place", StandardField.LOCATION),
                    withField("publisher", StandardField.PUBLISHER))),
            override("musical_score", Map.ofEntries(
                    withField("publisher-place", StandardField.LOCATION),
                    withField("publisher", StandardField.PUBLISHER))),
            override("paper-conference", Map.ofEntries(
                    withField("collection-title", StandardField.SERIES),
                    withField("container-title", StandardField.JOURNALTITLE),
                    withField("volume", StandardField.VOLUME),
                    withField("event-place", StandardField.LOCATION),
                    withField("page", StandardField.PAGES),
                    withField("publisher", StandardField.PUBLISHER))),
            override("patent", Map.ofEntries(
                    withField("number", StandardField.NUMBER))),
            override("personal_communication", Map.ofEntries(
                    withField("event-place", StandardField.LOCATION))),
            override("post-weblog", Map.ofEntries(
                    withField("publisher", StandardField.ORGANIZATION))),
            override("report", Map.ofEntries(
                    withField("page", StandardField.PAGES),
                    withField("number", StandardField.NUMBER),
                    withField("publisher", StandardField.INSTITUTION),
                    withField("publisher-place", StandardField.LOCATION))),
            override("song", Map.ofEntries(
                    withField("event-place", StandardField.LOCATION),
                    withField("publisher", StandardField.ORGANIZATION))),
            override("speech", Map.ofEntries(
                    withField("event-place", StandardField.LOCATION),
                    withField("publisher", StandardField.ORGANIZATION))),
            override("thesis", Map.ofEntries(
                    withField("publisher", StandardField.INSTITUTION),
                    withField("publisher-place", StandardField.LOCATION))),
            override("webpage", Map.ofEntries(
                    withField("container-title", StandardField.JOURNALTITLE),
                    withField("URL", StandardField.URL)))
    );

    static Optional<EntryType> getEntryType(String zoteroItemType) {
        return Optional.ofNullable(ITEM_TYPES.get(zoteroItemType));
    }

    static Map<String, StandardField> getFieldMappings(String zoteroItemType) {
        Map<String, StandardField> fieldMappings = new HashMap<>(COMMON_FIELDS);
        fieldMappings.putAll(FIELD_MAPPING.getOrDefault(zoteroItemType, Map.of()));
        return fieldMappings;
    }

    private static Map.Entry<String, StandardField> withField(String cslField, StandardField field) {
        return Map.entry(cslField, field);
    }

    private static Map.Entry<String, Map<String, StandardField>> override(String cslType, Map<String, StandardField> fields) {
        return Map.entry(cslType, fields);
    }
}
