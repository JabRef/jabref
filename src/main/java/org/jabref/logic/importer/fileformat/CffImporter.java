package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.BiblatexSoftwareField;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
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

        public CffFormat() {
        }

        public JsonNode getPreferredCitation() {
            return preferredCitation;
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

        StandardEntryType entryType = StandardEntryType.Misc;
        if (citation.type != null) {
            entryType = mapType(citation.type);
        }
        BibEntry entry = new BibEntry(entryType);
        HashMap<Field, String> entryMap = new HashMap<>();

        if (citation.getPreferredCitation() != null) {
            PreferredCitationMethod(citation.getPreferredCitation(), entryMap, entry);
        }

        MainCffContentMethod(citation, entryMap, entry);
        entryMap.forEach(entry::setField);

        List<BibEntry> entriesList = new ArrayList<>();
        entriesList.add(entry);

        return new ParserResult(entriesList);
    }

    private void PreferredCitationMethod(JsonNode preferredCitation, Map<Field, String> entryMap, BibEntry entry) {
        if (preferredCitation != null) {
            if (preferredCitation.has("title")) {
                entryMap.put(StandardField.TITLE, preferredCitation.get("title").asText());
            }
            if (preferredCitation.has("doi")) {
                entryMap.put(StandardField.DOI, preferredCitation.get("doi").asText());
            }
            if (preferredCitation.has("authors")) {
                List<String> authorsList = new ArrayList<>();
                preferredCitation.get("authors").forEach(authorNode -> {
                    String givenName = authorNode.has("given-names") ? authorNode.get("given-names").asText() : "";
                    String familyName = authorNode.has("family-names") ? authorNode.get("family-names").asText() : "";
                    authorsList.add((givenName + " " + familyName).trim());
                });
                String authors = String.join(" and ", authorsList);
                entryMap.put(StandardField.AUTHOR, authors);
            }
            if (preferredCitation.has("journal")) {
                entryMap.put(StandardField.JOURNAL, preferredCitation.get("journal").asText());
            }
            if (preferredCitation.has("volume")) {
                entryMap.put(StandardField.VOLUME, preferredCitation.get("volume").asText());
            }
            if (preferredCitation.has("issue")) {
                entryMap.put(StandardField.ISSUE, preferredCitation.get("issue").asText());
            }
            if (preferredCitation.has("year")) {
                entryMap.put(StandardField.YEAR, preferredCitation.get("year").asText());
            }
            if (preferredCitation.has("start") && preferredCitation.has("end")) {
                String pages = preferredCitation.get("start").asText() + "-" + preferredCitation.get("end").asText();
                entryMap.put(StandardField.PAGES, pages);
            }
            if (preferredCitation.has("type")) {
                String typeValue = preferredCitation.get("type").asText();
                StandardEntryType entryType = mapType(typeValue);
                entry.setType(entryType);
            }
        }
    }

    private void MainCffContentMethod(CffFormat citation, Map<Field, String> entryMap, BibEntry entry) {
        if (!entryMap.containsKey(StandardField.TITLE) && citation.values.containsKey("title")) {
            entryMap.put(StandardField.TITLE, citation.values.get("title"));
        }
        if (!entryMap.containsKey(StandardField.AUTHOR) && citation.authors != null && !citation.authors.isEmpty()) {
            List<String> authorsList = new ArrayList<>();
            for (CffAuthor author : citation.authors) {
                String givenName = author.values.getOrDefault("given-names", "");
                String familyName = author.values.getOrDefault("family-names", "");
                authorsList.add((givenName + " " + familyName).trim());
            }
            String authors = String.join(" and ", authorsList);
            entryMap.put(StandardField.AUTHOR, authors);
        }
        if (!entryMap.containsKey(StandardField.DOI) && citation.values.containsKey("doi")) {
            entryMap.put(StandardField.DOI, citation.values.get("doi"));
        }
        if (!entryMap.containsKey(StandardField.VERSION) && citation.values.containsKey("version")) {
            entryMap.put(StandardField.VERSION, citation.values.get("version"));
        }
        if (!entryMap.containsKey(StandardField.YEAR) && citation.values.containsKey("date-released")) {
            String dateReleased = citation.values.get("date-released");
            String year = dateReleased.split("-")[0];
            entryMap.put(StandardField.YEAR, year);
        }
        if (!entryMap.containsKey(StandardField.URL) && citation.values.containsKey("url")) {
            entryMap.put(StandardField.URL, citation.values.get("url"));
        }
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
            case "article", "conference-paper" -> StandardEntryType.Article;
            case "book" -> StandardEntryType.Book;
            case "conference" -> StandardEntryType.InProceedings;
            case "proceedings" -> StandardEntryType.Proceedings;
            case "misc" -> StandardEntryType.Misc;
            case "manual" -> StandardEntryType.Manual;
            case "software" -> StandardEntryType.Software;
            case "report" -> StandardEntryType.TechReport;
            case "unpublished" -> StandardEntryType.Unpublished;
            default -> StandardEntryType.Misc;
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
