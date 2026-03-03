package org.jabref.logic.exporter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.DateTimeException;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.citationstyle.CSLStyleLoader;
import org.jabref.logic.citationstyle.CitationStyleGenerator;
import org.jabref.logic.citationstyle.CitationStyleOutputFormat;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.io.FileNameUniqueness;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.Date;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;

import org.jspecify.annotations.NullMarked;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/// Exports a JabRef library to the [academicpages](https://academicpages.github.io) Jekyll template format.
/// Each [BibEntry] is written to a separate Markdown file in the given output directory.
/// The file name follows the pattern `YYYY-MM-DD-citationkey.md`.
@NullMarked
public class AcademicPagesExporter extends Exporter {

    private static final String COLLECTION = "publications";
    private static final String PUBLICATION_PATH = "publication";

    /// Orders entries by type priority: book -> article -> incollection -> inproceedings -> others
    private static final Comparator<BibEntry> ENTRY_TYPE_ORDER = Comparator.comparingInt(
            entry -> switch (entry.getType().getName()) {
                case "book" ->
                        0;
                case "article" ->
                        1;
                case "incollection" ->
                        2;
                case "inproceedings" ->
                        3;
                default ->
                        4;
            }
    );

    private final FieldPreferences fieldPreferences;
    private final BibEntryTypesManager entryTypesManager;

    public AcademicPagesExporter(FieldPreferences fieldPreferences, BibEntryTypesManager entryTypesManager) {
        super("academicpages", "Academic Pages", StandardFileType.MARKDOWN);
        this.fieldPreferences = fieldPreferences;
        this.entryTypesManager = entryTypesManager;
    }

    @Override
    public void export(BibDatabaseContext databaseContext,
                       Path outputFile,
                       List<BibEntry> entries) throws SaveException {
        export(databaseContext, outputFile, entries, List.of(), JournalAbbreviationLoader.loadBuiltInRepository());
    }

    @Override
    public void export(BibDatabaseContext databaseContext,
                       Path outputFile,
                       List<BibEntry> entries,
                       List<Path> fileDirForDatabase,
                       JournalAbbreviationRepository abbreviationRepository) throws SaveException {
        if (entries.isEmpty()) {
            return;
        }

        String folderName = outputFile.getFileName().toString().replaceAll("\\.md$", "");
        Path parent = outputFile.getParent() != null ? outputFile.getParent() : outputFile;
        Path outputDir = parent.resolve(folderName);
        Path publicationsDir = outputDir.resolve("_publications");
        Path filesDir = outputDir.resolve("files");
        try {
            Files.createDirectories(publicationsDir);
            Files.createDirectories(filesDir);
        } catch (IOException ex) {
            throw new SaveException(ex);
        }
        List<BibEntry> sorted = entries.stream().sorted(ENTRY_TYPE_ORDER).toList();
        for (BibEntry entry : sorted) {
            String fileName = buildFileName(entry, publicationsDir);
            Path mdFile = publicationsDir.resolve(fileName);
            try {
                Files.writeString(mdFile, buildContent(entry, filesDir, databaseContext, fileDirForDatabase));
            } catch (IOException ex) {
                throw new SaveException(ex);
            }
        }
    }

    private String buildFileName(BibEntry entry, Path targetDir) {
        String key = sanitizeKey(entry.getCitationKey().orElse("unknown"));
        String baseName = buildDate(entry) + "-" + key;
        String fullName = FileUtil.getValidFileName(baseName + ".md");
        return FileNameUniqueness.generateUniqueFileName(targetDir, fullName);
    }

    private String buildDate(BibEntry entry) {
        return entry.getFieldOrAlias(StandardField.DATE)
                    .flatMap(Date::parse)
                    .map(date -> formatDate(date.toTemporalAccessor()))
                    .orElse("0000-01-01");
    }

    private String formatDate(TemporalAccessor temporal) {
        try {
            return DateTimeFormatter.ofPattern("uuuu-MM-dd").format(temporal);
        } catch (DateTimeException e1) {
            try {
                return DateTimeFormatter.ofPattern("uuuu-MM").format(temporal) + "-01";
            } catch (DateTimeException e2) {
                try {
                    return DateTimeFormatter.ofPattern("uuuu").format(temporal) + "-01-01";
                } catch (DateTimeException e3) {
                    return "0000-01-01";
                }
            }
        }
    }

    /// Builds the full Markdown file content: YAML front matter + abstract body.
    /// Format based on the [academicpages template](https://github.com/academicpages/academicpages.github.io/blob/master/_publications/2009-10-01-paper-title-number-1.md).
    private String buildContent(BibEntry entry, Path outputDir, BibDatabaseContext databaseContext, List<Path> fileDirForDatabase) {
        String date = buildDate(entry);
        String key = sanitizeKey(entry.getCitationKey().orElse("unknown"));

        Map<String, Object> yamlMap = new LinkedHashMap<>();
        yamlMap.put("title", entry.getFieldOrAliasLatexFree(StandardField.TITLE).orElse(""));
        yamlMap.put("collection", COLLECTION);
        yamlMap.put("permalink", "/" + PUBLICATION_PATH + "/" + date + "-" + key);
        yamlMap.put("date", date);

        buildVenue(entry).ifPresent(venue -> yamlMap.put("venue", venue));
        copyPdfAndGetUrl(entry, outputDir, key, fileDirForDatabase)
                .ifPresent(paperUrl -> yamlMap.put("paperurl", "/files/" + paperUrl));
        writeBibFile(entry, outputDir, key)
                .ifPresent(bibUrl -> yamlMap.put("bibtexurl", "/files/" + bibUrl));
        yamlMap.put("citation", buildCitation(entry, databaseContext));
        yamlMap.put("category", mapCategory(entry.getType()));

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);
        Yaml yaml = new Yaml(options);

        StringJoiner joiner = new StringJoiner("\n");
        joiner.add("---");
        joiner.add(yaml.dump(yamlMap).stripTrailing());
        joiner.add("---");

        entry.getFieldOrAliasLatexFree(StandardField.ABSTRACT).ifPresent(abstractText -> {
            joiner.add("");
            joiner.add(abstractText);
        });

        return joiner.toString() + "\n";
    }

    private Optional<String> buildVenue(BibEntry entry) {
        return entry.getFieldOrAliasLatexFree(StandardField.JOURNAL)
                    .or(() -> entry.getFieldOrAliasLatexFree(StandardField.BOOKTITLE));
    }

    /// Copies the first attached PDF next to the output markdown file and returns its filename
    /// Returns empty if no PDF attachment is found or the file cannot be accessed
    private Optional<String> copyPdfAndGetUrl(BibEntry entry, Path outputDir, String citationKey, List<Path> fileDirForDatabase) {
        Optional<LinkedFile> pdfFile = entry.getFiles().stream()
                                            .filter(f -> "pdf".equalsIgnoreCase(f.getFileType()))
                                            .findFirst();
        if (pdfFile.isEmpty()) {
            return Optional.empty();
        }

        Optional<Path> source = pdfFile.get().findIn(fileDirForDatabase);
        if (source.isEmpty()) {
            return Optional.empty();
        }

        String destFileName = citationKey + ".pdf";
        Path target = outputDir.resolve(destFileName).normalize();
        if (!target.startsWith(outputDir)) {
            return Optional.empty();
        }
        try {
            Files.copy(source.get(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            return Optional.empty();
        }
        return Optional.of(destFileName);
    }

    /// Writes a .bib file for the entry next to the markdown file
    /// Returns the filename so it can be linked via bibtexurl
    private Optional<String> writeBibFile(BibEntry entry, Path outputDir, String citationKey) {
        String bibContent = entry.getStringRepresentation(entry, BibDatabaseMode.BIBTEX, entryTypesManager, fieldPreferences);
        if (bibContent.isEmpty()) {
            return Optional.empty();
        }
        String bibFileName = citationKey + ".bib";
        Path target = outputDir.resolve(bibFileName).normalize();
        if (!target.startsWith(outputDir)) {
            return Optional.empty();
        }
        try {
            Files.writeString(target, bibContent);
            return Optional.of(bibFileName);
        } catch (IOException ex) {
            return Optional.empty();
        }
    }

    /// Generates a citation string from the entry using citation style IEEE
    private String buildCitation(BibEntry entry, BibDatabaseContext databaseContext) {
        BibDatabaseContext context = new BibDatabaseContext(new BibDatabase(List.of(entry)));
        context.setMode(databaseContext.getMode());
        String style = CSLStyleLoader.getDefaultStyle().getSource();
        return CitationStyleGenerator.generateBibliography(
                                             List.of(entry), style, CitationStyleOutputFormat.TEXT, context, entryTypesManager)
                                     .getFirst()
                                     .trim();
    }

    private String mapCategory(EntryType type) {
        if (type == StandardEntryType.InProceedings) {
            return "conferences";
        } else {
            return "manuscripts";
        }
    }

    /// Sanitizes a citation key for safe use as a filename
    private String sanitizeKey(String key) {
        return FileUtil.getValidFileName(key);
    }
}
