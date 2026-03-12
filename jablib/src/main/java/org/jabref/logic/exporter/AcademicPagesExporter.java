package org.jabref.logic.exporter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.DateTimeException;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.citationstyle.CSLStyleLoader;
import org.jabref.logic.citationstyle.CitationStyleGenerator;
import org.jabref.logic.citationstyle.CitationStyleOutputFormat;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.layout.Layout;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.layout.LayoutHelper;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.io.FileNameUniqueness;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.Date;
import org.jabref.model.entry.field.StandardField;

import org.jspecify.annotations.NullMarked;

/// Exports JabRef entries to the [academicpages](https://academicpages.github.io) Jekyll template format.
/// Each {@link BibEntry} is written as a separate Markdown file under {@code _publications/}.
/// YAML front matter is rendered via JabRef's layout engine; file management and computed fields are handled in Java.
@NullMarked
public class AcademicPagesExporter extends Exporter {

    private static final String LAYOUT_PREFIX = "/resource/layout/academicpages/academicpages";
    private static final String LAYOUT_EXTENSION = ".layout";
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
            });

    private final LayoutFormatterPreferences layoutPreferences;
    private final FieldPreferences fieldPreferences;
    private final BibEntryTypesManager entryTypesManager;

    public AcademicPagesExporter(LayoutFormatterPreferences layoutPreferences,
                                 FieldPreferences fieldPreferences,
                                 BibEntryTypesManager entryTypesManager) {
        super("academicpages", "Academic Pages", StandardFileType.MARKDOWN);
        this.layoutPreferences = layoutPreferences;
        this.fieldPreferences = fieldPreferences;
        this.entryTypesManager = entryTypesManager;
    }

    @Override
    public void export(BibDatabaseContext databaseContext, Path outputFile, List<BibEntry> entries) throws SaveException {
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
        Path parent = outputFile.getParent() != null ? outputFile.getParent() : Path.of(".");
        Path publicationsDir = parent.resolve(folderName).resolve("_publications");
        Path filesDir = parent.resolve(folderName).resolve("files");

        try {
            Files.createDirectories(publicationsDir);
            Files.createDirectories(filesDir);
        } catch (IOException ex) {
            throw new SaveException(ex);
        }

        for (BibEntry entry : entries.stream().sorted(ENTRY_TYPE_ORDER).toList()) {
            Path mdFile = publicationsDir.resolve(generateFileName(entry, publicationsDir));
            try {
                Files.writeString(mdFile, buildMarkdown(entry, filesDir, databaseContext, fileDirForDatabase, abbreviationRepository));
            } catch (IOException ex) {
                throw new SaveException(ex);
            }
        }
    }

    private String generateFileName(BibEntry entry, Path targetDir) {
        String key = FileUtil.getValidFileName(entry.getCitationKey().orElse("unknown"));
        return FileNameUniqueness.generateUniqueFileName(targetDir, resolveDate(entry) + "-" + key + ".md");
    }

    /// Builds the full Markdown file: YAML front matter (layout + computed fields) + optional abstract body
    private String buildMarkdown(BibEntry entry,
                                 Path filesDir,
                                 BibDatabaseContext databaseContext,
                                 List<Path> fileDirForDatabase,
                                 JournalAbbreviationRepository abbreviationRepository) {
        String date = resolveDate(entry);
        String key = FileUtil.getValidFileName(entry.getCitationKey().orElse("unknown"));

        StringBuilder sb = new StringBuilder("---\n");

        // Layout engine renders: title, collection, excerpt (note), venue, category
        renderLayout(entry, databaseContext.getDatabase(), fileDirForDatabase, abbreviationRepository)
                .ifPresent(rendered -> sb.append(rendered.stripTrailing()).append("\n"));

        // permalink, date, paperurl, bibtexurl, citation
        sb.append("permalink: /").append(PUBLICATION_PATH).append("/").append(date).append("-").append(key).append("\n");
        sb.append("date: ").append(date).append("\n");
        copyPdfAndGetUrl(entry, filesDir, key, fileDirForDatabase)
                .ifPresent(url -> sb.append("paperurl: '/files/").append(url).append("'\n"));
        writeBibFile(entry, filesDir, key)
                .ifPresent(url -> sb.append("bibtexurl: '/files/").append(url).append("'\n"));
        sb.append("citation: '").append(generateCitation(entry, databaseContext).replace("'", "''")).append("'\n");

        sb.append("---");

        entry.getFieldOrAliasLatexFree(StandardField.ABSTRACT)
             .ifPresent(abs -> sb.append("\n\n").append(abs));

        return sb.append("\n").toString();
    }

    /// Renders entry-type-specific YAML fields using a layout file
    /// Falls back to the default layout if no type-specific layout exists
    private Optional<String> renderLayout(BibEntry entry,
                                          BibDatabase database,
                                          List<Path> fileDirForDatabase,
                                          JournalAbbreviationRepository abbreviationRepository) {
        String typeName = entry.getType().getName().toLowerCase();
        return loadLayout(LAYOUT_PREFIX + "." + typeName + LAYOUT_EXTENSION, fileDirForDatabase, abbreviationRepository)
                .or(() -> loadLayout(LAYOUT_PREFIX + LAYOUT_EXTENSION, fileDirForDatabase, abbreviationRepository))
                .map(layout -> layout.doLayout(entry, database));
    }

    private Optional<Layout> loadLayout(String resourcePath,
                                        List<Path> fileDirForDatabase,
                                        JournalAbbreviationRepository abbreviationRepository) {
        try (InputStream is = AcademicPagesExporter.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                return Optional.empty();
            }
            try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                return Optional.of(new LayoutHelper(reader, fileDirForDatabase, layoutPreferences, abbreviationRepository)
                        .getLayoutFromText());
            }
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    /// Copies the first attached PDF to the files directory and returns its filename.
    private Optional<String> copyPdfAndGetUrl(BibEntry entry, Path outputDir, String key, List<Path> fileDirForDatabase) {
        return entry.getFiles().stream()
                    .filter(f -> "pdf".equalsIgnoreCase(f.getFileType()))
                    .findFirst()
                    .flatMap(f -> f.findIn(fileDirForDatabase))
                    .flatMap(source -> {
                        String destName = key + ".pdf";
                        Path target = outputDir.resolve(destName).normalize();
                        if (!target.startsWith(outputDir)) {
                            return Optional.empty();
                        }
                        try {
                            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                            return Optional.of(destName);
                        } catch (IOException ex) {
                            return Optional.empty();
                        }
                    });
    }

    /// Writes a .bib file for the entry to the files directory and returns its filename.
    private Optional<String> writeBibFile(BibEntry entry, Path outputDir, String key) {
        String bibContent = entry.getStringRepresentation(entry, BibDatabaseMode.BIBTEX, entryTypesManager, fieldPreferences);
        if (bibContent.isEmpty()) {
            return Optional.empty();
        }
        String bibName = key + ".bib";
        Path target = outputDir.resolve(bibName).normalize();
        if (!target.startsWith(outputDir)) {
            return Optional.empty();
        }
        try {
            Files.writeString(target, bibContent);
            return Optional.of(bibName);
        } catch (IOException ex) {
            return Optional.empty();
        }
    }

    private String generateCitation(BibEntry entry, BibDatabaseContext databaseContext) {
        BibDatabaseContext ctx = new BibDatabaseContext(new BibDatabase(List.of(entry)));
        ctx.setMode(databaseContext.getMode());
        return CitationStyleGenerator.generateBibliography(
                List.of(entry),
                CSLStyleLoader.getDefaultStyle().getSource(),
                CitationStyleOutputFormat.TEXT,
                ctx,
                entryTypesManager).getFirst().trim();
    }

    private String resolveDate(BibEntry entry) {
        return entry.getFieldOrAlias(StandardField.DATE)
                    .flatMap(Date::parse)
                    .map(date -> formatDate(date.toTemporalAccessor()))
                    .orElse("0000-01-01");
    }

    private String formatDate(TemporalAccessor temporal) {
        for (String pattern : List.of("uuuu-MM-dd", "uuuu-MM", "uuuu")) {
            try {
                String formatted = DateTimeFormatter.ofPattern(pattern).format(temporal);
                return formatted + "-01".repeat((int) pattern.chars().filter(c -> c == '-').count() < 2
                                                ? 2 - (int) pattern.chars().filter(c -> c == '-').count() : 0);
            } catch (DateTimeException ignored) {
                // try next pattern
            }
        }
        return "0000-01-01";
    }
}
