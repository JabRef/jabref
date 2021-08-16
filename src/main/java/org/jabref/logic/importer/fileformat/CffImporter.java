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
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
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

    @Override
    public ParserResult importDatabase(BufferedReader reader) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        CffFormat citation = mapper.readValue(reader, CffFormat.class);
        HashMap<Field, String> entryMap = new HashMap<Field, String>();

        HashMap<String, StandardField> fieldMap = getFieldMappings();
        for (Map.Entry<String, String> property : citation.vals.entrySet()) {
            if (fieldMap.containsKey(property.getKey())) {
                entryMap.put(fieldMap.get(property.getKey()), property.getValue());
            }
        }

        List<String> authors = new ArrayList<String>();
        for (CffAuthor auth: citation.authors) {
            String aName = auth.vals.get("name");
            String aGivenNames = auth.vals.get("given-names");
            String aFamilyNames = auth.vals.get("family-names");
            String aNameParticle = auth.vals.get("name-particle");
            String aAlias = auth.vals.get("alias");

            if (aName != null) {
                authors.add(aName);
            } else if (aFamilyNames != null && aNameParticle != null && aGivenNames != null) {
                authors.add(aGivenNames + " " + aNameParticle + " " + aFamilyNames);
            } else if (aFamilyNames != null && aGivenNames != null) {
                authors.add(aGivenNames + " " + aFamilyNames);
            } else if (aFamilyNames != null) {
                authors.add(aFamilyNames);
            } else if (aAlias != null) {
                authors.add(aAlias);
            }
        }

        String authorStr = String.join(", ", authors);
        entryMap.put(StandardField.AUTHOR, authorStr);

        BibEntry entry = new BibEntry(StandardEntryType.Software);
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

        return hm;
    }
}
