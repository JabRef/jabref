package org.jabref.logic.directorylibrary;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.exporter.HayagrivaEntryWriter;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UserSpecificCommentField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownSidecarTest {

    private static final String SIDECAR = """
            ---
            smith2020:
                type: article
                title: A Test Article
                author: Smith, Jane
            ---

            # Notes

            Shared comment text.

            ## comment-koppor

            Per-user comment text.

            ## Ideas

            File-only content, not imported.
            """;

    @TempDir
    Path tempDir;

    private final MarkdownSidecar sidecar = new MarkdownSidecar();

    private Path write(String fileName, String content) throws IOException {
        Path file = tempDir.resolve(fileName);
        Files.writeString(file, content);
        return file;
    }

    @Test
    void readsFrontmatterEntryWithBodyComments() throws IOException {
        Path file = write("smith2020.md", SIDECAR);

        List<BibEntry> entries = sidecar.read(file).getDatabase().getEntries();

        assertEquals(1, entries.size());
        BibEntry entry = entries.getFirst();
        assertEquals(Optional.of("smith2020"), entry.getCitationKey());
        assertEquals(Optional.of("A Test Article"), entry.getField(StandardField.TITLE));
        assertEquals(Optional.of("Shared comment text."), entry.getField(StandardField.COMMENT));
        assertEquals(Optional.of("Per-user comment text."), entry.getField(FieldFactory.parseField("comment-koppor")));
    }

    @Test
    void recognizesSidecarByItsFrontmatter() throws IOException {
        Path file = write("smith2020.md", SIDECAR);

        assertTrue(sidecar.looksLikeSidecar(file));
    }

    @Test
    void plainMarkdownIsNoSidecar() throws IOException {
        Path file = write("README.md", """
                # A readme

                Just prose.
                """);

        assertFalse(sidecar.looksLikeSidecar(file));
    }

    @Test
    void nonHayagrivaFrontmatterIsNoSidecar() throws IOException {
        Path file = write("post.md", """
                ---
                title: A blog post
                layout: default
                ---

                Content.
                """);

        assertFalse(sidecar.looksLikeSidecar(file));
    }

    @Test
    void bodyWithoutNotesHeadingStillBecomesComment() throws IOException {
        Path file = write("smith2020.md", """
                ---
                smith2020:
                    type: article
                    title: A Test Article
                ---

                Comment without a heading.
                """);

        BibEntry entry = sidecar.read(file).getDatabase().getEntries().getFirst();

        assertEquals(Optional.of("Comment without a heading."), entry.getField(StandardField.COMMENT));
    }

    @Test
    void multiParagraphCommentKeepsItsInnerBlankLine() throws IOException {
        Path file = write("smith2020.md", """
                ---
                smith2020:
                    type: article
                    title: A Test Article
                ---

                # Notes

                First paragraph.

                Second paragraph.
                """);

        BibEntry entry = sidecar.read(file).getDatabase().getEntries().getFirst();

        assertEquals(Optional.of("First paragraph.\n\nSecond paragraph."), entry.getField(StandardField.COMMENT));
    }

    @Test
    void mergeRoundTripsCommentsThroughTheBody() throws IOException {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("smith2020")
                .withField(StandardField.TITLE, "A Test Article")
                .withField(StandardField.COMMENT, "Shared comment text.")
                .withField(new UserSpecificCommentField("koppor"), "Per-user comment text.");

        String document = sidecar.merge(null, List.of(new HayagrivaEntryWriter.KeyedEntry("", "smith2020", entry)));
        Path file = write("smith2020.md", document);

        assertTrue(document.startsWith("---\n"), () -> "missing frontmatter: " + document);
        assertTrue(document.contains("\n# Notes\n\nShared comment text.\n\n## comment-koppor\n\nPer-user comment text.\n"),
                () -> "unexpected body: " + document);
        BibEntry reimported = sidecar.read(file).getDatabase().getEntries().getFirst();
        assertEquals(entry.getField(StandardField.COMMENT), reimported.getField(StandardField.COMMENT));
        assertEquals(entry.getField(new UserSpecificCommentField("koppor")),
                reimported.getField(new UserSpecificCommentField("koppor")));
        assertEquals(entry.getField(StandardField.TITLE), reimported.getField(StandardField.TITLE));
    }

    @Test
    void emptyBodySetsNoCommentFields() throws IOException {
        Path file = write("smith2020.md", """
                ---
                smith2020:
                    type: article
                    title: A Test Article
                ---
                """);

        BibEntry entry = sidecar.read(file).getDatabase().getEntries().getFirst();

        assertEquals(Optional.empty(), entry.getField(StandardField.COMMENT));
    }
}
