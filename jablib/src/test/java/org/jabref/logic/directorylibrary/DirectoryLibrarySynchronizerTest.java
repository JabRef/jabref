package org.jabref.logic.directorylibrary;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import javafx.collections.FXCollections;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fetcher.CrossRef;
import org.jabref.logic.importer.fetcher.DoiFetcher;
import org.jabref.logic.importer.util.GrobidPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.EntriesEventSource;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UserSpecificCommentField;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DirectoryLibrarySynchronizerTest {

    private static final String ARTICLE_YAML = """
            smith2020:
                type: article
                title: A Test Article
                author: Smith, Jane
                note: first version
            """;

    private static final String MARKDOWN_SIDECAR = """
            ---
            smith2020:
                type: article
                title: A Test Article
                author: Smith, Jane
            ---

            # Notes

            Shared comment text.
            """;

    /// Deterministic clock for the rename grace window.
    private static final class SteppingClock extends Clock {
        private Instant now = Instant.parse("2026-07-13T12:00:00Z");

        private void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }

    @TempDir
    Path root;

    private final SteppingClock clock = new SteppingClock();

    private final List<Path> disposedFiles = new ArrayList<>();

    /// Tests opt into pattern renames by replacing this; the default keeps file names as-is.
    private Function<BibEntry, Optional<String>> fileNameGenerator = entry -> Optional.empty();

    private BibDatabaseContext context;
    private DirectoryLibrarySynchronizer synchronizer;

    private void openLibrary() throws IOException {
        PdfEntryFactory pdfEntryFactory = offlinePdfEntryFactory();
        DirectoryLibraryScanner.ScanResult scanResult = new DirectoryLibraryScanner(pdfEntryFactory).scan(root);
        context = scanResult.databaseContext();
        synchronizer = new DirectoryLibrarySynchronizer(context, scanResult.catalog(), pdfEntryFactory,
                disposedFiles::add, fileNameGenerator, Runnable::run, clock);
    }

    /// GROBID off and no identifiers in the fixtures, so no network is touched
    private static PdfEntryFactory offlinePdfEntryFactory() {
        GrobidPreferences noGrobid = mock(GrobidPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(noGrobid.isGrobidEnabled()).thenReturn(false);
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.fieldPreferences().getNonWrappableFields()).thenReturn(FXCollections.emptyObservableList());
        when(importFormatPreferences.grobidPreferences()).thenReturn(noGrobid);
        return new PdfEntryFactory(importFormatPreferences, mock(FilePreferences.class, Answers.RETURNS_DEEP_STUBS),
                DirectoryLibraryScannerTest.authYearPatternPreferences(), mock(CrossRef.class), mock(DoiFetcher.class));
    }

    private List<BibEntry> entries() {
        return context.getDatabase().getEntries();
    }

    @Test
    void externallyCreatedSidecarAddsEntryAndLinksPdf() throws IOException {
        Files.createFile(root.resolve("smith2020.pdf"));
        openLibrary();
        // The bare PDF became a stub during the scan; an appearing sidecar adds its entry
        Path sidecar = root.resolve("smith2020.yml");
        Files.writeString(sidecar, ARTICLE_YAML);

        synchronizer.handleFileCreated(sidecar);

        assertEquals(2, entries().size());
        BibEntry added = entries().getLast();
        assertEquals(Optional.of("smith2020"), added.getCitationKey());
        assertEquals(1, added.getFiles().size());
    }

    @Test
    void externalChangeUpdatesTheSameEntryInstance() throws IOException {
        Path sidecar = root.resolve("smith2020.yml");
        Files.writeString(sidecar, ARTICLE_YAML);
        Files.createFile(root.resolve("smith2020.pdf"));
        openLibrary();
        BibEntry entry = entries().getFirst();

        Files.writeString(sidecar, ARTICLE_YAML.replace("first version", "second version"));
        synchronizer.handleFileChanged(sidecar);

        assertEquals(1, entries().size());
        assertSame(entry, entries().getFirst());
        assertEquals(Optional.of("second version"), entry.getField(StandardField.NOTE));
        assertEquals(1, entry.getFiles().size());
    }

    @Test
    void externallyCreatedMarkdownSidecarAddsEntryWithComments() throws IOException {
        openLibrary();
        Path sidecar = root.resolve("smith2020.md");
        Files.writeString(sidecar, MARKDOWN_SIDECAR);

        synchronizer.handleFileCreated(sidecar);

        assertEquals(1, entries().size());
        BibEntry added = entries().getFirst();
        assertEquals(Optional.of("smith2020"), added.getCitationKey());
        assertEquals(Optional.of("Shared comment text."), added.getField(StandardField.COMMENT));
    }

    @Test
    void externalMarkdownChangeUpdatesCommentOnTheSameEntryInstance() throws IOException {
        Path sidecar = root.resolve("smith2020.md");
        Files.writeString(sidecar, MARKDOWN_SIDECAR);
        openLibrary();
        BibEntry entry = entries().getFirst();

        Files.writeString(sidecar, MARKDOWN_SIDECAR.replace("Shared comment text.", "Updated comment text."));
        synchronizer.handleFileChanged(sidecar);

        assertEquals(1, entries().size());
        assertSame(entry, entries().getFirst());
        assertEquals(Optional.of("Updated comment text."), entry.getField(StandardField.COMMENT));
    }

    @Test
    void externalChangeAddsAndRemovesEntriesOfMultiEntryFile() throws IOException {
        Path file = root.resolve("collection.yml");
        Files.writeString(file, """
                first:
                    type: article
                    title: First
                second:
                    type: article
                    title: Second
                """);
        openLibrary();

        Files.writeString(file, """
                first:
                    type: article
                    title: First
                third:
                    type: article
                    title: Third
                """);
        synchronizer.handleFileChanged(file);

        assertEquals(List.of(Optional.of("first"), Optional.of("third")),
                entries().stream().map(BibEntry::getCitationKey).toList());
    }

    @Test
    void externalDeleteRemovesEntriesOnlyAfterGraceWindow() throws IOException {
        Path sidecar = root.resolve("smith2020.yml");
        Files.writeString(sidecar, ARTICLE_YAML);
        openLibrary();

        Files.delete(sidecar);
        synchronizer.handleFileDeleted(sidecar);
        assertEquals(1, entries().size());

        clock.advance(Duration.ofSeconds(3));
        synchronizer.commitExpiredStagedDeletions();
        assertEquals(0, entries().size());
    }

    @Test
    void renameIsDetectedAsMoveAndPreservesEntryInstance() throws IOException {
        Path oldFile = root.resolve("smith2020.yml");
        Files.writeString(oldFile, ARTICLE_YAML);
        openLibrary();
        BibEntry entry = entries().getFirst();

        Path newFile = root.resolve("renamed.yml");
        Files.move(oldFile, newFile);
        synchronizer.handleFileDeleted(oldFile);
        synchronizer.handleFileCreated(newFile);

        clock.advance(Duration.ofSeconds(3));
        synchronizer.commitExpiredStagedDeletions();

        assertEquals(1, entries().size());
        assertSame(entry, entries().getFirst());
    }

    @Test
    void selfWrittenFileIsNotReimported() throws IOException {
        openLibrary();
        Path sidecar = root.resolve("smith2020.yml");
        byte[] content = ARTICLE_YAML.getBytes(StandardCharsets.UTF_8);
        Files.write(sidecar, content);
        synchronizer.recordWrittenFile(sidecar, content);

        synchronizer.handleFileCreated(sidecar);

        assertEquals(0, entries().size());
    }

    @Test
    void changeToNonHayagrivaContentRemovesItsEntries() throws IOException {
        Path file = root.resolve("smith2020.yml");
        Files.writeString(file, ARTICLE_YAML);
        openLibrary();

        Files.writeString(file, """
                jobs:
                    build:
                        runs-on: ubuntu-latest
                """);
        synchronizer.handleFileChanged(file);

        assertEquals(0, entries().size());
    }

    @Test
    void createdPdfLinksToExistingSidecarEntry() throws IOException {
        Files.writeString(root.resolve("smith2020.yml"), ARTICLE_YAML);
        openLibrary();
        BibEntry entry = entries().getFirst();
        assertEquals(List.of(), entry.getFiles());

        Path pdf = root.resolve("smith2020.pdf");
        Files.createFile(pdf);
        synchronizer.handleFileCreated(pdf);

        assertEquals(1, entry.getFiles().size());
        assertEquals("smith2020.pdf", entry.getFiles().getFirst().getLink());
    }

    @Test
    void createdPdfWithoutSidecarBecomesStub() throws IOException {
        openLibrary();
        Path pdf = root.resolve("interesting-paper.pdf");
        Files.createFile(pdf);

        synchronizer.handleFileCreated(pdf);

        assertEquals(1, entries().size());
        assertEquals(Optional.of("interesting-paper"), entries().getFirst().getField(StandardField.TITLE));
    }

    @Test
    void deletedPdfRemovesStubButKeepsSidecarEntry() throws IOException {
        Files.writeString(root.resolve("smith2020.yml"), ARTICLE_YAML);
        Files.createFile(root.resolve("smith2020.pdf"));
        Files.createFile(root.resolve("loose.pdf"));
        openLibrary();
        assertEquals(2, entries().size());

        Files.delete(root.resolve("loose.pdf"));
        synchronizer.handleFileDeleted(root.resolve("loose.pdf"));
        assertEquals(1, entries().size());

        Files.delete(root.resolve("smith2020.pdf"));
        synchronizer.handleFileDeleted(root.resolve("smith2020.pdf"));
        assertEquals(1, entries().size());
        assertTrue(entries().getFirst().getFiles().isEmpty());
    }

    @Test
    void localEditRewritesSidecarPreservingUnknownContent() throws IOException {
        Path sidecar = root.resolve("smith2020.yml");
        Files.writeString(sidecar, ARTICLE_YAML + "    tongus: 2\n");
        openLibrary();
        BibEntry entry = entries().getFirst();

        entry.setField(StandardField.NOTE, "rewritten by JabRef");
        synchronizer.handleLocalChange(entry);
        synchronizer.flush();

        String written = Files.readString(sidecar);
        assertTrue(written.contains("rewritten by JabRef"));
        assertTrue(written.contains("tongus"));
    }

    @Test
    void firstEditOfStubEntryCreatesMarkdownSidecarNextToPdf() throws IOException {
        Files.createFile(root.resolve("loose.pdf"));
        openLibrary();
        BibEntry stub = entries().getFirst();

        stub.setField(StandardField.AUTHOR, "Doe, John");
        synchronizer.handleLocalChange(stub);
        synchronizer.flush();

        Path sidecar = root.resolve("loose.md");
        assertTrue(Files.exists(sidecar));
        String written = Files.readString(sidecar);
        assertTrue(written.startsWith("---\n"));
        assertTrue(written.contains("Doe, John"));
    }

    @Test
    void newEntryWithoutFileGetsCitationKeyNamedMarkdownSidecar() throws IOException {
        openLibrary();
        BibEntry entry = new BibEntry(org.jabref.model.entry.types.StandardEntryType.Article)
                .withCitationKey("fresh2026")
                .withField(StandardField.TITLE, "Fresh Entry");
        context.getDatabase().insertEntry(entry);

        synchronizer.handleLocalChange(entry);
        synchronizer.flush();

        assertTrue(Files.exists(root.resolve("fresh2026.md")));
    }

    @Test
    void commentEditsLandInTheMarkdownBody() throws IOException {
        Files.createFile(root.resolve("loose.pdf"));
        openLibrary();
        BibEntry stub = entries().getFirst();

        stub.setField(StandardField.COMMENT, "First thoughts.");
        stub.setField(new UserSpecificCommentField("koppor"), "Per-user thoughts.");
        synchronizer.handleLocalChange(stub);
        synchronizer.flush();

        String written = Files.readString(root.resolve("loose.md"));
        assertTrue(written.contains("# Notes\n\nFirst thoughts.\n\n## comment-koppor\n\nPer-user thoughts."),
                () -> "unexpected body: " + written);
        assertFalse(written.contains("comment:"), () -> "comment leaked into the frontmatter: " + written);
    }

    @Test
    void markdownRewriteKeepsForeignBodySections() throws IOException {
        Path sidecar = root.resolve("smith2020.md");
        Files.writeString(sidecar, MARKDOWN_SIDECAR + """

                ## Reading list

                Follow-up papers.
                """);
        openLibrary();
        BibEntry entry = entries().getFirst();

        entry.setField(StandardField.COMMENT, "Updated comment text.");
        synchronizer.handleLocalChange(entry);
        synchronizer.flush();

        String written = Files.readString(sidecar);
        assertTrue(written.contains("Updated comment text."));
        assertTrue(written.contains("## Reading list\n\nFollow-up papers."), () -> "foreign section lost: " + written);
        assertFalse(written.contains("Shared comment text."));
    }

    @Test
    void filteredKeystrokeEventsStillTriggerTheDebouncedWrite() throws IOException, InterruptedException {
        Path sidecar = root.resolve("smith2020.yml");
        Files.writeString(sidecar, ARTICLE_YAML);
        openLibrary();
        BibEntry entry = entries().getFirst();

        // The CoarseChangeFilter marks every keystroke of a same-field burst as filtered; only
        // relying on unfiltered events would strand the burst's tail (it never gets one)
        entry.setField(StandardField.NOTE, "typed letter by letter", EntriesEventSource.LOCAL);
        FieldChangedEvent keystroke = new FieldChangedEvent(entry, StandardField.NOTE, "typed letter by letter", "first version");
        keystroke.setFilteredOut(true);
        synchronizer.listen(keystroke);

        Instant deadline = Instant.now().plusSeconds(10);
        while (!Files.readString(sidecar).contains("typed letter by letter")) {
            if (Instant.now().isAfter(deadline)) {
                throw new AssertionError("debounced write did not happen; file: " + Files.readString(sidecar));
            }
            Thread.sleep(25);
        }
    }

    @Test
    void citationKeyEditRenamesYamlKey() throws IOException {
        Path sidecar = root.resolve("smith2020.yml");
        Files.writeString(sidecar, ARTICLE_YAML);
        openLibrary();
        BibEntry entry = entries().getFirst();

        entry.setCitationKey("smith2021");
        synchronizer.handleLocalChange(entry);
        synchronizer.flush();

        String written = Files.readString(sidecar);
        assertTrue(written.contains("smith2021:"));
        assertFalse(written.contains("smith2020:"));
    }

    @Test
    void deletingLastEntryDisposesSidecarOnly() throws IOException {
        Path sidecar = root.resolve("smith2020.yml");
        Files.writeString(sidecar, ARTICLE_YAML);
        Files.createFile(root.resolve("smith2020.pdf"));
        openLibrary();
        BibEntry entry = entries().getFirst();

        context.getDatabase().removeEntries(List.of(entry));
        synchronizer.handleLocalRemoval(List.of(entry));
        synchronizer.flush();

        assertEquals(List.of(sidecar), disposedFiles);
        assertTrue(Files.exists(root.resolve("smith2020.pdf")));
    }

    @Test
    void deletingOneEntryOfMultiEntryFileRewritesRemainder() throws IOException {
        Path file = root.resolve("collection.yml");
        Files.writeString(file, """
                first:
                    type: article
                    title: First
                second:
                    type: article
                    title: Second
                """);
        openLibrary();
        BibEntry first = entries().getFirst();

        context.getDatabase().removeEntries(List.of(first));
        synchronizer.handleLocalRemoval(List.of(first));
        synchronizer.flush();

        String written = Files.readString(file);
        assertFalse(written.contains("first:"));
        assertTrue(written.contains("second:"));
        assertEquals(List.of(), disposedFiles);
    }

    @Test
    void ownSidecarWritesAreNotReimported() throws IOException {
        Path sidecar = root.resolve("smith2020.yml");
        Files.writeString(sidecar, ARTICLE_YAML);
        openLibrary();
        BibEntry entry = entries().getFirst();

        entry.setField(StandardField.NOTE, "written back");
        synchronizer.handleLocalChange(entry);
        synchronizer.flush();
        synchronizer.handleFileChanged(sidecar);

        assertEquals(1, entries().size());
        assertEquals(Optional.of("written back"), entry.getField(StandardField.NOTE));
    }

    @Test
    void patternRenameMovesSidecarAndPairedPdfTogether() throws IOException {
        fileNameGenerator = entry -> entry.getCitationKey();
        Path sidecar = root.resolve("smith2020.yml");
        Files.writeString(sidecar, ARTICLE_YAML);
        Files.createFile(root.resolve("smith2020.pdf"));
        openLibrary();
        BibEntry entry = entries().getFirst();

        entry.setCitationKey("smith2021");
        synchronizer.handleLocalChange(entry);
        synchronizer.flush();

        assertTrue(Files.exists(root.resolve("smith2021.yml")));
        assertTrue(Files.exists(root.resolve("smith2021.pdf")));
        assertFalse(Files.exists(sidecar));
        assertFalse(Files.exists(root.resolve("smith2020.pdf")));
        assertEquals("smith2021.pdf", entry.getFiles().getFirst().getLink());
        assertTrue(Files.readString(root.resolve("smith2021.yml")).contains("smith2021:"));
    }

    @Test
    void patternRenameMovesMarkdownSidecarAndPairedPdfTogether() throws IOException {
        fileNameGenerator = entry -> entry.getCitationKey();
        Path sidecar = root.resolve("smith2020.md");
        Files.writeString(sidecar, MARKDOWN_SIDECAR);
        Files.createFile(root.resolve("smith2020.pdf"));
        openLibrary();
        BibEntry entry = entries().getFirst();

        entry.setCitationKey("smith2021");
        synchronizer.handleLocalChange(entry);
        synchronizer.flush();

        assertTrue(Files.exists(root.resolve("smith2021.md")));
        assertTrue(Files.exists(root.resolve("smith2021.pdf")));
        assertFalse(Files.exists(sidecar));
        String written = Files.readString(root.resolve("smith2021.md"));
        assertTrue(written.contains("smith2021:"));
        assertTrue(written.contains("Shared comment text."));
    }

    @Test
    void patternRenameSkipsOccupiedTargetNames() throws IOException {
        fileNameGenerator = entry -> Optional.of("taken");
        Path sidecar = root.resolve("smith2020.yml");
        Files.writeString(sidecar, ARTICLE_YAML);
        Files.writeString(root.resolve("taken.yml"), ARTICLE_YAML.replace("smith2020", "taken"));
        openLibrary();
        BibEntry entry = entries().stream()
                                  .filter(candidate -> candidate.getCitationKey().equals(Optional.of("smith2020")))
                                  .findFirst().orElseThrow();

        entry.setField(StandardField.NOTE, "changed");
        synchronizer.handleLocalChange(entry);
        synchronizer.flush();

        assertTrue(Files.exists(sidecar));
        assertTrue(Files.readString(sidecar).contains("changed"));
    }

    @Test
    void multiEntryFilesKeepTheirNameDespitePattern() throws IOException {
        fileNameGenerator = entry -> Optional.of("wrong");
        Path file = root.resolve("collection.yml");
        Files.writeString(file, """
                first:
                    type: article
                    title: First
                second:
                    type: article
                    title: Second
                """);
        openLibrary();
        BibEntry first = entries().getFirst();

        first.setField(StandardField.NOTE, "edited");
        synchronizer.handleLocalChange(first);
        synchronizer.flush();

        assertTrue(Files.exists(file));
        assertFalse(Files.exists(root.resolve("wrong.yml")));
    }
}
