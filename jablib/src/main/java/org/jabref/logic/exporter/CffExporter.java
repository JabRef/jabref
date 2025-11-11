package org.jabref.logic.exporter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Date;
import org.jabref.model.entry.ParsedEntryLink;
import org.jabref.model.entry.field.BiblatexSoftwareField;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;

import org.jspecify.annotations.NonNull;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

public class CffExporter extends Exporter {
    // Fields that are taken 1:1 from BibTeX to CFF
    public static final List<String> UNMAPPED_FIELDS = List.of(
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
    public void export(@NonNull BibDatabaseContext databaseContext,
                       @NonNull Path file,
                       @NonNull List<BibEntry> entries) throws SaveException {
        // Do not export if no entries to export -- avoids exports with only template text
        if (entries.isEmpty()) {
            return;
        }

        // Make a copy of the list to avoid modifying the original list
        final List<BibEntry> entriesToTransform = new ArrayList<>(entries);

        // Set up YAML options
        DumperOptions options = new DumperOptions();
        options.setWidth(Integer.MAX_VALUE);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndentWithIndicator(true);
        options.setIndicatorIndent(2);
        Yaml yaml = new Yaml(options);

        BibEntry main = null;
        boolean mainIsDummy = false;
        int countOfSoftwareAndDataSetEntries = 0;
        for (BibEntry entry : entriesToTransform) {
            if (entry.getType() == StandardEntryType.Software || entry.getType() == StandardEntryType.Dataset) {
                main = entry;
                countOfSoftwareAndDataSetEntries++;
            }
        }
        if (countOfSoftwareAndDataSetEntries == 1) {
            // If there is only one software or dataset entry, use it as the main entry
            entriesToTransform.remove(main);
        } else {
            // If there are no software or dataset entries, create a dummy main entry holding the given entries
            main = new BibEntry(StandardEntryType.Software);
            mainIsDummy = true;
        }

        // Transform main entry to CFF format
        Map<String, Object> cffData = transformEntry(main, true, mainIsDummy);

        // Preferred citation
        if (main.hasField(StandardField.CITES)) {
            String citeKey = main.getField(StandardField.CITES).orElse("").split(",")[0];
            List<BibEntry> citedEntries = databaseContext.getDatabase().getEntriesByCitationKey(citeKey);
            entriesToTransform.removeAll(citedEntries);
            if (!citedEntries.isEmpty()) {
                BibEntry citedEntry = citedEntries.getFirst();
                cffData.put("preferred-citation", transformEntry(citedEntry, false, false));
            }
        }

        // References
        List<Map<String, Object>> related = new ArrayList<>();
        if (main.hasField(StandardField.RELATED)) {
            main.getEntryLinkList(StandardField.RELATED, databaseContext.getDatabase())
                .stream()
                .map(ParsedEntryLink::getLinkedEntry)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(entry -> {
                    related.add(transformEntry(entry, false, false));
                    entriesToTransform.remove(entry);
                });
        }

        // Add remaining entries as references
        for (BibEntry entry : entriesToTransform) {
            related.add(transformEntry(entry, false, false));
        }
        if (!related.isEmpty()) {
            cffData.put("references", related);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            yaml.dump(cffData, writer);
        } catch (IOException ex) {
            throw new SaveException(ex);
        }
    }

    private Map<String, Object> transformEntry(BibEntry entry, boolean main, boolean dummy) {
        Map<String, Object> cffData = new LinkedHashMap<>();
        Map<Field, String> fields = new HashMap<>(entry.getFieldMap());

        if (main) {
            // Mandatory CFF version field
            cffData.put("cff-version", "1.2.0");

            // Mandatory message field
            String message = fields.getOrDefault(StandardField.COMMENT,
                    "If you use this software, please cite it using the metadata from this file.");
            cffData.put("message", message);
            fields.remove(StandardField.COMMENT);
        }

        // Mandatory title field
        String title = fields.getOrDefault(StandardField.TITLE, "No title specified.");
        cffData.put("title", title);
        fields.remove(StandardField.TITLE);

        // Mandatory authors field
        List<Author> authors = AuthorList.parse(fields.getOrDefault(StandardField.AUTHOR, ""))
                                         .getAuthors();
        parseAuthors(cffData, authors);
        fields.remove(StandardField.AUTHOR);

        // Type
        if (!dummy) {
            cffData.put("type", TYPES_MAP.getOrDefault(entry.getType(), "misc"));
        }

        // Keywords
        String keywords = fields.getOrDefault(StandardField.KEYWORDS, null);
        if (keywords != null) {
            cffData.put("keywords", keywords.split(",\\s*"));
        }
        fields.remove(StandardField.KEYWORDS);

        // Date
        String date = fields.getOrDefault(StandardField.DATE, null);
        if (date != null) {
            parseDate(cffData, date);
        }
        fields.remove(StandardField.DATE);

        // Remaining fields not handled above
        for (Field field : fields.keySet()) {
            if (FIELDS_MAP.containsKey(field)) {
                cffData.put(FIELDS_MAP.get(field), fields.get(field));
            } else if (field instanceof UnknownField) {
                // Check that field is accepted by CFF format specification
                if (UNMAPPED_FIELDS.contains(field.getName())) {
                    cffData.put(field.getName(), fields.get(field));
                }
            }
        }
        return cffData;
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
        Optional<Date> parsedDateOpt = Date.parse(date);
        if (parsedDateOpt.isEmpty()) {
            data.put("issue-date", date);
            return;
        }
        Date parsedDate = parsedDateOpt.get();
        if (parsedDate.getYear().isPresent() && parsedDate.getMonth().isPresent() && parsedDate.getDay().isPresent()) {
            data.put("date-released", parsedDate.getNormalized());
            return;
        }
        parsedDate.getMonth().ifPresent(month -> data.put("month", month.getNumber()));
        parsedDate.getYear().ifPresent(year -> data.put("year", year));
    }
}

