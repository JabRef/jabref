package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
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

public class CffImporter extends Importer {

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
        return "Importer for the CFF format. Is only used to cite software, one entry per file. Can also " +
                "cite a preferred citation.";
    }

    // POJO classes for yaml data
    private static class CffFormat {
        private final HashMap<String, String> values = new HashMap<>();

        @JsonProperty("authors")
        private List<CffAuthor> authors;

        @JsonProperty("identifiers")
        private List<CffIdentifier> ids;

        @JsonProperty("preferred-citation")
        private CffPreferredCitation citation;

        public CffFormat() {
        }

        @JsonAnySetter
        private void setValues(String key, String value) {
            values.put(key, value);
        }
    }

    private static class CffAuthor {
        private final HashMap<String, String> values = new HashMap<>();

        public CffAuthor() {
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

    private static class CffPreferredCitation {
        private final HashMap<String, String> values = new HashMap<>();

        @JsonProperty("type")
        private String type;

        @JsonProperty("authors")
        private List<CffAuthor> authors;

        public CffPreferredCitation() {
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

        // Retrieve mappings from CFF to JabRef
        HashMap<String, Field> fieldMap = getFieldMappings();
        HashMap<String, EntryType> typeMap = getTypeMappings();

        // Parse main entry
        HashMap<Field, String> entryMap = new HashMap<>();
        EntryType entryType = typeMap.getOrDefault(citation.values.get("type"), StandardEntryType.Software);

        // Map CFF fields to JabRef Fields
        for (Map.Entry<String, String> property : citation.values.entrySet()) {
            if (fieldMap.containsKey(property.getKey())) {
                entryMap.put(fieldMap.get(property.getKey()), property.getValue());
            } else if (getUnmappedFields().contains(property.getKey())) {
                entryMap.put(new UnknownField(property.getKey()), property.getValue());
            }
        }

        // Translate CFF author format to JabRef author format
        entryMap.put(StandardField.AUTHOR, parseAuthors(citation.authors));

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

        // Handle `preferred-citation` field
        if (citation.citation != null) {
            HashMap<Field, String> preferredEntryMap = new HashMap<>();
            EntryType preferredEntryType = typeMap.getOrDefault(citation.citation.type, StandardEntryType.Article);
            for (Map.Entry<String, String> property : citation.citation.values.entrySet()) {
                if (fieldMap.containsKey(property.getKey())) {
                    preferredEntryMap.put(fieldMap.get(property.getKey()), property.getValue());
                }
            }

            preferredEntryMap.put(StandardField.AUTHOR, parseAuthors(citation.citation.authors));
            BibEntry preferredEntry = new BibEntry(preferredEntryType);
            preferredEntry.setField(preferredEntryMap);
            entriesList.add(preferredEntry);
        }

        return new ParserResult(entriesList);
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

    private HashMap<String, Field> getFieldMappings() {
        HashMap<String, Field> fieldMappings = new HashMap<>();
        fieldMappings.put("title", StandardField.TITLE);
        fieldMappings.put("version", StandardField.VERSION);
        fieldMappings.put("doi", StandardField.DOI);
        fieldMappings.put("license", BiblatexSoftwareField.LICENSE);
        fieldMappings.put("repository", BiblatexSoftwareField.REPOSITORY);
        fieldMappings.put("url", StandardField.URL);
        fieldMappings.put("abstract", StandardField.ABSTRACT);
        fieldMappings.put("message", StandardField.COMMENT);
        fieldMappings.put("date-released", StandardField.DATE);
        fieldMappings.put("keywords", StandardField.KEYWORDS);

        // specific to preferred-citation
        fieldMappings.put("month", StandardField.MONTH);
        fieldMappings.put("year", StandardField.YEAR);
        return fieldMappings;
    }

    private HashMap<String, EntryType> getTypeMappings() {
        HashMap<String, EntryType> typeMappings = new HashMap<>();
        typeMappings.put("article", StandardEntryType.Article);
        typeMappings.put("book", StandardEntryType.Book);
        typeMappings.put("pamphlet", StandardEntryType.Booklet);
        typeMappings.put("conference-paper", StandardEntryType.InProceedings);
        typeMappings.put("misc", StandardEntryType.Misc);
        typeMappings.put("manual", StandardEntryType.Manual);
        typeMappings.put("software", StandardEntryType.Software);
        typeMappings.put("dataset", StandardEntryType.Dataset);
        typeMappings.put("report", StandardEntryType.Report);
        typeMappings.put("unpublished", StandardEntryType.Unpublished);
        return typeMappings;
    }

    private List<String> getUnmappedFields() {
        List<String> fields = new ArrayList<>();

        fields.add("commit");
        fields.add("license-url");
        fields.add("repository-code");
        fields.add("repository-artifact");

        return fields;
    }

    private String parseAuthors(List<CffAuthor> authors) {
        return authors.stream()
                      .map(author -> author.values)
                      .map(vals -> vals.get("name") != null ?
                              new Author(vals.get("name"), "", "", "", "") :
                              new Author(vals.get("given-names"), null, vals.get("name-particle"),
                                      vals.get("family-names"), vals.get("name-suffix")))
                      .collect(AuthorList.collect())
                      .getAsFirstLastNamesWithAnd();
    }
}
