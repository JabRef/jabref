package org.jabref.logic.directorylibrary;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPatterns;
import org.jabref.logic.directorylibrary.DirectoryLibraryScanner.ScanResult;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fetcher.CrossRef;
import org.jabref.logic.importer.fetcher.DoiFetcher;
import org.jabref.logic.importer.util.GrobidPreferences;
import org.jabref.logic.shared.DatabaseLocation;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.groups.DirectoryStructureGroup;
import org.jabref.model.groups.GroupTreeNode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences.DEFAULT_UNWANTED_CHARACTERS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DirectoryLibraryScannerTest {

    private static final String ARTICLE_YAML = """
            smith2020:
                type: article
                title: A Test Article
                author: Smith, Jane
                date: 2020-10-14
                note: Read twice
            """;

    @TempDir
    Path root;

    private final DirectoryLibraryScanner scanner = new DirectoryLibraryScanner(offlinePdfEntryFactory());

    /// GROBID off and no identifiers in the fixtures, so no network is touched
    private static PdfEntryFactory offlinePdfEntryFactory() {
        GrobidPreferences noGrobid = mock(GrobidPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(noGrobid.isGrobidEnabled()).thenReturn(false);
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.fieldPreferences().getNonWrappableFields()).thenReturn(FXCollections.emptyObservableList());
        when(importFormatPreferences.grobidPreferences()).thenReturn(noGrobid);
        return new PdfEntryFactory(importFormatPreferences, mock(FilePreferences.class, Answers.RETURNS_DEEP_STUBS),
                authYearPatternPreferences(), mock(CrossRef.class), mock(DoiFetcher.class));
    }

    static CitationKeyPatternPreferences authYearPatternPreferences() {
        return new CitationKeyPatternPreferences(
                false,
                false,
                false,
                false,
                CitationKeyPatternPreferences.KeySuffix.SECOND_WITH_A,
                "",
                "",
                DEFAULT_UNWANTED_CHARACTERS,
                GlobalCitationKeyPatterns.fromPattern("[auth][year]"),
                new SimpleObjectProperty<>(','));
    }

    private ScanResult scan() throws IOException {
        return scanner.scan(root);
    }

    private BibEntry singleEntry(ScanResult result) {
        List<BibEntry> entries = result.databaseContext().getDatabase().getEntries();
        assertEquals(1, entries.size());
        return entries.getFirst();
    }

    @Test
    void contextIsDirectoryLibraryRootedAtTheScannedDirectory() throws IOException {
        ScanResult result = scan();

        assertEquals(DatabaseLocation.DIRECTORY, result.databaseContext().getLocation());
        assertEquals(Optional.of(root), result.databaseContext().getDirectoryLibraryRoot());
        assertEquals(Optional.of(root.toAbsolutePath().toString()),
                result.databaseContext().getMetaData().getLibrarySpecificFileDirectory());
        assertEquals(Optional.empty(), result.databaseContext().getDatabasePath());
    }

    @Test
    void sidecarEntryIsImportedAndPairedWithPdf() throws IOException {
        Files.writeString(root.resolve("smith2020.yml"), ARTICLE_YAML);
        Files.createFile(root.resolve("smith2020.pdf"));

        ScanResult result = scan();
        BibEntry entry = singleEntry(result);

        assertEquals(Optional.of("smith2020"), entry.getCitationKey());
        assertEquals(Optional.of("Read twice"), entry.getField(StandardField.NOTE));
        assertEquals(List.of(new LinkedFile("", Path.of("smith2020.pdf"), "PDF")), entry.getFiles());
        assertEquals(Optional.of(new DirectoryLibraryCatalog.EntrySource(root.resolve("smith2020.yml"), "smith2020")),
                result.catalog().sourceOf(entry));
    }

    @Test
    void markdownSidecarIsImportedWithNotesAndPairedPdf() throws IOException {
        Files.writeString(root.resolve("smith2020.md"), """
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
                """);
        Files.createFile(root.resolve("smith2020.pdf"));

        ScanResult result = scan();
        BibEntry entry = singleEntry(result);

        assertEquals(Optional.of("smith2020"), entry.getCitationKey());
        assertEquals(Optional.of("A Test Article"), entry.getField(StandardField.TITLE));
        assertEquals(Optional.of("Shared comment text."), entry.getField(StandardField.COMMENT));
        assertEquals(Optional.of("Per-user comment text."), entry.getField(FieldFactory.parseField("comment-koppor")));
        assertEquals(List.of(new LinkedFile("", Path.of("smith2020.pdf"), "PDF")), entry.getFiles());
        assertEquals(Optional.of(new DirectoryLibraryCatalog.EntrySource(root.resolve("smith2020.md"), "smith2020")),
                result.catalog().sourceOf(entry));
    }

    @Test
    void plainMarkdownFileIsIgnored() throws IOException {
        Files.writeString(root.resolve("README.md"), """
                # A readme

                Just prose.
                """);

        ScanResult result = scan();

        assertEquals(List.of(), result.databaseContext().getDatabase().getEntries());
        assertEquals(List.of(), result.warnings());
    }

    @Test
    void sidecarInNestedDirectoryLinksPdfRelativeToRoot() throws IOException {
        Path subDirectory = Files.createDirectories(root.resolve("conference").resolve("2020"));
        Files.writeString(subDirectory.resolve("smith2020.yaml"), ARTICLE_YAML);
        Files.createFile(subDirectory.resolve("smith2020.pdf"));

        BibEntry entry = singleEntry(scan());

        assertEquals(List.of(new LinkedFile("", Path.of("conference", "2020", "smith2020.pdf"), "PDF")),
                entry.getFiles());
    }

    @Test
    void bareYamlYieldsEntryWithoutFileLink() throws IOException {
        Files.writeString(root.resolve("smith2020.yml"), ARTICLE_YAML);

        BibEntry entry = singleEntry(scan());

        assertEquals(Optional.of("smith2020"), entry.getCitationKey());
        assertEquals(List.of(), entry.getFiles());
    }

    @Test
    void barePdfBecomesStubEntryTitledAfterTheFile() throws IOException {
        Files.createFile(root.resolve("interesting-paper.pdf"));

        ScanResult result = scan();
        BibEntry entry = singleEntry(result);

        assertEquals(StandardEntryType.Misc, entry.getType());
        assertEquals(Optional.of("interesting-paper"), entry.getField(StandardField.TITLE));
        assertEquals(List.of(new LinkedFile("", Path.of("interesting-paper.pdf"), "PDF")), entry.getFiles());
        assertEquals(Optional.empty(), result.catalog().sourceOf(entry));
        assertEquals(List.of(new DirectoryLibraryScanner.PendingPdfImport(entry, root.resolve("interesting-paper.pdf"))),
                result.pendingPdfImports());
    }

    @Test
    void enrichmentExtractsPdfMetadataIntoTheStubEntry() throws IOException, URISyntaxException {
        Path fixture = Path.of(getClass().getResource("/pdfs/PdfContentImporter/Kriha2018.pdf").toURI());
        Files.copy(fixture, root.resolve("kriha2018.pdf"));

        ScanResult result = scan();
        BibEntry entry = singleEntry(result);
        // Scanning is instant: only the stub exists until the enrichment task ran
        assertEquals(Optional.of("kriha2018"), entry.getField(StandardField.TITLE));

        new PdfEnrichmentTask(result.pendingPdfImports(), offlinePdfEntryFactory(),
                result.databaseContext(), Runnable::run).call();

        assertEquals(Optional.of("On How We Can Teach – Exploring New Ways in Professional Software Development for Students"),
                entry.getField(StandardField.TITLE));
        assertEquals(List.of(new LinkedFile("", Path.of("kriha2018.pdf"), "PDF")), entry.getFiles());
    }

    @Test
    void groupsPanelMirrorsTheDirectoryStructure() throws IOException {
        Path subDirectory = Files.createDirectories(root.resolve("conference"));
        Files.writeString(subDirectory.resolve("zygos.yml"), ARTICLE_YAML.replace("smith2020", "zygos"));
        Files.writeString(root.resolve("smith2020.yml"), ARTICLE_YAML);

        ScanResult result = scan();

        GroupTreeNode groupsRoot = result.databaseContext().getMetaData().getGroups().orElseThrow();
        AbstractGroup directoryGroup = groupsRoot.getChildren().getFirst().getGroup();
        assertEquals(root.getFileName().toString(), directoryGroup.getName());
        BibEntry nested = result.databaseContext().getDatabase().getEntries().stream()
                                .filter(entry -> entry.getCitationKey().equals(Optional.of("zygos")))
                                .findFirst().orElseThrow();
        GroupTreeNode conference = ((DirectoryStructureGroup) directoryGroup).createSubgroups(nested).iterator().next();
        assertEquals("conference", conference.getGroup().getName());
        assertTrue(conference.getGroup().contains(nested));
    }

    @Test
    void multiEntryFileYieldsAllEntriesRegisteredToTheSameFile() throws IOException {
        Files.writeString(root.resolve("collection.yml"), """
                first:
                    type: article
                    title: First
                second:
                    type: book
                    title: Second
                """);

        ScanResult result = scan();

        List<BibEntry> entries = result.databaseContext().getDatabase().getEntries();
        assertEquals(2, entries.size());
        assertEquals(2, result.catalog().entryIdsIn(root.resolve("collection.yml")).size());
    }

    @Test
    void nonHayagrivaYamlIsIgnoredSilently() throws IOException {
        Files.writeString(root.resolve("ci-config.yml"), """
                jobs:
                    build:
                        runs-on: ubuntu-latest
                """);

        ScanResult result = scan();

        assertEquals(List.of(), result.databaseContext().getDatabase().getEntries());
        assertEquals(List.of(), result.warnings());
    }

    @Test
    void unparseableHayagrivaFileIsReportedAsWarning() throws IOException {
        Files.writeString(root.resolve("broken.yml"), """
                broken:
                    type: article
                    title: [unclosed
                """);

        ScanResult result = scan();

        assertEquals(List.of(), result.databaseContext().getDatabase().getEntries());
        assertEquals(1, result.warnings().size());
        assertTrue(result.warnings().getFirst().contains("broken.yml"));
    }

    @Test
    void hiddenAndGitDirectoriesAreSkipped() throws IOException {
        Path hidden = Files.createDirectories(root.resolve(".git"));
        Files.writeString(hidden.resolve("smith2020.yml"), ARTICLE_YAML);
        Files.writeString(root.resolve(".hidden.yml"), ARTICLE_YAML);

        ScanResult result = scan();

        assertEquals(List.of(), result.databaseContext().getDatabase().getEntries());
    }

    @Test
    void gitignoredFilesAreSkipped() throws IOException {
        Files.writeString(root.resolve(".gitignore"), "drafts\n");
        Path drafts = Files.createDirectories(root.resolve("drafts"));
        Files.writeString(drafts.resolve("smith2020.yml"), ARTICLE_YAML);
        Files.writeString(root.resolve("smith2020.yml"), ARTICLE_YAML);

        ScanResult result = scan();

        assertEquals(1, result.databaseContext().getDatabase().getEntries().size());
    }

    @Test
    void scanningDoesNotModifyTheDirectory() throws IOException {
        Files.writeString(root.resolve("smith2020.yml"), ARTICLE_YAML);
        Files.createFile(root.resolve("interesting-paper.pdf"));
        List<Path> before;
        try (var paths = Files.walk(root)) {
            before = paths.sorted().toList();
        }

        scan();

        List<Path> after;
        try (var paths = Files.walk(root)) {
            after = paths.sorted().toList();
        }
        assertEquals(before, after);
    }
}
