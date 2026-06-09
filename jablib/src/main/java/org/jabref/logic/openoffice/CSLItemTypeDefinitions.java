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
            Map.entry("article-journal", StandardEntryType.Article),
            Map.entry("article-magazine", StandardEntryType.Article),
            Map.entry("article-newspaper", StandardEntryType.Article),
            Map.entry("bill", BiblatexApaEntryType.Legislation),
            Map.entry("book", StandardEntryType.Book),
            Map.entry("broadcast", StandardEntryType.Misc),
            Map.entry("chapter", StandardEntryType.InCollection),
            Map.entry("data", StandardEntryType.Dataset),
            Map.entry("dataset", StandardEntryType.Dataset),
            Map.entry("entry-dictionary", StandardEntryType.InReference),
            Map.entry("entry-encyclopedia", StandardEntryType.InReference),
            Map.entry("figure", BiblatexNonStandardEntryType.Image),
            Map.entry("graphic", BiblatexNonStandardEntryType.Image),
            Map.entry("hearing", BiblatexApaEntryType.Jurisdiction),
            Map.entry("instantMessage", StandardEntryType.Misc),
            Map.entry("interview", StandardEntryType.Misc),
            Map.entry("legal_case", BiblatexApaEntryType.Jurisdiction),
            Map.entry("legislation", BiblatexApaEntryType.Legislation),
            Map.entry("manuscript", StandardEntryType.Unpublished),
            Map.entry("map", StandardEntryType.Misc),
            Map.entry("motion_picture", BiblatexNonStandardEntryType.Movie),
            Map.entry("musical_score", BiblatexNonStandardEntryType.Audio),
            Map.entry("pamphlet", StandardEntryType.Booklet),
            Map.entry("paper-conference", StandardEntryType.InProceedings),
            Map.entry("patent", IEEETranEntryType.Patent),
            Map.entry("personal_communication", BiblatexNonStandardEntryType.Letter),
            Map.entry("post-weblog", StandardEntryType.Online),
            Map.entry("report", StandardEntryType.Report),
            Map.entry("review", BiblatexNonStandardEntryType.Review),
            Map.entry("review-book", BiblatexNonStandardEntryType.Review),
            Map.entry("song", BiblatexNonStandardEntryType.Music),
            Map.entry("speech", StandardEntryType.Misc),
            Map.entry("thesis", StandardEntryType.Thesis),
            Map.entry("treaty", BiblatexApaEntryType.Legal),
            Map.entry("webpage", StandardEntryType.Online)
    );

    private static final Map<String, StandardField> COMMON_FIELDS = Map.ofEntries(
            field("title", StandardField.TITLE),
            field("DOI", StandardField.DOI),
            field("URL", StandardField.URL)
    );

    private static final Map<String, Map<String, StandardField>> FIELD_MAPPING = Map.ofEntries(
            override("article-journal", Map.ofEntries(
                    field("container-title", StandardField.JOURNALTITLE),
                    field("issue", StandardField.NUMBER),
                    field("page", StandardField.PAGES),
                    field("volume", StandardField.VOLUME))),
            override("article-magazine", Map.ofEntries(
                    field("container-title", StandardField.JOURNALTITLE),
                    field("issue", StandardField.NUMBER),
                    field("page", StandardField.PAGES),
                    field("volume", StandardField.VOLUME))),
            override("article-newspaper", Map.ofEntries(
                    field("container-title", StandardField.JOURNALTITLE),
                    field("issue", StandardField.NUMBER),
                    field("page", StandardField.PAGES),
                    field("volume", StandardField.VOLUME))),
            override("book", Map.ofEntries(
                    field("collection-title", StandardField.SERIES),
                    field("edition", StandardField.EDITION),
                    field("ISBN", StandardField.ISBN),
                    field("publisher", StandardField.PUBLISHER),
                    field("publisher-place", StandardField.LOCATION),
                    field("volume", StandardField.VOLUME))),
            override("chapter", Map.ofEntries(
                    field("collection-title", StandardField.SERIES),
                    field("container-title", StandardField.BOOKTITLE),
                    field("edition", StandardField.EDITION),
                    field("ISBN", StandardField.ISBN),
                    field("page", StandardField.PAGES),
                    field("publisher", StandardField.PUBLISHER),
                    field("publisher-place", StandardField.LOCATION),
                    field("volume", StandardField.VOLUME))),
            override("entry-dictionary", Map.ofEntries(
                    field("collection-title", StandardField.SERIES),
                    field("container-title", StandardField.BOOKTITLE),
                    field("edition", StandardField.EDITION),
                    field("ISBN", StandardField.ISBN),
                    field("page", StandardField.PAGES),
                    field("publisher", StandardField.PUBLISHER),
                    field("publisher-place", StandardField.LOCATION),
                    field("volume", StandardField.VOLUME))),
            override("entry-encyclopedia", Map.ofEntries(
                    field("collection-title", StandardField.SERIES),
                    field("container-title", StandardField.BOOKTITLE),
                    field("edition", StandardField.EDITION),
                    field("ISBN", StandardField.ISBN),
                    field("page", StandardField.PAGES),
                    field("publisher", StandardField.PUBLISHER),
                    field("publisher-place", StandardField.LOCATION),
                    field("volume", StandardField.VOLUME))),
            override("paper-conference", Map.ofEntries(
                    field("collection-title", StandardField.SERIES),
                    field("container-title", StandardField.BOOKTITLE),
                    field("event-place", StandardField.LOCATION),
                    field("event-title", StandardField.EVENTTITLE),
                    field("ISBN", StandardField.ISBN),
                    field("page", StandardField.PAGES),
                    field("publisher", StandardField.PUBLISHER))),
            override("post-weblog", Map.ofEntries(
                    field("publisher", StandardField.ORGANIZATION))),
            override("webpage", Map.ofEntries(
                    field("publisher", StandardField.ORGANIZATION))),
            override("report", Map.ofEntries(
                    field("genre", StandardField.TYPE),
                    field("issue", StandardField.NUMBER),
                    field("page", StandardField.PAGES),
                    field("publisher", StandardField.INSTITUTION),
                    field("publisher-place", StandardField.LOCATION))),
            override("thesis", Map.ofEntries(
                    field("genre", StandardField.TYPE),
                    field("publisher", StandardField.INSTITUTION),
                    field("publisher-place", StandardField.LOCATION))),
            override("patent", Map.ofEntries(
                    field("genre", StandardField.TYPE),
                    field("number", StandardField.NUMBER))),
            override("bill", Map.ofEntries(
                    field("number", StandardField.NUMBER),
                    field("publisher", StandardField.ORGANIZATION),
                    field("publisher-place", StandardField.LOCATION))),
            override("hearing", Map.ofEntries(
                    field("number", StandardField.NUMBER),
                    field("publisher", StandardField.ORGANIZATION),
                    field("publisher-place", StandardField.LOCATION))),
            override("legal_case", Map.ofEntries(
                    field("number", StandardField.NUMBER),
                    field("publisher", StandardField.ORGANIZATION),
                    field("publisher-place", StandardField.LOCATION))),
            override("legislation", Map.ofEntries(
                    field("number", StandardField.NUMBER),
                    field("publisher", StandardField.ORGANIZATION),
                    field("publisher-place", StandardField.LOCATION))),
            override("treaty", Map.ofEntries(
                    field("number", StandardField.NUMBER),
                    field("publisher", StandardField.ORGANIZATION),
                    field("publisher-place", StandardField.LOCATION))),
            override("broadcast", Map.ofEntries(
                    field("genre", StandardField.TYPE),
                    field("publisher", StandardField.ORGANIZATION))),
            override("data", Map.ofEntries(
                    field("genre", StandardField.TYPE),
                    field("publisher", StandardField.ORGANIZATION))),
            override("dataset", Map.ofEntries(
                    field("genre", StandardField.TYPE),
                    field("publisher", StandardField.ORGANIZATION))),
            override("figure", Map.ofEntries(
                    field("genre", StandardField.TYPE),
                    field("publisher", StandardField.ORGANIZATION))),
            override("graphic", Map.ofEntries(
                    field("genre", StandardField.TYPE),
                    field("publisher", StandardField.ORGANIZATION))),
            override("instantMessage", Map.ofEntries(
                    field("genre", StandardField.TYPE),
                    field("publisher", StandardField.ORGANIZATION))),
            override("interview", Map.ofEntries(
                    field("genre", StandardField.TYPE),
                    field("publisher", StandardField.ORGANIZATION))),
            override("manuscript", Map.ofEntries(
                    field("genre", StandardField.TYPE),
                    field("publisher", StandardField.ORGANIZATION))),
            override("map", Map.ofEntries(
                    field("genre", StandardField.TYPE),
                    field("publisher", StandardField.ORGANIZATION))),
            override("motion_picture", Map.ofEntries(
                    field("genre", StandardField.TYPE),
                    field("publisher", StandardField.ORGANIZATION))),
            override("musical_score", Map.ofEntries(
                    field("genre", StandardField.TYPE),
                    field("publisher", StandardField.ORGANIZATION))),
            override("pamphlet", Map.ofEntries(
                    field("genre", StandardField.TYPE),
                    field("publisher", StandardField.ORGANIZATION))),
            override("personal_communication", Map.ofEntries(
                    field("genre", StandardField.TYPE),
                    field("publisher", StandardField.ORGANIZATION))),
            override("review", Map.ofEntries(
                    field("genre", StandardField.TYPE),
                    field("publisher", StandardField.ORGANIZATION))),
            override("review-book", Map.ofEntries(
                    field("genre", StandardField.TYPE),
                    field("publisher", StandardField.ORGANIZATION))),
            override("song", Map.ofEntries(
                    field("genre", StandardField.TYPE),
                    field("publisher", StandardField.ORGANIZATION))),
            override("speech", Map.ofEntries(
                    field("genre", StandardField.TYPE),
                    field("publisher", StandardField.ORGANIZATION)))
    );

    static Optional<EntryType> getEntryType(String zoteroItemType) {
        return Optional.of(ITEM_TYPES.get(zoteroItemType));
    }

    static Map<String, StandardField> getFieldMappings(String zoteroItemType) {
        Map<String, StandardField> fieldMappings = new HashMap<>(COMMON_FIELDS);
        fieldMappings.putAll(FIELD_MAPPING.getOrDefault(zoteroItemType, Map.of()));
        return fieldMappings;
    }

    private static Map.Entry<String, StandardField> field(String cslField, StandardField field) {
        return Map.entry(cslField, field);
    }

    private static Map.Entry<String, Map<String, StandardField>> override(String cslType, Map<String, StandardField> fields) {
        return Map.entry(cslType, fields);
    }
}
