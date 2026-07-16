package org.jabref.logic.directorylibrary;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.HayagrivaImporter;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.StandardField;

import org.jspecify.annotations.NullMarked;

/// A directory-library sidecar in Markdown form: `X.md` next to `X.pdf`. The YAML frontmatter
/// (between two `---` lines) is a regular Hayagriva document carrying the bibliographic data;
/// the Markdown body below carries JabRef's long-form notes. The text under the `# Notes`
/// heading is the entry's comment field, and every `## comment-<name>` section is the
/// corresponding per-user comment field. Body content under other headings is not imported and
/// stays file-only, so the file remains a normal, markdownlint-clean Markdown note (usable in
/// Obsidian and plain editors); Typst users extract the frontmatter to obtain a plain Hayagriva
/// file.
///
/// The body always describes the frontmatter's first entry; JabRef-authored Markdown sidecars
/// contain exactly one entry.
@NullMarked
public class MarkdownSidecar {

    public static final String MARKDOWN_EXTENSION = "md";
    static final String FRONTMATTER_DELIMITER = "---";

    /// Field name (and name prefix) of JabRef's comment fields, see
    /// [org.jabref.model.entry.field.UserSpecificCommentField].
    private static final String COMMENT_FIELD_PREFIX = "comment-";
    private static final String SECTION_HEADING_PREFIX = "## ";

    private final HayagrivaImporter importer = new HayagrivaImporter();

    /// The split of a sidecar's raw text into its Hayagriva frontmatter and its notes body.
    record Document(String frontmatter, String body) {
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
        String currentSection = "";
        boolean titleSkipped = false;
        StringBuilder currentText = new StringBuilder();
        for (String line : body.lines().toList()) {
            if (line.startsWith(SECTION_HEADING_PREFIX)) {
                applySection(entry, currentSection, currentText.toString());
                currentSection = line.substring(SECTION_HEADING_PREFIX.length()).strip();
                currentText.setLength(0);
            } else if (!titleSkipped && currentSection.isEmpty() && currentText.toString().isBlank() && line.startsWith("# ")) {
                titleSkipped = true;
            } else {
                currentText.append(line).append('\n');
            }
        }
        applySection(entry, currentSection, currentText.toString());
    }

    private static void applySection(BibEntry entry, String section, String text) {
        String value = text.strip();
        if (value.isEmpty()) {
            return;
        }
        if (section.isEmpty()) {
            entry.setField(StandardField.COMMENT, value);
        } else if (section.startsWith(COMMENT_FIELD_PREFIX)) {
            entry.setField(FieldFactory.parseField(section), value);
        }
    }
}
