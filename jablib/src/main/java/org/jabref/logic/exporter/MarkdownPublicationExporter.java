package org.jabref.logic.exporter;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.Layout;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.layout.LayoutHelper;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Date;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.Month;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.groups.GroupTreeNode;

import org.jspecify.annotations.NullMarked;

// [impl->feat~export.markdown-publications~1]
@NullMarked
public class MarkdownPublicationExporter extends Exporter {

    private static final Pattern DAY = Pattern.compile("[0-9]+");
    private static final Pattern HEADING_MARKUP = Pattern.compile("[\\\\`*_{}\\[\\]()#+.!|<>-]");
    private static final Set<String> ONLINE_SCHEMES = Set.of("http", "https", "ftp", "ftps");

    private final FilePreferences filePreferences;
    private final LayoutFormatterPreferences layoutPreferences;

    public MarkdownPublicationExporter(
            LayoutFormatterPreferences layoutPreferences) {
        this(layoutPreferences, FilePreferences.getDefault());
    }

    public MarkdownPublicationExporter(
            LayoutFormatterPreferences layoutPreferences,
            FilePreferences filePreferences) {
        super(
                "markdown-publications",
                Localization.lang("Markdown publications"),
                StandardFileType.MARKDOWN
        );
        this.filePreferences = filePreferences;
        this.layoutPreferences = layoutPreferences;
    }

    @Override
    public void export(
            BibDatabaseContext databaseContext,
            Path file,
            List<BibEntry> entries) throws IOException {
        List<Path> directories = databaseContext.getFileDirectories(filePreferences);

        export(
                databaseContext,
                file,
                entries,
                directories,
                JournalAbbreviationLoader.loadBuiltInRepository()
        );
    }

    @Override
    public void export(
            BibDatabaseContext databaseContext,
            Path file,
            List<BibEntry> entries,
            List<Path> fileDirectories,
            JournalAbbreviationRepository abbreviationRepository)
            throws IOException {
        export(databaseContext, file, entries, fileDirectories, abbreviationRepository, Optional.of(file));
    }

    @Override
    public void exportToStandardOutput(
            BibDatabaseContext databaseContext,
            Path temporaryFile,
            List<BibEntry> entries,
            List<Path> fileDirectories,
            JournalAbbreviationRepository abbreviationRepository) throws IOException {
        export(databaseContext, temporaryFile, entries, fileDirectories, abbreviationRepository, Optional.empty());
    }

    private void export(
            BibDatabaseContext databaseContext,
            Path file,
            List<BibEntry> entries,
            List<Path> suppliedFileDirectories,
            JournalAbbreviationRepository abbreviationRepository,
            Optional<Path> linkDestination) throws IOException {
        List<Path> fileDirectories = Stream.concat(
                                                   suppliedFileDirectories.stream(),
                                                   databaseContext.getFileDirectories(filePreferences).stream())
                                           .distinct()
                                           .toList();

        if (entries.isEmpty()) {
            try (AtomicFileWriter writer =
                         new AtomicFileWriter(file, StandardCharsets.UTF_8)) {
                writer.write("");
            }
            return;
        }

        List<BibEntry> sorted = entries.stream()
                                       .sorted(Comparator
                                               .comparingLong(this::publicationDateKey)
                                               .reversed()
                                               .thenComparing(entry ->
                                                       entry.getCitationKey().orElse(""))
                                               .thenComparing(entry ->
                                                       entry.getField(StandardField.TITLE).orElse("")))
                                       .toList();

        var resource = Optional.ofNullable(
                                       MarkdownPublicationExporter.class.getResourceAsStream(
                                               "/resource/layout/markdown-publications/publication.layout"))
                               .orElseThrow(() ->
                                       new IOException(Localization.lang("Cannot find Markdown publication layout.")));

        Layout layout;
        try (Reader reader =
                     new InputStreamReader(resource, StandardCharsets.UTF_8)) {
            LayoutHelper helper = new LayoutHelper(
                    reader,
                    fileDirectories,
                    layoutPreferences,
                    abbreviationRepository
            );
            layout = Optional.ofNullable(helper.getLayoutFromText())
                             .orElseThrow(() ->
                                     new IOException(Localization.lang("Cannot load Markdown publication layout.")));
        }

        StringJoiner sections = new StringJoiner("\n\n", "", "\n");
        sections.add("## " + Localization.lang("Recent publications") + "\n\n"
                + renderPublications(
                sorted.stream().limit(5).toList(),
                layout,
                databaseContext,
                fileDirectories,
                linkDestination
        ));
        sections.add("## " + Localization.lang("All publications") + "\n\n"
                + renderPublications(
                sorted,
                layout,
                databaseContext,
                fileDirectories,
                linkDestination
        ));

        databaseContext.getMetaData().getGroups()
                       .flatMap(root -> root.findGroupByName("Export"))
                       .ifPresent(exportGroup -> {
                           for (GroupTreeNode subgroup : exportGroup.getChildren()) {
                               appendGroupSection(
                                       sections,
                                       subgroup,
                                       3,
                                       sorted,
                                       layout,
                                       databaseContext,
                                       fileDirectories,
                                       linkDestination
                               );
                           }
                       });

        try (AtomicFileWriter writer =
                     new AtomicFileWriter(file, StandardCharsets.UTF_8)) {
            writer.write(sections.toString());
        }
    }

    private String renderPublications(
            List<BibEntry> entries,
            Layout layout,
            BibDatabaseContext databaseContext,
            List<Path> fileDirectories,
            Optional<Path> outputFile) {
        return entries.stream()
                      .map(entry -> renderPublication(
                              entry,
                              layout,
                              databaseContext,
                              fileDirectories,
                              outputFile
                      ))
                      .collect(Collectors.joining())
                      .stripTrailing();
    }

    // [impl->req~export.markdown-publications.groups~1]
    private void appendGroupSection(
            StringJoiner sections,
            GroupTreeNode group,
            int headingLevel,
            List<BibEntry> sorted,
            Layout layout,
            BibDatabaseContext databaseContext,
            List<Path> fileDirectories,
            Optional<Path> outputFile) {
        if (!hasMatchingPublications(group, sorted)) {
            return;
        }

        String heading = group.getGroup().getDescription()
                              .filter(description -> !description.isBlank())
                              .orElse(group.getGroup().getName())
                              .replace('\r', ' ')
                              .replace('\n', ' ');
        heading = HEADING_MARKUP.matcher(heading.replace("&", "&amp;"))
                                .replaceAll("\\\\$0");

        StringJoiner section = new StringJoiner("\n\n");
        section.add("#".repeat(Math.min(headingLevel, 6)) + " " + heading);

        String publications = renderPublications(
                group.findMatches(sorted),
                layout,
                databaseContext,
                fileDirectories,
                outputFile
        );
        if (!publications.isEmpty()) {
            section.add(publications);
        }
        sections.add(section.toString());

        for (GroupTreeNode child : group.getChildren()) {
            appendGroupSection(
                    sections,
                    child,
                    headingLevel + 1,
                    sorted,
                    layout,
                    databaseContext,
                    fileDirectories,
                    outputFile
            );
        }
    }

    private boolean hasMatchingPublications(
            GroupTreeNode group,
            List<BibEntry> entries) {
        if (!group.findMatches(entries).isEmpty()) {
            return true;
        }

        return group.getChildren().stream()
                    .anyMatch(child -> hasMatchingPublications(child, entries));
    }

    // [impl->req~export.markdown-publications.links~1]
    private String renderPublication(
            BibEntry entry,
            Layout layout,
            BibDatabaseContext databaseContext,
            List<Path> fileDirectories,
            Optional<Path> outputFile) {
        // Fields are already resolved; do not expand library string macros again after validating the URL.
        String publication =
                layout.doLayout(publicationForLayout(entry, databaseContext), new BibDatabase());

        Optional<String> pdfLink = entry.getFiles().stream()
                                        .filter(this::isPdfAttachment)
                                        .filter(linkedFile -> !linkedFile.getLink().isBlank())
                                        .map(linkedFile -> resolvePdfLink(
                                                linkedFile,
                                                fileDirectories,
                                                outputFile
                                        ))
                                        .flatMap(Optional::stream)
                                        .findFirst();

        return pdfLink
                .map(link -> publication.stripTrailing()
                        + " [PDF](<"
                        + link
                        + ">)\n")
                .orElse(publication);
    }

    private BibEntry publicationForLayout(BibEntry entry, BibDatabaseContext databaseContext) {
        BibEntry publication = new BibEntry(entry.getType());
        for (StandardField field : List.of(StandardField.AUTHOR, StandardField.TITLE,
                StandardField.JOURNAL, StandardField.BOOKTITLE, StandardField.YEAR)) {
            entry.getResolvedFieldOrAlias(field, databaseContext.getDatabase())
                 .ifPresent(value -> publication.withField(field, value));
        }

        entry.getResolvedFieldOrAlias(StandardField.DOI, databaseContext.getDatabase())
             .flatMap(DOI::parse)
             .flatMap(DOI::getExternalURI)
             .map(URI::toASCIIString)
             .flatMap(this::normalizeOnlineLink)
             .or(() -> entry.getResolvedFieldOrAlias(StandardField.URL, databaseContext.getDatabase())
                            .flatMap(this::normalizeOnlineLink))
             .ifPresent(link -> publication.withField(StandardField.URL, link));
        return publication;
    }

    private Optional<String> normalizeOnlineLink(String value) {
        String link = value.strip();
        if (link.regionMatches(true, 0, "www.", 0, 4)) {
            link = "https://" + link;
        }
        if (link.codePoints().anyMatch(Character::isISOControl)) {
            return Optional.empty();
        }

        String encoded = link.replace(" ", "%20")
                             .replace("<", "%3C")
                             .replace(">", "%3E")
                             .replace("\"", "%22")
                             .replace("\\", "%5C")
                             .replace("`", "%60");
        try {
            URI uri = new URI(encoded);
            return Optional.ofNullable(uri.getScheme())
                           .filter(scheme -> ONLINE_SCHEMES.contains(scheme.toLowerCase(Locale.ROOT)))
                           .flatMap(_ -> Optional.ofNullable(uri.getRawAuthority()))
                           .filter(authority -> !authority.isBlank())
                           .map(_ -> uri.toASCIIString());
        } catch (URISyntaxException _) {
            return Optional.empty();
        }
    }

    private boolean isPdfAttachment(LinkedFile linkedFile) {
        if ("PDF".equalsIgnoreCase(linkedFile.getFileType())) {
            return true;
        }

        return linkedFile.getFileType().isBlank()
                && linkedFile.getFileName()
                             .map(name -> name.regionMatches(
                                     true,
                                     name.length() - 4,
                                     ".pdf",
                                     0,
                                     4))
                             .orElse(false);
    }

    private Optional<String> resolvePdfLink(
            LinkedFile linkedFile,
            List<Path> fileDirectories,
            Optional<Path> outputFile) {
        if (linkedFile.isOnlineLink()) {
            return normalizeOnlineLink(linkedFile.getLink());
        }

        return outputFile.flatMap(destination -> linkedFile.findIn(fileDirectories)
                                                           .flatMap(path -> relativePdfLink(destination, path)));
    }

    private Optional<String> relativePdfLink(
            Path outputFile,
            Path pdfFile) {
        return Optional.ofNullable(outputFile.toAbsolutePath().normalize().getParent())
                       .flatMap(directory -> relativePdfLinkFromDirectory(directory, pdfFile));
    }

    private Optional<String> relativePdfLinkFromDirectory(
            Path outputDirectory,
            Path pdfFile) {
        Path absolutePdf = pdfFile.toAbsolutePath().normalize();

        try {
            Path relativePath = outputDirectory.relativize(absolutePdf);
            String portablePath = relativePath.toString()
                                              .replace(relativePath.getFileSystem().getSeparator(), "/");

            // Prevent a colon from being interpreted as a URI scheme.
            if (!portablePath.startsWith(".")
                    && portablePath.contains(":")) {
                portablePath = "./" + portablePath;
            }

            return Optional.of(
                    new URI(null, null, portablePath, null)
                            .toASCIIString()
            );
        } catch (IllegalArgumentException | URISyntaxException _) {
            // Different Windows drives cannot form a relative path.
            return Optional.empty();
        }
    }

    // [impl->req~export.markdown-publications.date-order~1]
    private long publicationDateKey(BibEntry entry) {
        try {
            return entry.getField(StandardField.DATE)
                        .map(Date::parse)
                        .orElseGet(() -> parseBibtexDate(entry))
                        .filter(date -> date.getYear().isPresent())
                        .map(date -> {
                            long year = date.getYear().orElseThrow();
                            int month = date.getMonth()
                                            .map(Month::getNumber)
                                            .orElse(0);
                            int day = date.getDay().orElse(0);

                            return year * 10000L + month * 100L + day;
                        })
                        .orElse(Long.MIN_VALUE);
        } catch (DateTimeException _) {
            return Long.MIN_VALUE;
        }
    }

    private Optional<Date> parseBibtexDate(BibEntry entry) {
        Optional<String> month = entry.getField(StandardField.MONTH);
        Optional<String> day = entry.getField(StandardField.DAY);
        if (month.filter(value -> Month.parse(value).isEmpty()).isPresent()
                || day.filter(value -> !DAY.matcher(value).matches()).isPresent()
                || day.filter(value -> value.length() > 2).isPresent()
                || (day.isPresent() && month.isEmpty())) {
            return Optional.empty();
        }
        // Date.parse otherwise silently discards unparseable month/day fields.
        return Date.parse(entry.getField(StandardField.YEAR), month, day);
    }
}
