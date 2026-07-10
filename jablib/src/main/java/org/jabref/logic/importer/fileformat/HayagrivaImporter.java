package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.type.TypeReference;
import tools.jackson.dataformat.yaml.YAMLMapper;

@NullMarked
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

    private static final int MAX_LOOKAHEAD_CHARS = 65_536;
    private static final int MAX_LOOKAHEAD_LINES = 1_000;
    private static final Pattern TYPE_LINE_PATTERN = Pattern.compile("^\\s*type:\\s*\"?'?([A-Za-z-]+)\"?'?\\s*$");

    @Override
    public ParserResult importDatabase(BufferedReader input) throws IOException {
        YAMLMapper mapper = new YAMLMapper();

        Map<String, HayagrivaEntry> hayagrivaEntries = mapper.readValue(
                input,
                new TypeReference<Map<String, HayagrivaEntry>>() {
                }
        );

        List<BibEntry> bibEntries = new ArrayList<>();

        if (hayagrivaEntries == null) {
            return new ParserResult(bibEntries);
        }

        for (Map.Entry<String, HayagrivaEntry> entryMap : hayagrivaEntries.entrySet()) {
            String citationKey = entryMap.getKey();
            HayagrivaEntry hData = entryMap.getValue();

            if (hData == null) {
                continue;
            }

            String type = hData.type == null ? "" : hData.type;
            EntryType entryType = TYPES_MAP.getOrDefault(type, StandardEntryType.Misc);
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

    private String formatAuthorName(String hayagrivaAuthor) {
        String[] parts = hayagrivaAuthor.split(",", 2);
        if (parts.length == 2) {
            String lastname = parts[0].trim();
            String name = parts[1].trim();
            return name + " " + lastname;
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
        @Nullable
        public String doi;

        @Nullable
        public String isbn;

        @Nullable
        public String issn;
    }

    public static class HayagrivaParent {
        @Nullable
        public String type;

        @Nullable
        public String title;
    }

    public static class HayagrivaEntry {
        @JsonProperty("type")
        @Nullable
        public String type;

        @JsonProperty("title")
        @Nullable
        public String title;

        @JsonProperty("url")
        @Nullable
        public String url;

        @JsonProperty("author")
        @Nullable
        public List<String> authors;

        @JsonProperty("date")
        @Nullable
        public String date;

        @JsonProperty("parent")
        @Nullable
        public HayagrivaParent parent;

        @JsonProperty("serial-number")
        @Nullable
        public HayagrivaSerialNumber serialNumber;
    }

    /// Lightweight recognition for the {@link BufferedReader} overload, which must reset the
    /// reader afterwards. Instead of doing a full YAML parse (which could read past the marked
    /// read-ahead limit and make {@code reset()} throw), this scans at most
    /// {@link #MAX_LOOKAHEAD_LINES} lines / {@link #MAX_LOOKAHEAD_CHARS} characters for a
    /// top-level {@code type:} key matching a known Hayagriva entry type.
    @Override
    public boolean isRecognizedFormat(BufferedReader input) throws IOException {
        input.mark(MAX_LOOKAHEAD_CHARS);
        try {
            int charsConsumed = 0;
            int linesRead = 0;
            String line;

            while (linesRead < MAX_LOOKAHEAD_LINES && (line = input.readLine()) != null) {
                linesRead++;
                charsConsumed += line.length() + 1;
                if (charsConsumed >= MAX_LOOKAHEAD_CHARS) {
                    break;
                }

                Matcher matcher = TYPE_LINE_PATTERN.matcher(line);
                if (matcher.matches() && TYPES_MAP.containsKey(matcher.group(1).toLowerCase(Locale.ROOT))) {
                    return true;
                }
            }

            return false;
        } finally {
            input.reset();
        }
    }

    /// Full recognition for the {@link Reader} overload, which has no reset requirement, so a
    /// complete (but loosely-typed) YAML parse is safe here.
    @Override
    public boolean isRecognizedFormat(Reader input) throws IOException {
        BufferedReader bufferedReader = input instanceof BufferedReader br ? br : new BufferedReader(input);

        YAMLMapper mapper = YAMLMapper.builder()
                                      .disable(StreamReadFeature.AUTO_CLOSE_SOURCE)
                                      .build();

        Map<String, Object> raw;
        try {
            raw = mapper.readValue(bufferedReader, new TypeReference<Map<String, Object>>() {
            });
        } catch (JacksonException e) {
            return false;
        }

        if (raw == null || raw.isEmpty()) {
            return false;
        }

        for (Object value : raw.values()) {
            if (value instanceof Map<?, ?> entry
                    && entry.get("type") instanceof String type
                    && TYPES_MAP.containsKey(type.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }

        return false;
    }
}
