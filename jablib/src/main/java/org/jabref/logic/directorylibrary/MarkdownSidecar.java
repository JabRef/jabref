package org.jabref.logic.directorylibrary;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.jabref.logic.exporter.HayagrivaEntryWriter;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.HayagrivaImporter;
import org.jabref.logic.importer.fileformat.HayagrivaMapping;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.StandardField;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import static java.util.function.Predicate.not;

/// A directory-library sidecar in Markdown form: `X.md` next to `X.pdf`. The YAML frontmatter
/// (between two `---` lines) is a regular Hayagriva document carrying the bibliographic data;
/// the Markdown body below carries JabRef's long-form notes. The text under the `# Notes`
/// heading is the entry's comment field, and every `## comment-<name>` section is the
/// corresponding per-user comment field. Body content under other headings is not imported but
/// survives rewrites verbatim, so the file remains a normal, markdownlint-clean Markdown note
/// (usable in Obsidian and plain editors); Typst users extract the frontmatter to obtain a
/// plain Hayagriva file.
///
/// The body always describes the frontmatter's first entry; JabRef-authored Markdown sidecars
/// contain exactly one entry.
@NullMarked
public class MarkdownSidecar {

    public static final String MARKDOWN_EXTENSION = "md";
    static final String FRONTMATTER_DELIMITER = "---";
    static final String NOTES_HEADING = "# Notes";
    private static final String SECTION_HEADING_PREFIX = "## ";

    private final HayagrivaImporter importer = new HayagrivaImporter();
    private final HayagrivaEntryWriter entryWriter = new HayagrivaEntryWriter();

    /// The split of a sidecar's raw text into its Hayagriva frontmatter and its notes body.
    record Document(String frontmatter, String body) {
    }

    /// A `##` section of the notes body, with its content stripped of surrounding blank lines.
    private record Section(String heading, String content) {
    }

    /// The parsed notes body: the intro under the (dropped) `# Notes` document heading, and the
    /// `##` sections in file order.
    private record Body(String intro, List<Section> sections) {
    }

    public static boolean hasMarkdownExtension(Path file) {
        return MARKDOWN_EXTENSION.equals(FileUtil.getFileExtension(file).orElse("").toLowerCase(Locale.ROOT));
    }

    /// Mirrors [HayagrivaImporter#isRecognizedFormat]: a Markdown file is a sidecar when it
    /// opens with a frontmatter block that is recognized as Hayagriva. Arbitrary Markdown
    /// (READMEs, plain notes) is no sidecar.
    public boolean looksLikeSidecar(Path file) throws IOException {
        Optional<Document> document = split(Files.readString(file, StandardCharsets.UTF_8));
        if (document.isEmpty()) {
            return false;
        }
        try (BufferedReader reader = new BufferedReader(Reader.of(document.get().frontmatter()))) {
            return importer.isRecognizedFormat(reader);
        }
    }

    /// Reads a Markdown sidecar: the frontmatter through the Hayagriva importer, the notes body
    /// into the first entry's comment fields. A file without a frontmatter block yields an empty
    /// result (callers recognize sidecars via [#looksLikeSidecar] first).
    public ParserResult read(Path file) throws IOException {
        Optional<Document> document = split(Files.readString(file, StandardCharsets.UTF_8));
        if (document.isEmpty()) {
            return new ParserResult();
        }
        ParserResult result;
        try (BufferedReader reader = new BufferedReader(Reader.of(document.get().frontmatter()))) {
            result = importer.importDatabase(reader);
        }
        List<BibEntry> entries = result.getDatabase().getEntries();
        if (!entries.isEmpty()) {
            applyBody(entries.getFirst(), document.get().body());
        }
        return result;
    }

    /// Merges the given entries into a Markdown sidecar document (read-modify-write): the
    /// frontmatter through [HayagrivaEntryWriter#mergeIntoDocument] — with the comment fields
    /// stripped, they live in the body — and the body by regenerating the intro and the
    /// `## comment-<name>` sections from the first entry while keeping foreign sections
    /// verbatim. The document heading is normalized to `# Notes`.
    public String merge(@Nullable String existingDocument, List<HayagrivaEntryWriter.KeyedEntry> entries) {
        Optional<Document> existing = Optional.ofNullable(existingDocument).flatMap(MarkdownSidecar::split);
        List<HayagrivaEntryWriter.KeyedEntry> frontmatterEntries = entries.stream()
                                                                          .map(keyed -> new HayagrivaEntryWriter.KeyedEntry(keyed.previousKey(), keyed.targetKey(), withoutCommentFields(keyed.entry())))
                                                                          .toList();
        String frontmatter = entryWriter.mergeIntoDocument(existing.map(Document::frontmatter).orElse(null), frontmatterEntries);
        String body = entries.isEmpty() ? "" : renderBody(existing.map(Document::body).orElse(""), entries.getFirst().entry());

        StringBuilder document = new StringBuilder();
        document.append(FRONTMATTER_DELIMITER).append('\n');
        document.append(frontmatter);
        if (!frontmatter.isEmpty() && !frontmatter.endsWith("\n")) {
            document.append('\n');
        }
        document.append(FRONTMATTER_DELIMITER).append('\n');
        if (!body.isEmpty()) {
            document.append('\n').append(body);
        }
        return document.toString();
    }

    /// Splits the raw text at the frontmatter delimiters: the first line must be `---`, the
    /// frontmatter runs until the next `---` line, the body is everything below.
    static Optional<Document> split(String content) {
        List<String> lines = content.lines().toList();
        if (lines.isEmpty() || !FRONTMATTER_DELIMITER.equals(lines.getFirst().strip())) {
            return Optional.empty();
        }
        for (int end = 1; end < lines.size(); end++) {
            if (FRONTMATTER_DELIMITER.equals(lines.get(end).strip())) {
                return Optional.of(new Document(
                        String.join("\n", lines.subList(1, end)),
                        String.join("\n", lines.subList(end + 1, lines.size()))));
            }
        }
        return Optional.empty();
    }

    /// The intro under the (optional) `# Notes` document heading becomes the comment field;
    /// every `## comment-<name>` section becomes the equally named per-user comment field.
    static void applyBody(BibEntry entry, String body) {
        Body parsed = parseBody(body);
        if (!parsed.intro().isEmpty()) {
            entry.setField(StandardField.COMMENT, parsed.intro());
        }
        for (Section section : parsed.sections()) {
            if (isCommentSection(section.heading()) && !section.content().isEmpty()) {
                entry.setField(FieldFactory.parseField(section.heading()), section.content());
            }
        }
    }

    private static Body parseBody(String body) {
        String currentHeading = "";
        boolean titleSkipped = false;
        StringBuilder currentText = new StringBuilder();
        String intro = "";
        List<Section> sections = new ArrayList<>();
        for (String line : body.lines().toList()) {
            if (line.startsWith(SECTION_HEADING_PREFIX)) {
                if (currentHeading.isEmpty()) {
                    intro = currentText.toString().strip();
                } else {
                    sections.add(new Section(currentHeading, currentText.toString().strip()));
                }
                currentHeading = line.substring(SECTION_HEADING_PREFIX.length()).strip();
                currentText.setLength(0);
            } else if (!titleSkipped && currentHeading.isEmpty() && currentText.toString().isBlank() && line.startsWith("# ")) {
                titleSkipped = true;
            } else {
                currentText.append(line).append('\n');
            }
        }
        if (currentHeading.isEmpty()) {
            intro = currentText.toString().strip();
        } else {
            sections.add(new Section(currentHeading, currentText.toString().strip()));
        }
        return new Body(intro, sections);
    }

    /// Regenerates the notes body: the entry's comment as the intro, existing comment sections
    /// updated in place (dropped when cleared), foreign sections kept verbatim in their
    /// position, and comment fields without an existing section appended in name order.
    private static String renderBody(String existingBody, BibEntry entry) {
        Body existing = parseBody(existingBody);
        List<String> blocks = new ArrayList<>();
        commentValue(entry, StandardField.COMMENT).ifPresent(blocks::add);

        Set<String> existingHeadings = new HashSet<>();
        for (Section section : existing.sections()) {
            if (isCommentSection(section.heading())) {
                existingHeadings.add(section.heading());
                commentValue(entry, FieldFactory.parseField(section.heading()))
                        .ifPresent(value -> blocks.add(SECTION_HEADING_PREFIX + section.heading() + "\n\n" + value));
            } else if (!section.content().isEmpty()) {
                blocks.add(SECTION_HEADING_PREFIX + section.heading() + "\n\n" + section.content());
            } else {
                blocks.add(SECTION_HEADING_PREFIX + section.heading());
            }
        }

        entry.getFields().stream()
             .map(Field::getName)
             .filter(MarkdownSidecar::isCommentSection)
             .filter(name -> !existingHeadings.contains(name))
             .sorted()
             .forEach(name -> commentValue(entry, FieldFactory.parseField(name))
                     .ifPresent(value -> blocks.add(SECTION_HEADING_PREFIX + name + "\n\n" + value)));

        if (blocks.isEmpty()) {
            return "";
        }
        return NOTES_HEADING + "\n\n" + String.join("\n\n", blocks) + "\n";
    }

    private static Optional<String> commentValue(BibEntry entry, Field field) {
        return entry.getField(field).map(String::strip).filter(not(String::isEmpty));
    }

    private static boolean isCommentSection(String heading) {
        return heading.startsWith(HayagrivaMapping.USER_COMMENT_PREFIX);
    }

    private static BibEntry withoutCommentFields(BibEntry entry) {
        BibEntry copy = new BibEntry(entry);
        copy.clearField(StandardField.COMMENT);
        copy.getFields().stream()
            .filter(field -> field.getName().startsWith(HayagrivaMapping.USER_COMMENT_PREFIX))
            .toList()
            .forEach(copy::clearField);
        return copy;
    }
}
