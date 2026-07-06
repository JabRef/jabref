package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.FileType;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;

import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.core.type.TypeReference;
import tools.jackson.dataformat.yaml.YAMLMapper;

public class HayagrivaImporter extends Importer {

    private static final Map<String, EntryType> TYPES_MAP = Map.of(
            "article", StandardEntryType.Article,
            "book", StandardEntryType.Book,
            "chapter", StandardEntryType.InBook,
            "conference", StandardEntryType.InProceedings,
            "thesis", StandardEntryType.PhdThesis,
            "report", StandardEntryType.TechReport,
            "web", StandardEntryType.Online
    );

    @Override
    public ParserResult importDatabase(BufferedReader input) throws IOException {
        YAMLMapper mapper = new YAMLMapper();

        Map<String, HayagrivaEntry> hayagrivaEntries = mapper.readValue(
                input,
                new TypeReference<Map<String, HayagrivaEntry>>() { }
        );

        List<BibEntry> bibEntries = new ArrayList<>();

        if (hayagrivaEntries == null) {
            return new ParserResult(bibEntries);
        }

        for (Map.Entry<String, HayagrivaEntry> entryMap : hayagrivaEntries.entrySet()) {
            String citationKey = entryMap.getKey();
            HayagrivaEntry hData = entryMap.getValue();

            EntryType entryType = TYPES_MAP.getOrDefault(hData.type, StandardEntryType.Misc);
            BibEntry bibEntry = new BibEntry(entryType);
            bibEntry.setCitationKey(citationKey);

            if (hData.title != null) {
                bibEntry.setField(StandardField.TITLE, hData.title);
            }

            if (hData.url != null) {
                bibEntry.setField(StandardField.URL, hData.url);
            }

            if (hData.date != null) {
                bibEntry.setField(StandardField.DATE, hData.date);
            }

            if (hData.authors != null && !hData.authors.isEmpty()) {
                String formattedAuthors = hData.authors.stream()
                                                       .map(this::formatAuthorName)
                                                       .collect(Collectors.joining(" and "));
                bibEntry.setField(StandardField.AUTHOR, formattedAuthors);
            }

            if (hData.parent != null && hData.parent.title != null) {
                bibEntry.setField(StandardField.JOURNAL, hData.parent.title);
            }

            if (hData.serialNumber != null) {
                if (hData.serialNumber.doi != null) {
                    bibEntry.setField(StandardField.DOI, hData.serialNumber.doi);
                }
                if (hData.serialNumber.isbn != null) {
                    bibEntry.setField(StandardField.ISBN, hData.serialNumber.isbn);
                }
                if (hData.serialNumber.issn != null) {
                    bibEntry.setField(StandardField.ISSN, hData.serialNumber.issn);
                }
            }

            bibEntries.add(bibEntry);
        }

        return new ParserResult(bibEntries);
    }

    /// Converte "Sobrenome, Nome" (formato Hayagriva) para "Nome Sobrenome" (formato JabRef)
    private String formatAuthorName(String hayagrivaAuthor) {
        String[] partes = hayagrivaAuthor.split(",", 2);
        if (partes.length == 2) {
            String sobrenome = partes[0].trim();
            String nome = partes[1].trim();
            return nome + " " + sobrenome;
        }
        return hayagrivaAuthor.trim();
    }

    @Override
    public String getId() {
        return "hayagrivayaml";
    }

    @Override
    public String getName() {
        return "Hayagriva YAML";
    }

    @Override
    public String getDescription() {
        return Localization.lang("Importer for the Hayagriva YAML format, used by Typst, for bibliographic entries.");
    }

    @Override
    public FileType getFileType() {
        return StandardFileType.YAML;
    }

    public static class HayagrivaSerialNumber {
        public String doi;
        public String isbn;
        public String issn;
    }

    public static class HayagrivaParent {
        public String type;
        public String title;
    }

    public static class HayagrivaEntry {
        @JsonProperty("type")
        public String type;
        @JsonProperty("title")
        public String title;
        @JsonProperty("url")
        public String url;
        @JsonProperty("author")
        public List<String> authors;
        @JsonProperty("date")
        public String date;
        @JsonProperty("parent")
        public HayagrivaParent parent;
        @JsonProperty("serial-number")
        public HayagrivaSerialNumber serialNumber;
    }

    @Override
    public boolean isRecognizedFormat(BufferedReader input) throws IOException {
        input.mark(Integer.MAX_VALUE);
        try {
            YAMLMapper mapper = new YAMLMapper();
            Map<String, HayagrivaEntry> entries = mapper.readValue(
                    input,
                    new TypeReference<Map<String, HayagrivaEntry>>() { }
            );

            if (entries == null) {
                return false;
            }

            for (HayagrivaEntry entry : entries.values()) {
                if (entry.type != null && (entry.title != null || entry.authors != null)) {
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            return false;
        } finally {
            input.reset();
        }
    }
}
