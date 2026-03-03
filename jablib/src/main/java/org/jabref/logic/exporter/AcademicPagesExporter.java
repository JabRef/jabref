package org.jabref.logic.exporter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.citationstyle.CSLStyleLoader;
import org.jabref.logic.citationstyle.CitationStyleGenerator;
import org.jabref.logic.citationstyle.CitationStyleOutputFormat;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;

import org.jspecify.annotations.NonNull;

/// Exports a JabRef library to the <a href="https://academicpages.github.io">academicpages</a> Jekyll template format.
/// Each {@link BibEntry} is written to a separate Markdown file in the given output directory.
/// The file name follows the pattern `YYYY-MM-DD-citationkey.md`.
public class AcademicPagesExporter extends Exporter {

    private static final String COLLECTION = "publications";

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
    public void export(@NonNull BibDatabaseContext databaseContext,
                       @NonNull Path outputFile,
                       @NonNull List<BibEntry> entries) throws SaveException {
        export(databaseContext, outputFile, entries, List.of(), JournalAbbreviationLoader.loadBuiltInRepository());
    }

    @Override
    public void export(@NonNull BibDatabaseContext databaseContext,
                       @NonNull Path outputFile,
                       @NonNull List<BibEntry> entries,
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
            Path mdFile = publicationsDir.resolve(buildFileName(entry));
            try {
                Files.writeString(mdFile, buildContent(entry, filesDir, databaseContext, fileDirForDatabase));
            } catch (IOException ex) {
                throw new SaveException(ex);
            }
        }
    }

    private String buildFileName(BibEntry entry) {
        String key = entry.getCitationKey().orElse("unknown");
        return buildDate(entry) + "-" + key + ".md";
    }

    private String buildDate(BibEntry entry) {
        String year = entry.getField(StandardField.YEAR)
                           .or(() -> entry.getField(StandardField.DATE)
                                          .map(date -> date.length() >= 4 ? date.substring(0, 4) : date))
                           .orElse("0000");
        String month = normalizeMonth(entry.getField(StandardField.MONTH).orElse("01"));
        return year + "-" + month + "-01";
    }

    /// Normalizes a BibTeX month value  to a two-digit string
    private String normalizeMonth(String month) {
        return switch (month.toLowerCase(Locale.ROOT).trim()) {
            case "1",
                 "jan",
                 "january" ->
                    "01";
            case "2",
                 "feb",
                 "february" ->
                    "02";
            case "3",
                 "mar",
                 "march" ->
                    "03";
            case "4",
                 "apr",
                 "april" ->
                    "04";
            case "5",
                 "may" ->
                    "05";
            case "6",
                 "jun",
                 "june" ->
                    "06";
            case "7",
                 "jul",
                 "july" ->
                    "07";
            case "8",
                 "aug",
                 "august" ->
                    "08";
            case "9",
                 "sep",
                 "september" ->
                    "09";
            case "10",
                 "oct",
                 "october" ->
                    "10";
            case "11",
                 "nov",
                 "november" ->
                    "11";
            case "12",
                 "dec",
                 "december" ->
                    "12";
            default ->
                    "01";
        };
    }

    /// Builds the full Markdown file content: YAML front matter + abstract body
    private String buildContent(BibEntry entry, Path outputDir, BibDatabaseContext databaseContext, List<Path> fileDirForDatabase) {
        String date = buildDate(entry);
        String key = entry.getCitationKey().orElse("unknown");

        StringBuilder sb = new StringBuilder();
        sb.append("---\n");
        sb.append("title: \"").append(escapeDoubleQuotes(entry.getField(StandardField.TITLE).orElse(""))).append("\"\n");
        sb.append("collection: ").append(COLLECTION).append("\n");
        sb.append("permalink: /publication/").append(date).append("-").append(key).append("\n");
        entry.getField(StandardField.NOTE).ifPresent(note ->
                sb.append("excerpt: '").append(escapeSingleQuotes(note)).append("'\n"));
        sb.append("date: ").append(date).append("\n");
        buildVenue(entry).ifPresent(venue ->
                sb.append("venue: '").append(escapeSingleQuotes(removeLatexEscapes(venue))).append("'\n"));
        copyPdfAndGetUrl(entry, outputDir, key, fileDirForDatabase).ifPresent(paperUrl ->
                sb.append("paperurl: '/files/").append(paperUrl).append("'\n"));
        writeBibFile(entry, outputDir, key).ifPresent(bibUrl ->
                sb.append("bibtexurl: '/files/").append(bibUrl).append("'\n"));
        sb.append("citation: '").append(escapeSingleQuotes(buildCitation(entry, databaseContext))).append("'\n");
        sb.append("category: ").append(mapCategory(entry.getType())).append("\n");
        sb.append("---\n");

        entry.getField(StandardField.ABSTRACT).ifPresent(abstract_ ->
                sb.append(abstract_).append("\n"));

        return sb.toString();
    }

    private Optional<String> buildVenue(BibEntry entry) {
        return entry.getField(StandardField.JOURNAL)
                    .or(() -> entry.getField(StandardField.JOURNALTITLE))
                    .or(() -> entry.getField(StandardField.BOOKTITLE));
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
        try {
            Files.copy(source.get(), outputDir.resolve(destFileName), StandardCopyOption.REPLACE_EXISTING);
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
        try {
            Files.writeString(outputDir.resolve(bibFileName), bibContent);
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

    private String escapeDoubleQuotes(String value) {
        return value.replace("\"", "\\\"");
    }

    private String escapeSingleQuotes(String value) {
        return value.replace("'", "\\'");
    }

    /// Removes common LaTeX escape sequences for use in plain text contexts
    private String removeLatexEscapes(String value) {
        return value.replace("\\&", "&")
                    .replace("\\%", "%")
                    .replace("\\$", "$")
                    .replace("\\#", "#")
                    .replace("\\_", "_")
                    .replace("\\{", "{")
                    .replace("\\}", "}");
    }
}
