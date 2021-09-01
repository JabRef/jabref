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
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
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
        return "Importer for the CFF format. Is only used to cite software, one entry per file.";
    }

    // POJO classes for yaml data
    private static class CffFormat {
        private final HashMap<String, String> values = new HashMap<>();

        @JsonProperty("authors")
        private List<CffAuthor> authors;

        @JsonProperty("identifiers")
        private List<CffIdentifier> ids;

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

    @Override
    public ParserResult importDatabase(BufferedReader reader) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        CffFormat citation = mapper.readValue(reader, CffFormat.class);
        HashMap<Field, String> entryMap = new HashMap<>();
        StandardEntryType entryType = StandardEntryType.Software;

        // Map CFF fields to JabRef Fields
        HashMap<String, StandardField> fieldMap = getFieldMappings();
        for (Map.Entry<String, String> property : citation.values.entrySet()) {
            if (fieldMap.containsKey(property.getKey())) {
                entryMap.put(fieldMap.get(property.getKey()), property.getValue());
            } else if (property.getKey().equals("type")) {
                if (property.getValue().equals("dataset")) {
                    entryType = StandardEntryType.Dataset;
                }
            } else if (getUnmappedFields().contains(property.getKey())) {
                entryMap.put(new UnknownField(property.getKey()), property.getValue());
            }
        }

        // Translate CFF author format to JabRef author format
        String authorStr = citation.authors.stream()
                        .map((author) -> author.values)
                        .map((vals) -> vals.get("name") != null ?
                                new Author(vals.get("name"), "", "", "", "") :
                                new Author(vals.get("given-names"), null, vals.get("name-particle"),
                                        vals.get("family-names"), vals.get("name-suffix")))
                        .collect(AuthorList.collect())
                        .getAsFirstLastNamesWithAnd();
        entryMap.put(StandardField.AUTHOR, authorStr);

        // Select DOI to keep
        if (entryMap.get(StandardField.DOI) == null && citation.ids != null) {
            List<CffIdentifier> doiIds = citation.ids.stream()
                            .filter(id -> id.type.equals("doi"))
                            .collect(Collectors.toList());
            if (doiIds.size() == 1) {
                entryMap.put(StandardField.DOI, doiIds.get(0).value);
            }
        }

        // Select SWHID to keep
        if (citation.ids != null) {
            List<String> swhIds = citation.ids.stream()
                                           .filter(id -> id.type.equals("swh"))
                                           .map(id -> id.value)
                                           .collect(Collectors.toList());

            if (swhIds.size() == 1) {
                entryMap.put(StandardField.SWHID, swhIds.get(0));
            } else if (swhIds.size() > 1) {
                List<String> relSwhIds = swhIds.stream()
                                               .filter(id -> id.split(":").length > 3) // quick filter for invalid swhids
                                               .filter(id -> id.split(":")[2].equals("rel"))
                                               .collect(Collectors.toList());
                if (relSwhIds.size() == 1) {
                    entryMap.put(StandardField.SWHID, relSwhIds.get(0));
                }
            }
        }

        BibEntry entry = new BibEntry(entryType);
        entry.setField(entryMap);

        List<BibEntry> entriesList = new ArrayList<>();
        entriesList.add(entry);

        return new ParserResult(entriesList);
    }

    @Override
    public boolean isRecognizedFormat(BufferedReader reader) throws IOException {

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        CffFormat citation;

        try {
            citation = mapper.readValue(reader, CffFormat.class);
            return citation != null && citation.values.get("title") != null;
        } catch (IOException e) {
            return false;
        }
    }

    private HashMap<String, StandardField> getFieldMappings() {
        HashMap<String, StandardField> fieldMappings = new HashMap<>();
        fieldMappings.put("title", StandardField.TITLE);
        fieldMappings.put("version", StandardField.VERSION);
        fieldMappings.put("doi", StandardField.DOI);
        fieldMappings.put("license", StandardField.LICENSE);
        fieldMappings.put("repository", StandardField.REPOSITORY);
        fieldMappings.put("url", StandardField.URL);
        fieldMappings.put("abstract", StandardField.ABSTRACT);
        fieldMappings.put("message", StandardField.COMMENT);
        fieldMappings.put("date-released", StandardField.DATE);
        fieldMappings.put("keywords", StandardField.KEYWORDS);
        return fieldMappings;
    }

    private List<String> getUnmappedFields() {
        List<String> fields = new ArrayList<>();

        fields.add("commit");
        fields.add("license-url");
        fields.add("repository-code");
        fields.add("repository-artifact");

        return fields;
    }
}
