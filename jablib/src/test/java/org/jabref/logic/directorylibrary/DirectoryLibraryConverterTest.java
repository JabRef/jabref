package org.jabref.logic.directorylibrary;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.FilePreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/// [utest->req~directory-library.convert~1]
class DirectoryLibraryConverterTest {

    @TempDir
    Path root;

    private final FilePreferences filePreferences = mock(FilePreferences.class, Answers.RETURNS_DEEP_STUBS);
    private final DirectoryLibraryConverter converter = new DirectoryLibraryConverter();

    private BibDatabaseContext contextWith(BibEntry... entries) {
        BibDatabaseContext context = new BibDatabaseContext(new BibDatabase(List.of(entries)));
        context.setDatabasePath(root.resolve("library.bib"));
        context.getMetaData().setLibrarySpecificFileDirectory(root.toString());
        return context;
    }

    @Test
    void determineRootPrefersLibrarySpecificFileDirectory(@TempDir Path elsewhere) {
        BibDatabaseContext context = new BibDatabaseContext();
        context.setDatabasePath(elsewhere.resolve("library.bib"));
        context.getMetaData().setLibrarySpecificFileDirectory(root.toString());

        assertEquals(Optional.of(root), DirectoryLibraryConverter.determineRoot(context));
    }

    @Test
    void determineRootFallsBackToBibDirectory() {
        BibDatabaseContext context = new BibDatabaseContext();
        context.setDatabasePath(root.resolve("library.bib"));

        assertEquals(Optional.of(root), DirectoryLibraryConverter.determineRoot(context));
    }

    @Test
    void noObstaclesWhenAllFilesLiveUnderRoot() throws IOException {
        Files.createDirectories(root.resolve("sub"));
        Files.createFile(root.resolve("sub/paper.pdf"));
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("smith2020")
                .withFiles(List.of(new LinkedFile("", "sub/paper.pdf", "PDF")));

        assertEquals(List.of(), converter.obstacles(contextWith(entry), root, filePreferences));
    }

    @Test
    void missingAndOutsideFilesAreObstacles(@TempDir Path elsewhere) throws IOException {
        Files.createFile(elsewhere.resolve("outside.pdf"));
        BibEntry missing = new BibEntry(StandardEntryType.Article)
                .withCitationKey("missing2020")
                .withFiles(List.of(new LinkedFile("", "gone.pdf", "PDF")));
        BibEntry outside = new BibEntry(StandardEntryType.Article)
                .withCitationKey("outside2020")
                .withFiles(List.of(new LinkedFile("", elsewhere.resolve("outside.pdf").toString(), "PDF")));

        List<String> obstacles = converter.obstacles(contextWith(missing, outside), root, filePreferences);

        assertEquals(2, obstacles.size());
        assertTrue(obstacles.getFirst().contains("gone.pdf"));
        assertTrue(obstacles.getLast().contains("outside.pdf"));
    }

    @Test
    void preambleAndStringsAreObstacles() {
        BibDatabaseContext context = contextWith();
        context.getDatabase().setPreamble("preamble");

        assertEquals(1, converter.obstacles(context, root, filePreferences).size());
    }

    @Test
    void sidecarsAreWrittenNextToLinkedFilesAndReadBack() throws IOException {
        Files.createDirectories(root.resolve("sub"));
        Files.createFile(root.resolve("sub/paper.pdf"));
        BibEntry paired = new BibEntry(StandardEntryType.Article)
                .withCitationKey("smith2020")
                .withField(StandardField.TITLE, "A Paired Article")
                .withFiles(List.of(new LinkedFile("", "sub/paper.pdf", "PDF")));
        BibEntry unpaired = new BibEntry(StandardEntryType.Article)
                .withCitationKey("doe2021")
                .withField(StandardField.TITLE, "An Unpaired Article");

        converter.writeSidecars(contextWith(paired, unpaired), root, filePreferences);

        Path pairedSidecar = root.resolve("sub/paper.md");
        assertTrue(Files.readString(pairedSidecar).contains("A Paired Article"));
        List<BibEntry> readBack = new MarkdownSidecar().read(root.resolve("doe2021.md")).getDatabase().getEntries();
        assertEquals(Optional.of("An Unpaired Article"), readBack.getFirst().getField(StandardField.TITLE));
    }

    @Test
    void entriesSharingAFileGetUniquifiedSidecarNames() throws IOException {
        Files.createFile(root.resolve("shared.pdf"));
        BibEntry first = new BibEntry(StandardEntryType.Article)
                .withCitationKey("first2020")
                .withFiles(List.of(new LinkedFile("", "shared.pdf", "PDF")));
        BibEntry second = new BibEntry(StandardEntryType.Article)
                .withCitationKey("second2020")
                .withFiles(List.of(new LinkedFile("", "shared.pdf", "PDF")));

        converter.writeSidecars(contextWith(first, second), root, filePreferences);

        assertTrue(Files.exists(root.resolve("shared.md")));
        assertTrue(Files.exists(root.resolve("shared-1.md")));
    }
}
