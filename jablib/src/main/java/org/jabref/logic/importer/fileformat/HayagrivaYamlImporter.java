package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;
import tools.jackson.dataformat.yaml.YAMLMapper;

public class HayagrivaYamlImporter extends Importer {

    private static final Map<String, EntryType> TYPES_MAP = Map.ofEntries(
            Map.entry("article", StandardEntryType.Article),
            Map.entry("book", StandardEntryType.Book),
            Map.entry("chapter", StandardEntryType.InBook),
            Map.entry("report", StandardEntryType.Report),
            Map.entry("thesis", StandardEntryType.Thesis),
            Map.entry("web", StandardEntryType.Online),
            Map.entry("proceedings", StandardEntryType.Proceedings),
            Map.entry("reference", StandardEntryType.Reference),
            Map.entry("anthos", StandardEntryType.InCollection),
            Map.entry("misc", StandardEntryType.Misc)
    );

    @Override
    public String getId() {
        return "hayagrivayaml";
    }

    @Override
    public String getName() {
        return "Hayagriva YAML";
    }

    @Override
    public StandardFileType getFileType() {
        return StandardFileType.YAML;
    }

    @Override
    public String getDescription() {
        return Localization.lang("Importer for the Hayagriva YAML format, which is used by the Typst typesetting system.");
    }

    // POJO classes for YAML deserialization

    private static class HayagrivaEntry {
        @JsonProperty("type")
        String type;

        // title may be a plain string or {value: "...", short: "...", verbatim: true, ...}
        @JsonProperty("title")
        Object title;

        // author/editor may be a single string, a list of strings, or a list of objects (name/given-name)
        @JsonProperty("author")
        List<Object> author;

        @JsonProperty("editor")
        List<Object> editor;

        @JsonProperty("date")
        String date;

        // url may be a plain string or {value: "...", date: "..."}
        @JsonProperty("url")
        Object url;

        @JsonProperty("doi")
        String doi;

        @JsonProperty("isbn")
        String isbn;

        @JsonProperty("issn")
        String issn;

        @JsonProperty("page-range")
        String pageRange;

        @JsonProperty("edition")
        String edition;

        @JsonProperty("location")
        String location;

        @JsonProperty("organization")
        String organization;

        @JsonProperty("institution")
        String institution;

        @JsonProperty("note")
        String note;

        @JsonProperty("volume")
        String volume;

        @JsonProperty("issue")
        String issue;

        @JsonProperty("publisher")
        String publisher;

        @JsonProperty("abstract")
        String abstractText;

        @JsonProperty("serial-number")
        Object serialNumber;

        // parent may be a single object or a list of objects
        @JsonProperty("parent")
        List<HayagrivaParent> parent;

        private final HashMap<String, String> unknownFields = new HashMap<>();

        public HayagrivaEntry() {
        }

        @JsonAnySetter
        private void setUnknownField(String key, Object value) {
            if (value != null) {
                unknownFields.put(key, value.toString());
            }
        }
    }

    private static class HayagrivaParent {
        @JsonProperty("type")
        String type;

        // title may be a plain string or {value: "...", verbatim: true, ...}
        @JsonProperty("title")
        Object title;

        @JsonProperty("volume")
        String volume;

        @JsonProperty("issue")
        String issue;

        @JsonProperty("publisher")
        String publisher;

        public HayagrivaParent() {
        }
    }

    /// Returns the string value from a Hayagriva title or url field.
    /// The field can be a plain string or a map containing a {@code value} key.
    @Nullable
    private static String extractStringValue(@Nullable Object field) {
        if (field instanceof String s) {
            return s;
        }
        if (field instanceof Map<?, ?> map) {
            Object value = map.get("value");
            if (value instanceof String s) {
                return s;
            }
        }
        return null;
    }

    /// Converts an author/editor value (scalar string or list of strings) to a list of name strings.
    /// List elements that are maps (structured person objects) are skipped.
    private static List<String> toStringList(@Nullable Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof String s) {
            return List.of(s);
        }
        if (value instanceof List<?> list) {
            return list.stream()
                       .filter(item -> item instanceof String)
                       .map(item -> (String) item)
                       .toList();
        }
        return List.of();
    }

    private static ObjectMapper createMapper() {
        return YAMLMapper.builder()
                         .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                         .build();
    }

    @Override
    public boolean isRecognizedFormat(@NonNull Reader reader) throws IOException {
        ObjectMapper mapper = new YAMLMapper(new YAMLFactory());
        try {
            Map<String, Object> root = mapper.readValue(reader, new TypeReference<>() { });
            if (root == null || root.isEmpty()) {
                return false;
            }
            return root.values().stream()
                       .anyMatch(v -> v instanceof Map<?, ?> entryMap
                               && (entryMap.containsKey("title") || entryMap.containsKey("type")));
        } catch (JacksonException e) {
            return false;
        }
    }

    @Override
    public ParserResult importDatabase(@NonNull BufferedReader reader) throws IOException {
        ObjectMapper mapper = createMapper();
        Map<String, HayagrivaEntry> entries = mapper.readValue(reader, new TypeReference<>() { });

        if (entries == null) {
            return new ParserResult();
        }

        List<BibEntry> bibEntries = new ArrayList<>();
        for (Map.Entry<String, HayagrivaEntry> yamlEntry : entries.entrySet()) {
            String citationKey = yamlEntry.getKey();
            HayagrivaEntry data = yamlEntry.getValue();
            bibEntries.add(toBibEntry(citationKey, data));
        }
        return new ParserResult(bibEntries);
    }

    private BibEntry toBibEntry(String citationKey, HayagrivaEntry data) {
        EntryType entryType = data.type == null
                ? StandardEntryType.Misc
                : TYPES_MAP.getOrDefault(data.type.toLowerCase(), StandardEntryType.Misc);

        Map<Field, String> fields = new HashMap<>();

        String title = extractStringValue(data.title);
        if (title != null) {
            fields.put(StandardField.TITLE, title);
        }
        List<String> authors = toStringList(data.author);
        if (!authors.isEmpty()) {
            fields.put(StandardField.AUTHOR, String.join(" and ", authors));
        }
        List<String> editors = toStringList(data.editor);
        if (!editors.isEmpty()) {
            fields.put(StandardField.EDITOR, String.join(" and ", editors));
        }
        if (data.date != null) {
            fields.put(StandardField.DATE, data.date);
        }
        String url = extractStringValue(data.url);
        if (url != null) {
            fields.put(StandardField.URL, url);
        }
        if (data.doi != null) {
            fields.put(StandardField.DOI, data.doi);
        }
        if (data.isbn != null) {
            fields.put(StandardField.ISBN, data.isbn);
        }
        if (data.issn != null) {
            fields.put(StandardField.ISSN, data.issn);
        }
        if (data.pageRange != null) {
            fields.put(StandardField.PAGES, data.pageRange);
        }
        if (data.edition != null) {
            fields.put(StandardField.EDITION, data.edition);
        }
        if (data.location != null) {
            fields.put(StandardField.ADDRESS, data.location);
        }
        if (data.organization != null) {
            fields.put(StandardField.ORGANIZATION, data.organization);
        }
        if (data.institution != null) {
            fields.put(StandardField.INSTITUTION, data.institution);
        }
        if (data.note != null) {
            fields.put(StandardField.NOTE, data.note);
        }
        if (data.volume != null) {
            fields.put(StandardField.VOLUME, data.volume);
        }
        if (data.issue != null) {
            fields.put(StandardField.NUMBER, data.issue);
        }
        if (data.publisher != null) {
            fields.put(StandardField.PUBLISHER, data.publisher);
        }
        if (data.abstractText != null) {
            fields.put(StandardField.ABSTRACT, data.abstractText);
        }

        parseSerialNumber(data.serialNumber, fields);
        parseParent(data.parent, entryType, fields);

        for (Map.Entry<String, String> unknown : data.unknownFields.entrySet()) {
            fields.put(new UnknownField(unknown.getKey()), unknown.getValue());
        }

        BibEntry entry = new BibEntry(entryType);
        entry.setCitationKey(citationKey);
        entry.setField(fields);
        return entry;
    }

    /// Flattens the Hayagriva {@code parent} block into flat BibTeX fields.
    /// A {@code periodical} parent provides the journal name, volume, issue, and publisher.
    /// When the parent has no explicit type and the entry is an Article, it is treated as a periodical.
    /// A {@code book} or {@code anthology} parent provides the booktitle.
    /// Any other parent type has its title stored in the series field.
    private void parseParent(@Nullable List<HayagrivaParent> parents, EntryType entryType, Map<Field, String> fields) {
        if (parents == null || parents.isEmpty()) {
            return;
        }
        HayagrivaParent parent = parents.getFirst();
        String parentTitle = extractStringValue(parent.title);
        String parentType = parent.type == null ? "" : parent.type.toLowerCase();

        // Infer periodical when the parent has no type but the entry is clearly an article
        if (parentType.isEmpty() && entryType == StandardEntryType.Article) {
            parentType = "periodical";
        }

        if ("periodical".equals(parentType) || "newspaper".equals(parentType)) {
            if (parentTitle != null) {
                fields.put(StandardField.JOURNAL, parentTitle);
            }
            if (parent.volume != null) {
                fields.putIfAbsent(StandardField.VOLUME, parent.volume);
            }
            if (parent.issue != null) {
                fields.putIfAbsent(StandardField.NUMBER, parent.issue);
            }
            if (parent.publisher != null) {
                fields.putIfAbsent(StandardField.PUBLISHER, parent.publisher);
            }
        } else if ("book".equals(parentType) || "anthology".equals(parentType)) {
            if (parentTitle != null) {
                fields.put(StandardField.BOOKTITLE, parentTitle);
            }
        } else {
            if (parentTitle != null) {
                fields.put(StandardField.SERIES, parentTitle);
            }
        }
    }

    /// Handles the {@code serial-number} field, which may be a plain string or a map of identifier types.
    @SuppressWarnings("unchecked")
    private void parseSerialNumber(@Nullable Object serialNumber, Map<Field, String> fields) {
        if (serialNumber == null) {
            return;
        }
        if (serialNumber instanceof Map<?, ?> idMap) {
            Map<String, Object> ids = (Map<String, Object>) idMap;
            if (ids.containsKey("doi")) {
                fields.putIfAbsent(StandardField.DOI, ids.get("doi").toString());
            }
            if (ids.containsKey("isbn")) {
                fields.putIfAbsent(StandardField.ISBN, ids.get("isbn").toString());
            }
            if (ids.containsKey("issn")) {
                fields.putIfAbsent(StandardField.ISSN, ids.get("issn").toString());
            }
            if (ids.containsKey("pmid")) {
                fields.putIfAbsent(StandardField.PMID, ids.get("pmid").toString());
            }
        }
        // Plain string/number serial-numbers without a known type scheme are not mapped
    }
}
