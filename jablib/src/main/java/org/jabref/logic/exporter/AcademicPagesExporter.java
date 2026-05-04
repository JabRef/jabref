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
import java.util.Map;
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
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;

import org.jspecify.annotations.NullMarked;

/// Exports JabRef entries to the [academicpages](https://academicpages.github.io) Jekyll template format.
/// Each {@link BibEntry} is written as a separate Markdown file under {@code _publications/}.
/// YAML front matter is rendered via JabRef's layout engine; file management and computed fields are handled in Java.
@NullMarked
public class AcademicPagesExporter extends Exporter {

    private static final String PUBLICATION_PATH = "publication";

    /// Orders entries by type priority: book -> article -> incollection -> inproceedings -> others
    private static final Map<EntryType, Integer> ENTRY_TYPE_PRIORITY = Map.of(
            StandardEntryType.Book, 1,
            StandardEntryType.Article, 2,
            StandardEntryType.InCollection, 3,
            StandardEntryType.InProceedings, 4
    );

    private static final Comparator<BibEntry> ENTRY_TYPE_ORDER = Comparator.comparingInt(
            entry -> ENTRY_TYPE_PRIORITY.getOrDefault(entry.getType(), 0)
    );

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
            String markdown = buildMarkdown(entry, filesDir, databaseContext, fileDirForDatabase, abbreviationRepository);
            // Normalize line endings to \n for Jekyll compatibility
            markdown = markdown.replace("\r\n", "\n").replace("\r", "\n");

            Path mdFile = publicationsDir.resolve(generateFileName(entry, publicationsDir));
            try {
                Files.writeString(mdFile, markdown);
            } catch (IOException ex) {
                throw new SaveException(ex);
            }
        }
    }

    private String generateFileName(BibEntry entry, Path targetDir) {
        String key = FileUtil.getValidFileName(entry.getCitationKey().orElse("unknown"));
        String prefix = resolveDate(entry).map(d -> d + "-").orElse("");
        return FileNameUniqueness.generateUniqueFileName(targetDir, prefix + key + ".md");
    }

    /// Builds the full Markdown file: YAML front matter (layout + computed fields) + optional abstract body
    private String buildMarkdown(BibEntry entry,
                                 Path filesDir,
                                 BibDatabaseContext databaseContext,
                                 List<Path> fileDirForDatabase,
                                 JournalAbbreviationRepository abbreviationRepository) {
        Optional<String> date = resolveDate(entry);
        String key = FileUtil.getValidFileName(entry.getCitationKey().orElse("unknown"));

        StringBuilder sb = new StringBuilder("---\n");

        // Layout engine renders: title, collection, excerpt (note), venue, category
        renderLayout(entry, databaseContext.getDatabase(), fileDirForDatabase, abbreviationRepository)
                .ifPresent(rendered -> sb.append(rendered.stripTrailing()).append("\n"));

        // permalink, date, paperurl, bibtexurl, citation
        date.ifPresent(d -> {
            sb.append("permalink: /").append(PUBLICATION_PATH).append("/").append(d).append("-").append(key).append("\n");
            sb.append("date: ").append(d).append("\n");
        });
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
        String layoutDir = TemplateExporter.LAYOUT_PREFIX + "academicpages/academicpages";
        return loadLayout(layoutDir + "." + typeName + TemplateExporter.LAYOUT_EXTENSION, fileDirForDatabase, abbreviationRepository)
                .or(() -> loadLayout(layoutDir + TemplateExporter.LAYOUT_EXTENSION, fileDirForDatabase, abbreviationRepository))
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

    private Optional<String> resolveDate(BibEntry entry) {
        return entry.getFieldOrAlias(StandardField.DATE)
                    .flatMap(Date::parse)
                    .map(date -> formatDate(date.toTemporalAccessor()));
    }

    private String formatDate(TemporalAccessor temporal) {
        try {
            return DateTimeFormatter.ofPattern("uuuu-MM-dd").format(temporal);
        } catch (DateTimeException e1) {
            try {
                return DateTimeFormatter.ofPattern("uuuu-MM").format(temporal) + "-01";
            } catch (DateTimeException e2) {
                return DateTimeFormatter.ofPattern("uuuu").format(temporal) + "-01-01";
            }
        }
    }
}
