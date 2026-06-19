package org.jabref.logic.openoffice;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.BiblatexApaEntryType;
import org.jabref.model.entry.types.BiblatexNonStandardEntryType;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.IEEETranEntryType;
import org.jabref.model.entry.types.StandardEntryType;

class CSLItemTypeDefinitions {
    private static final Pattern CONTAINS_DIGIT_PATTERN = Pattern.compile("\\d");

    /// CSL item type <-> Zotero item type mapping can be found via [zotero-schema/schema.json](https://github.com/zotero/zotero-schema/blob/62e983a2e575fe9b9a3677ad7c9772080b67a1e4/schema.json#L3219-L3324)
    /// CSL item type <-> BibLaTeX entry type can be found via [zotero-better-bibtex/biblatex.ts](https://github.com/retorquere/zotero-better-bibtex/blob/master/translators/bibtex/biblatex.ts)
    private static final Map<String, EntryType> CSL2BIB_TYPES = Map.ofEntries(
            // preprint
            Map.entry("article", StandardEntryType.Article),
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
            Map.entry("broadcast", BiblatexNonStandardEntryType.Audio),
            // Book Section
            Map.entry("chapter", StandardEntryType.InBook),
            // Dataset
            Map.entry("dataset", StandardEntryType.Dataset),
            // Dictionary Entry
            Map.entry("entry-dictionary", StandardEntryType.InReference),
            // Encyclopedia Article
            Map.entry("entry-encyclopedia", StandardEntryType.InReference),
            // Artwork
            Map.entry("graphic", BiblatexNonStandardEntryType.Artwork),
            // Hearing
            Map.entry("hearing", BiblatexApaEntryType.Legal),
            // Interview
            Map.entry("interview", BiblatexNonStandardEntryType.Audio),
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
            // Audio Recording
            Map.entry("song", BiblatexNonStandardEntryType.Music),
            // Presentation
            Map.entry("speech", BiblatexNonStandardEntryType.Audio),
            // Standard
            Map.entry("standard", BiblatexNonStandardEntryType.Standard),
            // Thesis
            Map.entry("thesis", StandardEntryType.Thesis),
            // Web Page
            Map.entry("webpage", StandardEntryType.Online)
    );

    /// Fields that each entry contains
    /// `Author` and `Date` are processed in `ZoteroCitationMarkParser`
    private static final Set<String> COMMON_FIELDS = Set.of(
            "DOI",
            "note",
            "title",
            "URL"
    );

    private static final Map<String, Field> CSL2BIB_FIELDS = Map.ofEntries(
            Map.entry("abstract", StandardField.ABSTRACT),
            Map.entry("call-number", StandardField.LIBRARY),
            Map.entry("container-title", StandardField.BOOKTITLE),
            Map.entry("chapter-number", StandardField.CHAPTER),
            Map.entry("collection-number", StandardField.NUMBER),
            Map.entry("collection-title", StandardField.SERIES),
            Map.entry("DOI", StandardField.DOI),
            Map.entry("edition", StandardField.EDITION),
            Map.entry("event-place", StandardField.VENUE),
            Map.entry("ISBN", StandardField.ISBN),
            Map.entry("ISSN", StandardField.ISSN),
            Map.entry("keyword", StandardField.KEYWORDS),
            Map.entry("language", StandardField.LANGUAGE),
            Map.entry("note", StandardField.NOTE),
            Map.entry("number", StandardField.NUMBER),
            Map.entry("number-of-pages", StandardField.PAGETOTAL),
            Map.entry("number-of-volumes", StandardField.VOLUMES),
            Map.entry("page", StandardField.PAGES),
            Map.entry("publisher", StandardField.PUBLISHER),
            Map.entry("publisher-place", StandardField.LOCATION),
            Map.entry("status", StandardField.PUBSTATE),
            Map.entry("title", StandardField.TITLE),
            Map.entry("title-short", StandardField.SHORTTITLE),
            Map.entry("URL", StandardField.URL),
            Map.entry("version", StandardField.VERSION),
            Map.entry("volume", StandardField.VOLUME),
            Map.entry("volume-title", StandardField.ISSUETITLE)
    );

    private static final Map<String, Map<String, Field>> CSL2BIB_FIELD_OVERRIDES = Map.ofEntries(
            Map.entry("article-journal", Map.of("number", StandardField.EID, "container-title", StandardField.JOURNALTITLE)),
            Map.entry("article", Map.of("number", StandardField.EID)),
            Map.entry("article-magazine", Map.of("container-title", StandardField.JOURNALTITLE)),
            Map.entry("article-newspaper", Map.of("container-title", StandardField.JOURNALTITLE)),
            Map.entry("manuscript", Map.of("publisher", StandardField.HOWPUBLISHED)),
            Map.entry("paper-conference", Map.of("publisher", StandardField.ORGANIZATION)),
            Map.entry("report", Map.of("publisher", StandardField.INSTITUTION)),
            Map.entry("thesis", Map.of("publisher", StandardField.INSTITUTION)),
            Map.entry("webpage", Map.of("publisher", StandardField.ORGANIZATION))
    );

    private static final Map<String, Set<String>> CSL_FIELDS_BY_TYPE = Map.ofEntries(
            cslTypeWithFields("article",
                    "container-title",
                    "issue",
                    "number",
                    "page",
                    "ISSN",
                    "volume"),

            cslTypeWithFields("article-journal",
                    "container-title",
                    "issue",
                    "number",
                    "page",
                    "ISSN",
                    "volume"),

            cslTypeWithFields("article-magazine",
                    "container-title",
                    "issue",
                    "number",
                    "page",
                    "ISSN",
                    "volume"),

            cslTypeWithFields("article-newspaper",
                    "container-title",
                    "issue",
                    "number",
                    "page",
                    "ISSN",
                    "volume"),

            cslTypeWithFields("bill",
                    "collection-number",
                    "volume",
                    "page"),

            cslTypeWithFields("book",
                    "collection-number",
                    "abstract",
                    "call-number",
                    "container-title",
                    "edition",
                    "volume",
                    "collection-title",
                    "keyword",
                    "language",
                    "number-of-pages",
                    "number-of-volumes",
                    "publisher-place",
                    "publisher",
                    "page",
                    "ISBN",
                    "URL",
                    "title-short",
                    "version",
                    "ISSN"),

            cslTypeWithFields("broadcast",
                    "collection-number",
                    "publisher-place",
                    "publisher"),
            // author -> creator

            cslTypeWithFields("chapter",
                    "collection-number",
                    "chapter-number",
                    "container-title",
                    "edition",
                    "volume",
                    "collection-title",
                    "publisher-place",
                    "ISBN",
                    "ISSN",
                    "URL",
                    "page",
                    "publisher"),

            cslTypeWithFields("dataset",
                    "collection-number",
                    "publisher-place",
                    "publisher"),

            cslTypeWithFields("entry-dictionary",
                    "collection-number",
                    "collection-title",
                    "volume",
                    "edition",
                    "URL",
                    "publisher-place",
                    "publisher",
                    "page",
                    "ISBN"),

            cslTypeWithFields("entry-encyclopedia",
                    "collection-number",
                    "collection-title",
                    "volume",
                    "edition",
                    "URL",
                    "publisher-place",
                    "publisher",
                    "page",
                    "ISBN"),

            cslTypeWithFields("graphic",
                    "collection-number",
                    "URL",
                    "event-place",
                    "publisher"),

            cslTypeWithFields("hearing",
                    "event-place",
                    "URL",
                    "publisher",
                    "page"),

            cslTypeWithFields("interview",
                    "collection-number",
                    "URL",
                    "publisher-place",
                    "publisher"),

            cslTypeWithFields("legal_case",
                    "collection-number",
                    "URL",
                    "volume",
                    "page"),

            cslTypeWithFields("legislation",
                    "number",
                    "volume",
                    "page",
                    "URL"),

            cslTypeWithFields("manuscript",
                    "collection-number",
                    "URL",
                    "publisher-place",
                    "publisher"),

            cslTypeWithFields("map",
                    "collection-number",
                    "edition",
                    "ISBN",
                    "URL",
                    "publisher-place",
                    "publisher"),

            cslTypeWithFields("motion_picture",
                    "collection-number",
                    "URL",
                    "publisher-place",
                    "publisher"),
            // Author - > director

            cslTypeWithFields("paper-conference",
                    "collection-title",
                    "container-title",
                    "volume",
                    "event-place",
                    "publisher-place",
                    "issue",
                    "page",
                    "ISBN",
                    "ISSN",
                    "URL",
                    "publisher"),

            cslTypeWithFields("patent",
                    "page",
                    "URL",
                    "number",
                    "publisher-place"),

            cslTypeWithFields("personal_communication",
                    "collection-number",
                    "URL",
                    "event-place"),

            cslTypeWithFields("post",
                    "collection-number",
                    "URL",
                    "container-title"),

            cslTypeWithFields("post-weblog",
                    "collection-number",
                    "URL",
                    "container-title"),

            cslTypeWithFields("report",
                    "page",
                    "number",
                    "ISBN",
                    "publisher",
                    "publisher-place"),

            cslTypeWithFields("song",
                    "collection-number",
                    "volume",
                    "ISBN",
                    "event-place",
                    "publisher"),

            cslTypeWithFields("speech",
                    "collection-number",
                    "collection-title",
                    "URL",
                    "event-place"),

            cslTypeWithFields("standard",
                    "publisher-place",
                    "URL"),

            cslTypeWithFields("thesis",
                    "collection-number",
                    "collection-title",
                    "ISBN",
                    "URL",
                    "ISSN",
                    "publisher",
                    "publisher-place"),

            cslTypeWithFields("webpage",
                    "collection-number",
                    "URL",
                    "publisher",
                    "container-title")
    );

    static EntryType getEntryType(String cslItemType) {
        return CSL2BIB_TYPES.getOrDefault(cslItemType, StandardEntryType.Misc);
    }

    static Map<String, Field> getFieldMappings(String cslItemType, ZoteroCitationData.ItemData itemData) {
        Map<String, Field> fieldMappings = new HashMap<>();
        addFieldMappings(fieldMappings, cslItemType, itemData, COMMON_FIELDS);
        addFieldMappings(fieldMappings, cslItemType, itemData, CSL_FIELDS_BY_TYPE.getOrDefault(cslItemType, Set.of()));

        return fieldMappings;
    }

    private static void addFieldMappings(Map<String, Field> fieldMappings,
                                         String cslItemType,
                                         ZoteroCitationData.ItemData itemData,
                                         Set<String> cslFields) {
        for (String cslField : cslFields) {
            getBibField(cslItemType, cslField, itemData)
                    .ifPresent(bibField -> fieldMappings.put(cslField, bibField));
        }
    }

    private static Optional<Field> getBibField(String cslItemType, String cslField, ZoteroCitationData.ItemData itemData) {
        String cslValue = itemData.getFieldValue(cslField);

        if ("issue".equals(cslField)) {
            return getIssueField(cslItemType, cslValue);
        }

        return Optional.ofNullable(CSL2BIB_FIELD_OVERRIDES.getOrDefault(cslItemType, Map.of()).get(cslField))
                       .or(() -> Optional.ofNullable(CSL2BIB_FIELDS.get(cslField)));
    }

    private static Optional<Field> getIssueField(String cslItemType, String cslValue) {
        if (StringUtil.isBlank(cslValue)) {
            return Optional.empty();
        }

        boolean containsDigit = CONTAINS_DIGIT_PATTERN.matcher(cslValue).find();
        return switch (cslItemType) {
            case "article",
                 "article-journal",
                 "article-magazine",
                 "article-newspaper" ->
                    Optional.of(containsDigit ? StandardField.NUMBER : StandardField.ISSUE);
            case "paper-conference" ->
                    containsDigit ? Optional.of(StandardField.NUMBER) : Optional.empty();
            default ->
                    Optional.empty();
        };
    }

    private static Map.Entry<String, Set<String>> cslTypeWithFields(String cslType, String... cslFields) {
        return Map.entry(cslType, Set.of(cslFields));
    }
}
