package org.jabref.logic.directorylibrary;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.directorylibrary.DirectoryLibraryScanner.ScanResult;
import org.jabref.logic.shared.DatabaseLocation;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    private final DirectoryLibraryScanner scanner = new DirectoryLibraryScanner();

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
