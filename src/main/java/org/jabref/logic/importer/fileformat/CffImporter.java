package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CffImporter extends Importer {
    private static final Logger LOGGER = LoggerFactory.getLogger(CffImporter.class);

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

    private static class CffFormat {
        private HashMap<String, String> vals = new HashMap<String, String>();

        @JsonProperty("authors")
        private List<CffAuthor> authors;

        @JsonProperty("identifiers")
        private List<CffIdentifier> ids;

        public CffFormat() {
        }

        @JsonAnySetter
        private void setValues(String key, String value) {
            vals.put(key, value);
        }
    }

    private static class CffAuthor {
        private HashMap<String, String> vals = new HashMap<String, String>();

        public CffAuthor() {
        }

        @JsonAnySetter
        private void setValues(String key, String value) {
            vals.put(key, value);
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
        HashMap<Field, String> entryMap = new HashMap<Field, String>();
        StandardEntryType entryType = StandardEntryType.Software;

        // Map CFF fields to JabRef Fields
        HashMap<String, StandardField> fieldMap = getFieldMappings();
        for (Map.Entry<String, String> property : citation.vals.entrySet()) {
            if (fieldMap.containsKey(property.getKey())) {
                entryMap.put(fieldMap.get(property.getKey()), property.getValue());
            } else if (property.getKey().equals("type")) {
                if (property.getValue().equals("dataset")) {
                    entryType = StandardEntryType.Dataset;
                }
            } else {
                entryMap.put(new UnknownField(property.getKey()), property.getValue());
            }
        }

        // Translate CFF author format to JabRef author format
        String authorStr = IntStream.range(0, citation.authors.size())
                        .mapToObj(citation.authors::get)
                        .map((author) -> author.vals)
                        .map((vals) -> vals.get("name") != null ?
                                new Author(vals.get("name"), "", "", "", "") :
                                new Author(vals.get("given-names"), null, vals.get("name-particle"),
                                        vals.get("family-names"), vals.get("name-suffix")))
                        .collect(AuthorList.collect())
                        .getAsFirstLastNamesWithAnd();
        entryMap.put(StandardField.AUTHOR, authorStr);

        // Select DOI to keep
        if (entryMap.get(StandardField.DOI) == null && citation.ids != null) {
            List<CffIdentifier> doiIds = IntStream.range(0, citation.ids.size())
                            .mapToObj(citation.ids::get)
                            .filter(id -> id.type.equals("doi"))
                            .collect(Collectors.toList());
            if (doiIds.size() == 1) {
                entryMap.put(StandardField.DOI, doiIds.get(0).value);
            }
        }

        // Select SWHID to keep
        if (citation.ids != null) {
            List<String> swhIds = IntStream.range(0, citation.ids.size())
                                           .mapToObj(citation.ids::get)
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

        List<BibEntry> entriesList = new ArrayList<BibEntry>();
        entriesList.add(entry);

        return new ParserResult(entriesList);
    }

    @Override
    public boolean isRecognizedFormat(BufferedReader reader) throws IOException {

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        CffFormat citation;

        try {
            citation = mapper.readValue(reader, CffFormat.class);
        } catch (IOException e) {
            return false;
        }

        if (citation != null && citation.vals.get("title") != null) {
            return true;
        } else {
            return false;
        }
    }

    private HashMap<String, StandardField> getFieldMappings() {
        HashMap<String, StandardField> hm = new HashMap<String, StandardField>();
        hm.put("title", StandardField.TITLE);
        hm.put("version", StandardField.VERSION);
        hm.put("doi", StandardField.DOI);
        hm.put("license", StandardField.LICENSE);
        hm.put("repository", StandardField.REPOSITORY);
        hm.put("url", StandardField.URL);
        hm.put("abstract", StandardField.ABSTRACT);
        hm.put("message", StandardField.COMMENT);
        hm.put("date-released", StandardField.DATE);
        hm.put("keywords", StandardField.KEYWORDS);
        return hm;
    }
}
