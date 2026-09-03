package org.jabref.logic.directorylibrary;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexString;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

        assertEquals(Optional.of(root), DirectoryLibraryConverter.determineRoot(context, filePreferences));
    }

    @Test
    void determineRootFallsBackToBibDirectory() {
        BibDatabaseContext context = new BibDatabaseContext();
        context.setDatabasePath(root.resolve("library.bib"));

        assertEquals(Optional.of(root), DirectoryLibraryConverter.determineRoot(context, filePreferences));
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

        assertEquals(List.of(
                        Localization.lang("Linked file '%0' of entry '%1' was not found.", "gone.pdf", "missing2020"),
                        Localization.lang("Linked file '%0' of entry '%1' is outside of '%2'.", elsewhere.resolve("outside.pdf").toString(), "outside2020", root.toString())),
                converter.obstacles(contextWith(missing, outside), root, filePreferences));
    }

    @Test
    void preambleAndStringsAreObstacles() {
        BibDatabaseContext context = contextWith();
        context.getDatabase().setPreamble("preamble");
        context.getDatabase().addString(new BibtexString("acm", "Association for Computing Machinery"));

        assertEquals(List.of(
                        Localization.lang("The library contains a preamble, which a folder library cannot represent."),
                        Localization.lang("The library contains BibTeX strings, which a folder library cannot represent.")),
                converter.obstacles(context, root, filePreferences));
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

        assertEquals("""
                ---
                smith2020:
                  type: article
                  title: A Paired Article
                ---
                """, Files.readString(root.resolve("sub/paper.md")));
        List<BibEntry> readBack = new MarkdownSidecar().read(root.resolve("doe2021.md")).getDatabase().getEntries();
        assertEquals(Optional.of("An Unpaired Article"), readBack.getFirst().getField(StandardField.TITLE));
    }

    @Test
    void convertMovesBibIntoRootAsMirrorWithBase() throws IOException {
        Files.createFile(root.resolve("paper.pdf"));
        Path bibFile = root.resolve("library.bib");
        Files.writeString(bibFile, "@Article{smith2020, title = {A Paired Article}, file = {:paper.pdf:PDF}}\n");
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("smith2020")
                .withField(StandardField.TITLE, "A Paired Article")
                .withFiles(List.of(new LinkedFile("", "paper.pdf", "PDF")));

        Path mirror = converter.convert(contextWith(entry), root, filePreferences);

        assertEquals(root.resolve(root.getFileName() + ".bib"), mirror);
        try (Stream<Path> files = Files.list(root)) {
            assertEquals(Stream.of(".jabref", "paper.md", "paper.pdf", root.getFileName() + ".bib").sorted().toList(),
                    files.map(file -> file.getFileName().toString()).sorted().toList());
        }
        assertEquals(Files.readString(mirror), Files.readString(root.resolve(".jabref").resolve("mirror-base.bib")));
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

        try (Stream<Path> files = Files.list(root)) {
            assertEquals(List.of("shared-1.md", "shared.md", "shared.pdf"),
                    files.map(file -> file.getFileName().toString()).sorted().toList());
        }
    }
}
