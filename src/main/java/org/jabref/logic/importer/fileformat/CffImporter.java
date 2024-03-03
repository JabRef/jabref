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
import org.jabref.model.entry.types.StandardEntryType;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
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

        @JsonProperty("preferred-citation")
        private JsonNode preferredCitation;

        @JsonProperty("type")
        private String type;

        public JsonNode getPreferredCitation() {
            return preferredCitation;
        }

        public CffFormat() {
        }

        public void setPreferredCitation(JsonNode preferredCitation) {
            this.preferredCitation = preferredCitation;
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
        List<BibEntry> entriesList = new ArrayList<>();
        HashMap<Field, String> entryMap = new HashMap<>();
        StandardEntryType entryType = StandardEntryType.Software;

        // Map CFF fields to JabRef Fields
        HashMap<String, Field> fieldMap = getFieldMappings();
        for (Map.Entry<String, String> property : citation.values.entrySet()) {
            if (fieldMap.containsKey(property.getKey())) {
                entryMap.put(fieldMap.get(property.getKey()), property.getValue());
            } else if ("type".equals(property.getKey())) {
                if ("dataset".equals(property.getValue())) {
                    entryType = StandardEntryType.Dataset;
                }
            } else if (getUnmappedFields().contains(property.getKey())) {
                entryMap.put(new UnknownField(property.getKey()), property.getValue());
            }
        }

        // Translate CFF author format to JabRef author format
        String authorStr = citation.authors.stream()
                                           .map(author -> author.values)
                                           .map(vals -> vals.get("name") != null ?
                                                   new Author(vals.get("name"), "", "", "", "") :
                                                   new Author(vals.get("given-names"), null, vals.get("name-particle"),
                                                           vals.get("family-names"), vals.get("name-suffix")))
                                           .collect(AuthorList.collect())
                                           .getAsFirstLastNamesWithAnd();
        entryMap.put(StandardField.AUTHOR, authorStr);

        // Select DOI to keep
        if ((entryMap.get(StandardField.DOI) == null) && (citation.ids != null)) {
            List<CffIdentifier> doiIds = citation.ids.stream()
                                                     .filter(id -> "doi".equals(id.type))
                                                     .collect(Collectors.toList());
            if (doiIds.size() == 1) {
                entryMap.put(StandardField.DOI, doiIds.getFirst().value);
            }
        }

        // Select SWHID to keep
        if (citation.ids != null) {
            List<String> swhIds = citation.ids.stream()
                                              .filter(id -> "swh".equals(id.type))
                                              .map(id -> id.value)
                                              .collect(Collectors.toList());

            if (swhIds.size() == 1) {
                entryMap.put(BiblatexSoftwareField.SWHID, swhIds.getFirst());
            } else if (swhIds.size() > 1) {
                List<String> relSwhIds = swhIds.stream()
                                               .filter(id -> id.split(":").length > 3) // quick filter for invalid swhids
                                               .filter(id -> "rel".equals(id.split(":")[2]))
                                               .collect(Collectors.toList());
                if (relSwhIds.size() == 1) {
                    entryMap.put(BiblatexSoftwareField.SWHID, relSwhIds.getFirst());
                }
            }
        }
        // Handle the main citation as a separate entry
        BibEntry mainEntry = new BibEntry(entryType);
        mainEntry.setField(entryMap);

        HashMap<String, Field> fieldMappings = getFieldMappings();
        // Now handle preferred citation as its own entry
        if (citation.getPreferredCitation() != null) {
            HashMap<Field, String> preferredEntryMap = new HashMap<>();
            processPreferredCitation(citation.getPreferredCitation(), preferredEntryMap, entriesList, fieldMappings);
        }

        BibEntry entry = new BibEntry(entryType);
        entry.setField(entryMap);

        entriesList.add(entry);

        return new ParserResult(entriesList);
    }

    private void processPreferredCitation(JsonNode preferredCitation, HashMap<Field, String> entryMap, List<BibEntry> entriesList, HashMap<String, Field> fieldMappings) {
        if (preferredCitation.isObject()) {
            BibEntry preferredEntry = new BibEntry();
            preferredCitation.fields().forEachRemaining(field -> {
                String key = field.getKey();
                JsonNode value = field.getValue();

                if (fieldMappings.containsKey(key)) {
                    preferredEntry.setField(fieldMappings.get(key), value.asText());
                } else if ("authors".equals(key) && value.isArray()) {
                    preferredEntry.setField(StandardField.AUTHOR, parseAuthors(value));
                } else if ("journal".equals(key)) {
                    preferredEntry.setField(StandardField.JOURNAL, value.asText());
                } else if ("doi".equals(key)) {
                    preferredEntry.setField(StandardField.DOI, value.asText());
                } else if ("year".equals(key)) {
                    preferredEntry.setField(StandardField.YEAR, value.asText());
                } else if ("volume".equals(key)) {
                    preferredEntry.setField(StandardField.VOLUME, value.asText());
                } else if ("issue".equals(key)) {
                    preferredEntry.setField(StandardField.ISSUE, value.asText());
                } else if ("pages".equals(key)) {
                    String pages = value.has("start") && value.has("end")
                            ? value.get("start").asText() + "--" + value.get("end").asText()
                            : value.asText();
                    preferredEntry.setField(StandardField.PAGES, pages);
                }
            });
            if (!preferredEntry.getField(StandardField.TITLE).orElse("").isEmpty()) {
                entriesList.add(preferredEntry);
            }
        }
    }

    private String parseAuthors(JsonNode authorsNode) {
        StringBuilder authors = new StringBuilder();
        for (JsonNode authorNode : authorsNode) {
            String givenNames = authorNode.has("given-names") ? authorNode.get("given-names").asText() : "";
            String familyNames = authorNode.has("family-names") ? authorNode.get("family-names").asText() : "";
            authors.append(givenNames).append(" ").append(familyNames).append(" and ");
        }
        if (authors.lastIndexOf(" and ") == authors.length() - 5) {
            authors.delete(authors.length() - 5, authors.length());
        }
        return authors.toString();
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

    private StandardEntryType mapType(String cffType) {
        return switch (cffType) {
            case "article" -> StandardEntryType.Article;
            case "book" -> StandardEntryType.Book;
            case "conference" -> StandardEntryType.InProceedings;
            case "proceedings" -> StandardEntryType.Proceedings;
            case "misc" -> StandardEntryType.Misc;
            case "manual" -> StandardEntryType.Manual;
            case "software" -> StandardEntryType.Software;
            case "report" -> StandardEntryType.TechReport;
            case "unpublished" -> StandardEntryType.Unpublished;
            default -> StandardEntryType.Dataset;
        };
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
