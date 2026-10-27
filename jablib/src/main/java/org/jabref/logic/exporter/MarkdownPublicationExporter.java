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
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.Layout;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.layout.LayoutHelper;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Date;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.Month;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.groups.GroupTreeNode;

import org.jspecify.annotations.NullMarked;

// [impl->feat~export.markdown-publications~1]
@NullMarked
public class MarkdownPublicationExporter extends Exporter {

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
        if (entries.isEmpty()) {
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
                file
        ));
        sections.add("## " + Localization.lang("All publications") + "\n\n"
                + renderPublications(
                sorted,
                layout,
                databaseContext,
                fileDirectories,
                file
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
                                       file
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
            Path outputFile) {
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
            Path outputFile) {
        if (!hasMatchingPublications(group, sorted)) {
            return;
        }

        String heading = group.getGroup().getDescription()
                              .filter(description -> !description.isBlank())
                              .orElse(group.getGroup().getName())
                              .replace('\r', ' ')
                              .replace('\n', ' ');

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
            Path outputFile) {
        String publication =
                layout.doLayout(entry, databaseContext.getDatabase());

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
            Path outputFile) {
        if (linkedFile.isOnlineLink()) {
            String link = linkedFile.getLink();
            if (link.regionMatches(true, 0, "www.", 0, 4)) {
                link = "https://" + link;
            }
            return Optional.of(
                    link
                            .replace(" ", "%20")
                            .replace("<", "%3C")
                            .replace(">", "%3E")
            );
        }

        return linkedFile.findIn(fileDirectories)
                         .flatMap(path -> relativePdfLink(outputFile, path));
    }

    private Optional<String> relativePdfLink(
            Path outputFile,
            Path pdfFile) {
        Path outputDirectory = outputFile.toAbsolutePath()
                                         .normalize()
                                         .getParent();
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
            return entry.getPublicationDate()
                        .or(() -> Date.parse(
                                entry.getField(StandardField.YEAR),
                                entry.getField(StandardField.MONTH),
                                entry.getField(StandardField.DAY)))
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
}
