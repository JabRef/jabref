package org.jabref.logic.exporter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jabref.logic.util.OS;
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

        try (AtomicFileWriter ps = new AtomicFileWriter(file, StandardCharsets.UTF_8)) {
            ps.write("# YAML 1.2" + OS.NEWLINE);
            ps.write("---" + OS.NEWLINE);
            ps.write("cff-version: 1.2.0" + OS.NEWLINE);

            for (BibEntry entry : entries) {
                // Retrieve all fields
                Map<Field, String> entryMap = entry.getFieldMap();

                // Compulsory message field
                String message = entryMap.getOrDefault(StandardField.COMMENT,
                        "If you use this software, please cite it using the metadata from this file.");
                ps.write("message: " + message + OS.NEWLINE);
                entryMap.remove(StandardField.COMMENT);

                // Compulsory title field
                String title = entryMap.getOrDefault(StandardField.TITLE, "No title specified.");
                ps.write("title: " + "\"" + title + "\"" + OS.NEWLINE);
                entryMap.remove(StandardField.TITLE);

                // Compulsory authors field
                List<Author> authors = AuthorList.parse(entryMap.getOrDefault(StandardField.AUTHOR, ""))
                                                 .getAuthors();
                writeAuthors(ps, authors, false);
                entryMap.remove(StandardField.AUTHOR);

                // Type
                Map<EntryType, String> typeMap = getTypeMappings();
                EntryType entryType = entry.getType();
                boolean pref = false;
                switch (entryType) {
                    case StandardEntryType.Software ->
                            ps.write("type: software");
                    case StandardEntryType.Dataset ->
                            ps.write("type: dataset");
                    default -> {
                        if (typeMap.containsKey(entryType)) {
                            pref = true;
                            ps.write("preferred-citation:" + OS.NEWLINE);
                            ps.write("  type: " + typeMap.get(entryType) + OS.NEWLINE);
                            writeAuthors(ps, authors, true);
                            ps.write("  title: " + "\"" + title + "\"");
                        }
                    }
                }
                ps.write(OS.NEWLINE);

                // Keywords
                String keywords = entryMap.getOrDefault(StandardField.KEYWORDS, null);
                if (keywords != null) {
                    ps.write(pref ? "  " : "");
                    ps.write("keywords:" + OS.NEWLINE);
                    for (String keyword : keywords.split(",\\s*")) {
                        ps.write(pref ? "  " : "");
                        ps.write("  - " + keyword + OS.NEWLINE);
                    }
                }
                entryMap.remove(StandardField.KEYWORDS);

                // Date
                String date = entryMap.getOrDefault(StandardField.DATE, null);
                if (date != null) {
                    writeDate(ps, date, pref);
                }
                entryMap.remove(StandardField.DATE);

                // Fields
                Map<Field, String> fieldMap = getFieldMappings();
                for (Field field : entryMap.keySet()) {
                    ps.write(pref ? "  " : "");
                    if (fieldMap.containsKey(field)) {
                        ps.write(fieldMap.get(field) + ": " + "\"" + entryMap.get(field) + "\"" + OS.NEWLINE);
                    } else if (field instanceof UnknownField) {
                        ps.write(field.getName() + ": " + "\"" + entryMap.get(field) + "\"" + OS.NEWLINE);
                    }
                }
            }
        } catch (IOException ex) {
            throw new SaveException(ex);
        }
    }

    private void writeAuthors(AtomicFileWriter ps, List<Author> authors, boolean pref) throws Exception {
        try {
            ps.write(pref ? "  " : "");
            ps.write("authors:");
            if (authors.isEmpty()) {
                ps.write(pref ? "  " : "");
                ps.write(" No author specified.");
            } else {
                ps.write(OS.NEWLINE);
            }
            for (Author author : authors) {
                boolean hyphen = false;
                if (author.getLast().isPresent()) {
                    ps.write(pref ? "  " : "");
                    ps.write("  - family-names: " + author.getLast().get() + OS.NEWLINE);
                    hyphen = true;
                }
                if (author.getFirst().isPresent()) {
                    ps.write(pref ? "  " : "");
                    ps.write(hyphen ? "    " : "  - ");
                    ps.write("given-names: " + author.getFirst().get() + OS.NEWLINE);
                    hyphen = true;
                }
                if (author.getVon().isPresent()) {
                    ps.write(pref ? "  " : "");
                    ps.write(hyphen ? "    " : "  - ");
                    ps.write("name-particle: " + author.getVon().get() + OS.NEWLINE);
                    hyphen = true;
                }
                if (author.getJr().isPresent()) {
                    ps.write(pref ? "  " : "");
                    ps.write(hyphen ? "    " : "  - ");
                    ps.write("name-suffix: " + author.getJr().get() + OS.NEWLINE);
                }
            }
        } catch (IOException ex) {
            throw new SaveException(ex);
        }
    }

    private void writeDate(AtomicFileWriter ps, String dateField, boolean pref) throws Exception {
        StringBuilder builder = new StringBuilder();
        String formatString = "yyyy-MM-dd";
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formatString);
            LocalDate date = LocalDate.parse(dateField, DateTimeFormatter.ISO_LOCAL_DATE);
            builder.append(pref ? "  " : "").append("date-released: ").append(date.format(formatter));
        } catch (DateTimeParseException e) {
            if (pref) {
                try {
                    formatString = "yyyy-MM";
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formatString);
                    YearMonth yearMonth = YearMonth.parse(dateField, formatter);
                    int month = yearMonth.getMonth().getValue();
                    int year = yearMonth.getYear();
                    builder.append("  month: ").append(month).append(OS.NEWLINE);
                    builder.append("  year: ").append(year).append(OS.NEWLINE);
                } catch (DateTimeParseException f) {
                    try {
                        formatString = "yyyy";
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formatString);
                        int year = Year.parse(dateField, formatter).getValue();
                        builder.append("  year: ").append(year).append(OS.NEWLINE);
                    } catch (DateTimeParseException g) {
                        builder.append("  issue-date: ").append(dateField).append(OS.NEWLINE);
                    }
                }
            }
        }
        try {
            ps.write(builder.toString());
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

