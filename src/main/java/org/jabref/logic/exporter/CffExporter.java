package org.jabref.logic.exporter;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jabref.logic.layout.format.DateFormatter;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.BiblatexSoftwareField;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

public class CffExporter extends Exporter {
    public static final List<String> UNMAPPED_FIELDS = Arrays.asList(
            "abbreviation", "collection-doi", "collection-title", "collection-type", "commit", "copyright",
            "data-type", "database", "date-accessed", "date-downloaded", "date-published", "department", "end",
            "entry", "filename", "format", "issue-date", "issue-title", "license-url", "loc-end", "loc-start",
            "medium", "nihmsid", "number-volumes", "patent-states", "pmcid", "repository-artifact", "repository-code",
            "scope", "section", "start", "term", "thesis-type", "volume-title", "year-original"
    );
    public static final Map<Field, String> FIELDS_MAP = Map.ofEntries(
            Map.entry(StandardField.ABSTRACT, "abstract"),
            Map.entry(StandardField.DATE, "date-released"),
            Map.entry(StandardField.DOI, "doi"),
            Map.entry(StandardField.KEYWORDS, "keywords"),
            Map.entry(BiblatexSoftwareField.LICENSE, "license"),
            Map.entry(StandardField.COMMENT, "message"),
            Map.entry(BiblatexSoftwareField.REPOSITORY, "repository"),
            Map.entry(StandardField.TITLE, "title"),
            Map.entry(StandardField.URL, "url"),
            Map.entry(StandardField.VERSION, "version"),
            Map.entry(StandardField.EDITION, "edition"),
            Map.entry(StandardField.ISBN, "isbn"),
            Map.entry(StandardField.ISSN, "issn"),
            Map.entry(StandardField.ISSUE, "issue"),
            Map.entry(StandardField.JOURNAL, "journal"),
            Map.entry(StandardField.MONTH, "month"),
            Map.entry(StandardField.NOTE, "notes"),
            Map.entry(StandardField.NUMBER, "number"),
            Map.entry(StandardField.PAGES, "pages"),
            Map.entry(StandardField.PUBSTATE, "status"),
            Map.entry(StandardField.VOLUME, "volume"),
            Map.entry(StandardField.YEAR, "year")
    );

    public static final Map<EntryType, String> TYPES_MAP = Map.ofEntries(
        Map.entry(StandardEntryType.Article, "article"),
        Map.entry(StandardEntryType.Book, "book"),
        Map.entry(StandardEntryType.Booklet, "pamphlet"),
        Map.entry(StandardEntryType.Proceedings, "conference"),
        Map.entry(StandardEntryType.InProceedings, "conference-paper"),
        Map.entry(StandardEntryType.Misc, "misc"),
        Map.entry(StandardEntryType.Manual, "manual"),
        Map.entry(StandardEntryType.Software, "software"),
        Map.entry(StandardEntryType.Dataset, "dataset"),
        Map.entry(StandardEntryType.Report, "report"),
        Map.entry(StandardEntryType.Unpublished, "unpublished")
    );

    public CffExporter() {
        super("cff", "CFF", StandardFileType.CFF);
    }

    @Override
    public void export(BibDatabaseContext databaseContext, Path file, List<BibEntry> entries) throws Exception {
        Objects.requireNonNull(databaseContext);
        Objects.requireNonNull(file);
        Objects.requireNonNull(entries);

        if (entries.isEmpty()) { // Do not export if no entries to export -- avoids exports with only template text
            return;
        }

        // Make a copy of the list to avoid modifying the original list
        entries = new ArrayList<>(entries);

        // Set up YAML options
        DumperOptions options = new DumperOptions();
        options.setWidth(Integer.MAX_VALUE);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndentWithIndicator(true);
        options.setIndicatorIndent(2);
        Yaml yaml = new Yaml(options);

        // Check number of `software` or `dataset` entries
        int counter = 0;
        boolean dummy = false;
        BibEntry main = null;
        for (BibEntry entry : entries) {
            if (entry.getType() == StandardEntryType.Software || entry.getType() == StandardEntryType.Dataset) {
                main = entry;
                counter++;
            }
        }
        if (counter == 1) {
            entries.remove(main);
        } else {
            main = new BibEntry(StandardEntryType.Software);
            dummy = true;
        }

        // Main entry
        Map<String, Object> data = parseEntry(main, true, dummy);

        // Preferred citation
        if (main.hasField(StandardField.CITES)) {
            String citeKey = main.getField(StandardField.CITES).orElse("").split(",")[0];
            List<BibEntry> citedEntries = databaseContext.getDatabase().getEntriesByCitationKey(citeKey);
            entries.removeAll(citedEntries);
            if (!citedEntries.isEmpty()) {
                BibEntry citedEntry = citedEntries.getFirst();
                data.put("preferred-citation", parseEntry(citedEntry, false, false));
            }
        }

        // References
        List<Map<String, Object>> related = new ArrayList<>();
        if (main.hasField(StandardField.RELATED)) {
            String[] citeKeys = main.getField(StandardField.RELATED).orElse("").split(",");
            List<BibEntry> relatedEntries = new ArrayList<>();
            Arrays.stream(citeKeys).forEach(citeKey ->
                    relatedEntries.addAll(databaseContext.getDatabase().getEntriesByCitationKey(citeKey)));
            entries.removeAll(relatedEntries);
            if (!relatedEntries.isEmpty()) {
                relatedEntries.forEach(entry -> related.add(parseEntry(entry, false, false)));
            }
        }

        // Add remaining entries as references
        for (BibEntry entry : entries) {
            related.add(parseEntry(entry, false, false));
        }
        if (!related.isEmpty()) {
            data.put("references", related);
        }

        // Write to file
        try (FileWriter writer = new FileWriter(file.toFile(), StandardCharsets.UTF_8)) {
            yaml.dump(data, writer);
        } catch (
                IOException ex) {
            throw new SaveException(ex);
        }
    }

    private Map<String, Object> parseEntry(BibEntry entry, boolean main, boolean dummy) {
        Map<String, Object> data = new LinkedHashMap<>();
        Map<Field, String> entryMap = new HashMap<>(entry.getFieldMap());

        if (main) {
            // Mandatory CFF version field
            data.put("cff-version", "1.2.0");

            // Mandatory message field
            String message = entryMap.getOrDefault(StandardField.COMMENT,
                    "If you use this software, please cite it using the metadata from this file.");
            data.put("message", message);
            entryMap.remove(StandardField.COMMENT);
        }

        // Mandatory title field
        String title = entryMap.getOrDefault(StandardField.TITLE, "No title specified.");
        data.put("title", title);
        entryMap.remove(StandardField.TITLE);

        // Mandatory authors field
        List<Author> authors = AuthorList.parse(entryMap.getOrDefault(StandardField.AUTHOR, ""))
                                         .getAuthors();
        parseAuthors(data, authors);
        entryMap.remove(StandardField.AUTHOR);

        // Type
        if (!dummy) {
            data.put("type", TYPES_MAP.getOrDefault(entry.getType(), "misc"));
        }

        // Keywords
        String keywords = entryMap.getOrDefault(StandardField.KEYWORDS, null);
        if (keywords != null) {
            data.put("keywords", keywords.split(",\\s*"));
        }
        entryMap.remove(StandardField.KEYWORDS);

        // Date
        String date = entryMap.getOrDefault(StandardField.DATE, null);
        if (date != null) {
            parseDate(data, date);
        }
        entryMap.remove(StandardField.DATE);

        // Fields
        for (Field field : entryMap.keySet()) {
            if (FIELDS_MAP.containsKey(field)) {
                data.put(FIELDS_MAP.get(field), entryMap.get(field));
            } else if (field instanceof UnknownField) {
                // Check that field is accepted by CFF format specification
                if (UNMAPPED_FIELDS.contains(field.getName())) {
                    data.put(field.getName(), entryMap.get(field));
                }
            }
        }
        return data;
    }

    private void parseAuthors(Map<String, Object> data, List<Author> authors) {
        List<Map<String, String>> authorsList = new ArrayList<>();
        authors.forEach(author -> {
            Map<String, String> authorMap = new LinkedHashMap<>();
            if (author.getFamilyName().isPresent()) {
                authorMap.put("family-names", author.getFamilyName().get());
            }
            if (author.getGivenName().isPresent()) {
                authorMap.put("given-names", author.getGivenName().get());
            }
            if (author.getNamePrefix().isPresent()) {
                authorMap.put("name-particle", author.getNamePrefix().get());
            }
            if (author.getNameSuffix().isPresent()) {
                authorMap.put("name-suffix", author.getNameSuffix().get());
            }
            authorsList.add(authorMap);
        });
        data.put("authors", authorsList.isEmpty() ? List.of(Map.of("name", "/")) : authorsList);
    }

    private void parseDate(Map<String, Object> data, String date) {
        String formatString;
        try {
            DateFormatter dateFormatter = new DateFormatter();
            data.put("date-released", dateFormatter.format(date));
        } catch (
                DateTimeParseException e) {
            try {
                formatString = "yyyy-MM";
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formatString);
                YearMonth yearMonth = YearMonth.parse(date, formatter);
                int month = yearMonth.getMonth().getValue();
                int year = yearMonth.getYear();
                data.put("month", month);
                data.put("year", year);
            } catch (
                    DateTimeParseException f) {
                try {
                    formatString = "yyyy";
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formatString);
                    int year = Year.parse(date, formatter).getValue();
                    data.put("year", year);
                } catch (
                        DateTimeParseException g) {
                    data.put("issue-date", date);
                }
            }
        }
    }
}

