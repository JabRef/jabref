package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.exporter.CffExporter;
import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.BiblatexSoftwareField;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.HashBiMap;

public class CffImporter extends Importer {

    public static final Map<String, Field> FIELDS_MAP = HashBiMap.create(CffExporter.FIELDS_MAP).inverse();
    public static final Map<String, EntryType> TYPES_MAP = HashBiMap.create(CffExporter.TYPES_MAP).inverse();

    private final CitationKeyPatternPreferences citationKeyPatternPreferences;

    public CffImporter(CitationKeyPatternPreferences citationKeyPatternPreferences) {
        this.citationKeyPatternPreferences = citationKeyPatternPreferences;
    }

    @Override
    public String getName() {
        return "CFF";
    }

    @Override
    public StandardFileType getFileType() {
        return StandardFileType.CFF;
    }

    @Override
    public String getId() {
        return "cff";
    }

    @Override
    public String getDescription() {
        return Localization.lang("Importer for the CFF format, which is intended to make software and datasets citable.");
    }

    // POJO classes for yaml data
    private static class CffFormat {
        private final HashMap<String, String> values = new HashMap<>();

        @JsonProperty("authors")
        private List<CffEntity> authors;

        @JsonProperty("identifiers")
        private List<CffIdentifier> ids;

        @JsonProperty("keywords")
        private List<String> keywords;

        @JsonProperty("preferred-citation")
        private CffReference preferred;

        @JsonProperty("references")
        private List<CffReference> references;

        public CffFormat() {
        }

        @JsonAnySetter
        private void setValues(String key, String value) {
            values.put(key, value);
        }
    }

    private static class CffEntity {
        private final HashMap<String, String> values = new HashMap<>();

        public CffEntity() {
        }

        @JsonAnySetter
        private void setValues(String key, String value) {
            values.put(key, value);
        }
    }

    private static class CffIdentifier {
        @JsonProperty("type")
        private String type;
        @JsonProperty("value")
        private String value;

        public CffIdentifier() {
        }
    }

    private static class CffReference {
        private final HashMap<String, String> values = new HashMap<>();

        @JsonProperty("authors")
        private List<CffEntity> authors;

        @JsonProperty("conference")
        private CffEntity conference;

        @JsonProperty("contact")
        private CffEntity contact;

        @JsonProperty("editors")
        private List<CffEntity> editors;

        @JsonProperty("editors-series")
        private List<CffEntity> editorsSeries;

        @JsonProperty("database-provider")
        private CffEntity databaseProvider;

        @JsonProperty("institution")
        private CffEntity institution;

        @JsonProperty("keywords")
        private List<String> keywords;

        @JsonProperty("languages")
        private List<String> languages;

        @JsonProperty("location")
        private CffEntity location;

        @JsonProperty("publisher")
        private CffEntity publisher;

        @JsonProperty("recipients")
        private List<CffEntity> recipients;

        @JsonProperty("senders")
        private List<CffEntity> senders;

        @JsonProperty("translators")
        private List<CffEntity> translators;

        @JsonProperty("type")
        private String type;

        public CffReference() {
        }

        @JsonAnySetter
        private void setValues(String key, String value) {
            values.put(key, value);
        }
    }

    @Override
    public ParserResult importDatabase(BufferedReader reader) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        CffFormat citation = mapper.readValue(reader, CffFormat.class);
        List<BibEntry> entriesList = new ArrayList<>();

        // Remove CFF version and type
        citation.values.remove("cff-version");

        // Parse main entry
        HashMap<Field, String> entryMap = new HashMap<>();
        EntryType entryType = TYPES_MAP.getOrDefault(citation.values.get("type"), StandardEntryType.Software);
        citation.values.remove("type");

        // Translate CFF author format to JabRef author format
        entryMap.put(StandardField.AUTHOR, parseAuthors(citation.authors));

        // Parse keywords
        if (citation.keywords != null) {
            entryMap.put(StandardField.KEYWORDS, String.join(", ", citation.keywords));
        }

        // Map CFF simple fields to JabRef Fields
        parseFields(citation.values, entryMap);

        // Select DOI to keep
        if ((entryMap.get(StandardField.DOI) == null) && (citation.ids != null)) {
            List<CffIdentifier> doiIds = citation.ids.stream()
                            .filter(id -> "doi".equals(id.type))
                            .toList();
            if (doiIds.size() == 1) {
                entryMap.put(StandardField.DOI, doiIds.getFirst().value);
            }
        }

        // Select SWHID to keep
        if (citation.ids != null) {
            List<String> swhIds = citation.ids.stream()
                                           .filter(id -> "swh".equals(id.type))
                                           .map(id -> id.value)
                                           .toList();

            if (swhIds.size() == 1) {
                entryMap.put(BiblatexSoftwareField.SWHID, swhIds.getFirst());
            } else if (swhIds.size() > 1) {
                List<String> relSwhIds = swhIds.stream()
                                               .filter(id -> id.split(":").length > 3) // quick filter for invalid swhids
                                               .filter(id -> "rel".equals(id.split(":")[2]))
                                               .toList();
                if (relSwhIds.size() == 1) {
                    entryMap.put(BiblatexSoftwareField.SWHID, relSwhIds.getFirst());
                }
            }
        }

        BibEntry entry = new BibEntry(entryType);
        entry.setField(entryMap);
        entriesList.add(entry);

        // Handle `preferred-citation` and `references` fields
        BibEntry preferred = null;
        List<BibEntry> references = null;

        if (citation.preferred != null) {
            preferred = parseEntry(citation.preferred);
            entriesList.add(preferred);
        }

        if (citation.references != null) {
            references = citation.references.stream().map(this::parseEntry).toList();
            entriesList.addAll(references);
        }

        ParserResult res = new ParserResult(entriesList);
        CitationKeyGenerator gen = new CitationKeyGenerator(res.getDatabaseContext(), citationKeyPatternPreferences);

        if (preferred != null) {
            gen.generateAndSetKey(preferred);
            entry.setField(StandardField.CITES, preferred.getCitationKey().orElse(""));
        }

        if (references != null) {
            references.forEach(ref -> {
                gen.generateAndSetKey(ref);
                String citeKey = ref.getCitationKey().orElse("");
                String related = entry.getField(StandardField.RELATED).orElse("");
                entry.setField(StandardField.RELATED, related.isEmpty() ? citeKey : related + "," + citeKey);
            });
        }
        return res;
    }

    @Override
    public boolean isRecognizedFormat(BufferedReader reader) throws IOException {

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        CffFormat citation;

        try {
            citation = mapper.readValue(reader, CffFormat.class);
            return (citation != null) && (citation.values.get("title") != null);
        } catch (IOException e) {
            return false;
        }
    }

    private String parseAuthors(List<CffEntity> authors) {
        return authors.stream()
                      .map(author -> author.values)
                      .map(vals -> vals.get("name") != null ?
                              new Author(vals.get("name"), "", "", "", "") :
                              new Author(vals.get("given-names"), null, vals.get("name-particle"),
                                      vals.get("family-names"), vals.get("name-suffix")))
                      .collect(AuthorList.collect())
                      .getAsFirstLastNamesWithAnd();
    }

    private BibEntry parseEntry(CffReference reference) {
        Map<Field, String> entryMap = new HashMap<>();
        EntryType entryType = TYPES_MAP.getOrDefault(reference.type, StandardEntryType.Article);
        entryMap.put(StandardField.AUTHOR, parseAuthors(reference.authors));
        parseFields(reference.values, entryMap);
        BibEntry entry = new BibEntry(entryType);
        entry.setField(entryMap);
        return entry;
    }

    private void parseFields(Map<String, String> values, Map<Field, String> entryMap) {
        for (Map.Entry<String, String> property : values.entrySet()) {
            if (FIELDS_MAP.containsKey(property.getKey())) {
                entryMap.put(FIELDS_MAP.get(property.getKey()), property.getValue());
            } else {
                entryMap.put(new UnknownField(property.getKey()), property.getValue());
            }
        }
    }
}
