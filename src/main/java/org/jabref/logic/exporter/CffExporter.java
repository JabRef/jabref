package org.jabref.logic.exporter;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

/**
 * Exporter for exporting in CFF format.
 */
class CffExporter extends Exporter {
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

        try (FileWriter writer = new FileWriter(file.toFile(), StandardCharsets.UTF_8)) {
            DumperOptions options = new DumperOptions();

            // Set line width to infinity to avoid line wrapping
            options.setWidth(Integer.MAX_VALUE);

            // Set collections to be written in block rather than inline
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setPrettyFlow(true);

            // Set indent for sequences to two spaces
            options.setIndentWithIndicator(true);
            options.setIndicatorIndent(2);
            Yaml yaml = new Yaml(options);

            Map<String, Object> originalData = new LinkedHashMap<>();
            Map<String, Object> preferredData = new LinkedHashMap<>();
            Map<String, Object> data = originalData;
            data.put("cff-version", "1.2.0");

            for (BibEntry entry : entries) {
                // Retrieve all fields
                Map<Field, String> entryMap = entry.getFieldMap();

                // Compulsory message field
                String message = entryMap.getOrDefault(StandardField.COMMENT,
                        "If you use this software, please cite it using the metadata from this file.");
                data.put("message", message);
                entryMap.remove(StandardField.COMMENT);

                // Compulsory title field
                String title = entryMap.getOrDefault(StandardField.TITLE, "No title specified.");
                data.put("title", title);
                entryMap.remove(StandardField.TITLE);

                // Compulsory authors field
                List<Author> authors = AuthorList.parse(entryMap.getOrDefault(StandardField.AUTHOR, ""))
                                                 .getAuthors();

                // Create two copies of the same list to avoid using YAML anchors and aliases
                List<Map<String, String>> authorsList = new ArrayList<>();
                List<Map<String, String>> authorsListPreferred = new ArrayList<>();
                authors.forEach(author -> {
                    Map<String, String> authorMap = new LinkedHashMap<>();
                    Map<String, String> authorMapPreferred = new LinkedHashMap<>();
                    if (author.getFamilyName().isPresent()) {
                        authorMap.put("family-names", author.getFamilyName().get());
                        authorMapPreferred.put("family-names", author.getFamilyName().get());
                    }
                    if (author.getGivenName().isPresent()) {
                        authorMap.put("given-names", author.getGivenName().get());
                        authorMapPreferred.put("given-names", author.getGivenName().get());
                    }
                    if (author.getNamePrefix().isPresent()) {
                        authorMap.put("name-particle", author.getNamePrefix().get());
                        authorMapPreferred.put("name-particle", author.getNamePrefix().get());
                    }
                    if (author.getNameSuffix().isPresent()) {
                        authorMap.put("name-suffix", author.getNameSuffix().get());
                        authorMapPreferred.put("name-suffix", author.getNameSuffix().get());
                    }
                    authorsList.add(authorMap);
                    authorsListPreferred.add(authorMapPreferred);
                });
                data.put("authors", authorsList.isEmpty() ? "No author specified." : authorsList);
                entryMap.remove(StandardField.AUTHOR);

                // Type
                Map<EntryType, String> typeMap = getTypeMappings();
                EntryType entryType = entry.getType();
                switch (entryType) {
                    case StandardEntryType.Software, StandardEntryType.Dataset ->
                            data.put("type", entryType.getName());
                    default -> {
                        if (typeMap.containsKey(entryType)) {
                            data.put("preferred-citation", preferredData);
                            data = preferredData;
                            data.put("type", typeMap.get(entryType));
                            data.put("authors", authorsListPreferred.isEmpty() ? "No author specified." : authorsListPreferred);
                            data.put("title", title);
                        }
                    }
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
                    String formatString;
                    try {
                        LocalDate localDate = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
                        data.put("date-released", localDate.toString());
                    } catch (DateTimeParseException e) {
                        try {
                            formatString = "yyyy-MM";
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formatString);
                            YearMonth yearMonth = YearMonth.parse(date, formatter);
                            int month = yearMonth.getMonth().getValue();
                            int year = yearMonth.getYear();
                            data.put("month", month);
                            data.put("year", year);
                        } catch (DateTimeParseException f) {
                            try {
                                formatString = "yyyy";
                                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formatString);
                                int year = Year.parse(date, formatter).getValue();
                                data.put("year", year);
                            } catch (DateTimeParseException g) {
                                data.put("issue-date", date);
                            }
                        }
                    }
                }
                entryMap.remove(StandardField.DATE);

                // Fields
                Map<Field, String> fieldMap = getFieldMappings();
                for (Field field : entryMap.keySet()) {
                    if (fieldMap.containsKey(field)) {
                        data.put(fieldMap.get(field), entryMap.get(field));
                    } else if (field instanceof UnknownField) {
                        data.put(field.getName(), entryMap.get(field));
                    }
                }
            }

            yaml.dump(originalData, writer);
        } catch (IOException ex) {
            throw new SaveException(ex);
        }
    }

    private Map<EntryType, String> getTypeMappings() {
        Map<EntryType, String> typeMappings = new HashMap<>();
        typeMappings.put(StandardEntryType.Article, "article");
        typeMappings.put(StandardEntryType.Conference, "article");
        typeMappings.put(StandardEntryType.Book, "book");
        typeMappings.put(StandardEntryType.Booklet, "pamphlet");
        typeMappings.put(StandardEntryType.InProceedings, "conference-paper");
        typeMappings.put(StandardEntryType.Proceedings, "proceedings");
        typeMappings.put(StandardEntryType.Misc, "misc");
        typeMappings.put(StandardEntryType.Manual, "manual");
        typeMappings.put(StandardEntryType.Report, "report");
        typeMappings.put(StandardEntryType.TechReport, "report");
        typeMappings.put(StandardEntryType.Unpublished, "unpublished");
        return typeMappings;
    }

    private Map<Field, String> getFieldMappings() {
        Map<Field, String> fieldMappings = new HashMap<>();
        fieldMappings.put(StandardField.TITLE, "title");
        fieldMappings.put(StandardField.VERSION, "version");
        fieldMappings.put(StandardField.DOI, "doi");
        fieldMappings.put(BiblatexSoftwareField.LICENSE, "license");
        fieldMappings.put(BiblatexSoftwareField.REPOSITORY, "repository");
        fieldMappings.put(StandardField.URL, "url");
        fieldMappings.put(StandardField.ABSTRACT, "abstract");
        fieldMappings.put(StandardField.COMMENT, "message");
        fieldMappings.put(StandardField.DATE, "date-released");
        fieldMappings.put(StandardField.KEYWORDS, "keywords");
        fieldMappings.put(StandardField.MONTH, "month");
        fieldMappings.put(StandardField.YEAR, "year");
        fieldMappings.put(StandardField.JOURNAL, "journal");
        fieldMappings.put(StandardField.ISSUE, "issue");
        fieldMappings.put(StandardField.VOLUME, "volume");
        fieldMappings.put(StandardField.NUMBER, "number");
        return fieldMappings;
    }
}

