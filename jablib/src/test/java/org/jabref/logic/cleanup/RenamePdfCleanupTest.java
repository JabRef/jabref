package org.jabref.logic.cleanup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.bibtex.FileFieldWriter;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.metadata.MetaData;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RenamePdfCleanupTest {

    private BibEntry entry;

    private FilePreferences filePreferences;
    private RenamePdfCleanup cleanup;
    private BibDatabaseContext context;

    // Ensure that the folder stays the same for all tests. By default @TempDir creates a new folder for each usage
    private Path testFolder;

    @BeforeEach
    void setUp(@TempDir Path testFolder) {
        this.testFolder = testFolder;
        Path path = testFolder.resolve("test.bib");
        MetaData metaData = new MetaData();
        context = new BibDatabaseContext(new BibDatabase(), metaData);
        context.setDatabasePath(path);

        entry = new BibEntry();
        entry.setCitationKey("Toot");

        filePreferences = mock(FilePreferences.class);
        when(filePreferences.shouldStoreFilesRelativeToBibFile()).thenReturn(true); // Set Biblocation as Primary Directory, otherwise the tmp folders won't be cleaned up correctly
        cleanup = new RenamePdfCleanup(false, () -> context, filePreferences);
    }

    /// Test for #466
    @Test
    void cleanupRenamePdfRenamesFileEvenIfOnlyDifferenceIsCase() throws IOException {
        Path path = testFolder.resolve("toot.tmp");
        Files.createFile(path);

        LinkedFile fileField = new LinkedFile("", path.toAbsolutePath(), "");
        entry.setField(StandardField.FILE, FileFieldWriter.getStringRepresentation(fileField));

        when(filePreferences.getFileNamePattern()).thenReturn("[citationkey]");
        cleanup.cleanup(entry);

        /* This special handling is for Windows. Window file system is case-insensitive,
        so renaming "toot.tmp" to "Toot.tmp" would overwrite the original file.
        Therefore, the file is renamed to `Toot (1).tmp` in Windows. Prior to testing,
        delete `AppData/Local/Temp/junit-*` folders, as the test doesn't clean up the
        renamed file and could potentially interfere with subsequent test runs. */
        assertTrue(Pattern.matches("^:Toot(?:\\s+\\(\\d+\\))?\\.tmp:$",
                entry.getField(StandardField.FILE).get()));
    }

    @Test
    void cleanupRenamePdfRenamesWithMultipleFiles() throws IOException {
        Path path = testFolder.resolve("Toot.tmp");
        Files.createFile(path);

        entry.setField(StandardField.TITLE, "test title");
        entry.setField(StandardField.FILE, FileFieldWriter.getStringRepresentation(
                Arrays.asList(
                        new LinkedFile("", Path.of(""), ""),
                        new LinkedFile("", path.toAbsolutePath(), ""),
                        new LinkedFile("", Path.of(""), ""))));

        when(filePreferences.getFileNamePattern()).thenReturn("[citationkey] - [fulltitle]");
        cleanup.cleanup(entry);

        assertEquals(Optional.of(FileFieldWriter.getStringRepresentation(
                        Arrays.asList(
                                new LinkedFile("", Path.of(""), ""),
                                new LinkedFile("", Path.of("Toot - test title.tmp"), ""),
                                new LinkedFile("", Path.of(""), "")))),
                entry.getField(StandardField.FILE));
    }

    @Test
    void cleanupRenamePdfRenamesFileStartingWithCitationKey() throws IOException {
        Path path = testFolder.resolve("Toot.tmp");
        Files.createFile(path);

        LinkedFile fileField = new LinkedFile("", path.toAbsolutePath(), "");
        entry.setField(StandardField.FILE, FileFieldWriter.getStringRepresentation(fileField));
        entry.setField(StandardField.TITLE, "test title");

        when(filePreferences.getFileNamePattern()).thenReturn("[citationkey] - [fulltitle]");
        cleanup.cleanup(entry);

        LinkedFile newFileField = new LinkedFile("", Path.of("Toot - test title.tmp"), "");
        assertEquals(Optional.of(FileFieldWriter.getStringRepresentation(newFileField)), entry.getField(StandardField.FILE));
    }

    @Test
    void cleanupRenamePdfRenamesFileInSameFolder() throws IOException {
        Path path = testFolder.resolve("Toot.pdf");
        Files.createFile(path);
        LinkedFile fileField = new LinkedFile("", Path.of("Toot.pdf"), "PDF");
        entry.setField(StandardField.FILE, FileFieldWriter.getStringRepresentation(fileField));
        entry.setField(StandardField.TITLE, "test title");

        when(filePreferences.getFileNamePattern()).thenReturn("[citationkey] - [fulltitle]");
        cleanup.cleanup(entry);

        LinkedFile newFileField = new LinkedFile("", Path.of("Toot - test title.pdf"), "PDF");
        assertEquals(Optional.of(FileFieldWriter.getStringRepresentation(newFileField)), entry.getField(StandardField.FILE));
    }

    @Test
    void cleanupRenamePdfUsesMutationSchedulerForEntryUpdate() throws IOException {
        Path path = testFolder.resolve("Toot.pdf");
        Files.createFile(path);
        entry.setField(StandardField.FILE, FileFieldWriter.getStringRepresentation(new LinkedFile("", path.toAbsolutePath(), "PDF")));
        entry.setField(StandardField.TITLE, "test title");

        when(filePreferences.getFileNamePattern()).thenReturn("[citationkey] - [fulltitle]");

        AtomicBoolean schedulerUsed = new AtomicBoolean(false);
        cleanup.cleanup(entry, mutation -> {
            schedulerUsed.set(true);
            mutation.run();
        });

        assertTrue(schedulerUsed.get());
    }

    @Test
    void cleanupRenameOnlyPdfFilesSkipsNonPdfFile() throws IOException {
        Path path = testFolder.resolve("Toot-fig6.jpg");
        Files.createFile(path);
        LinkedFile fileField = new LinkedFile("", Path.of("Toot-fig6.jpg"), "");
        entry.setField(StandardField.FILE, FileFieldWriter.getStringRepresentation(fileField));

        when(filePreferences.getFileNamePattern()).thenReturn("[citationkey]");
        RenamePdfCleanup onlyPdfCleanup = new RenamePdfCleanup(false, true, () -> context, filePreferences);
        onlyPdfCleanup.cleanup(entry);

        // The non-PDF supplementary file keeps its original (custom) name.
        assertEquals(Optional.of(FileFieldWriter.getStringRepresentation(fileField)), entry.getField(StandardField.FILE));
    }

    @Test
    void cleanupRenameOnlyPdfFilesRenamesPdfFile() throws IOException {
        Path path = testFolder.resolve("Toot.pdf");
        Files.createFile(path);
        LinkedFile fileField = new LinkedFile("", Path.of("Toot.pdf"), "PDF");
        entry.setField(StandardField.FILE, FileFieldWriter.getStringRepresentation(fileField));
        entry.setField(StandardField.TITLE, "test title");

        when(filePreferences.getFileNamePattern()).thenReturn("[citationkey] - [fulltitle]");
        RenamePdfCleanup onlyPdfCleanup = new RenamePdfCleanup(false, true, () -> context, filePreferences);
        onlyPdfCleanup.cleanup(entry);

        LinkedFile newFileField = new LinkedFile("", Path.of("Toot - test title.pdf"), "PDF");
        assertEquals(Optional.of(FileFieldWriter.getStringRepresentation(newFileField)), entry.getField(StandardField.FILE));
    }

    @Test
    void cleanupRenameOnlyRelativeAndOnlyPdfCombineAsIntersection() throws IOException {
        Files.createFile(testFolder.resolve("Toot.pdf"));
        Files.createFile(testFolder.resolve("Toot.tmp"));
        LinkedFile relativePdf = new LinkedFile("", Path.of("Toot.pdf"), "PDF");
        LinkedFile relativeNonPdf = new LinkedFile("", Path.of("Toot.tmp"), "");
        entry.setField(StandardField.FILE, FileFieldWriter.getStringRepresentation(Arrays.asList(relativePdf, relativeNonPdf)));
        entry.setField(StandardField.TITLE, "test title");

        when(filePreferences.getFileNamePattern()).thenReturn("[citationkey] - [fulltitle]");
        // Both modifiers active: only files that are both relative and PDF are renamed.
        RenamePdfCleanup onlyRelativePdf = new RenamePdfCleanup(true, true, () -> context, filePreferences);
        onlyRelativePdf.cleanup(entry);

        // The relative PDF is renamed; the non-PDF file (although relative) is left untouched.
        assertEquals(Optional.of(FileFieldWriter.getStringRepresentation(Arrays.asList(
                        new LinkedFile("", Path.of("Toot - test title.pdf"), "PDF"),
                        new LinkedFile("", Path.of("Toot.tmp"), "")))),
                entry.getField(StandardField.FILE));
    }

    @Test
    void cleanupPreserveCustomSuffixKeepsSuffix() throws IOException {
        Path path = testFolder.resolve("Toot-fig6.jpg");
        Files.createFile(path);
        LinkedFile fileField = new LinkedFile("", Path.of("Toot-fig6.jpg"), "");
        entry.setField(StandardField.FILE, FileFieldWriter.getStringRepresentation(fileField));

        when(filePreferences.getFileNamePattern()).thenReturn("[citationkey]");
        RenamePdfCleanup preserveSuffixCleanup = new RenamePdfCleanup(false, false, true, () -> context, filePreferences);
        preserveSuffixCleanup.cleanup(entry);

        // The "-fig6" suffix is kept instead of being collapsed onto the plain pattern, so nothing changes.
        assertEquals(Optional.of(FileFieldWriter.getStringRepresentation(fileField)), entry.getField(StandardField.FILE));
    }

    @Test
    void cleanupWithoutPreserveCustomSuffixCollapsesSuffix() throws IOException {
        Path path = testFolder.resolve("Toot-fig6.jpg");
        Files.createFile(path);
        LinkedFile fileField = new LinkedFile("", Path.of("Toot-fig6.jpg"), "");
        entry.setField(StandardField.FILE, FileFieldWriter.getStringRepresentation(fileField));

        when(filePreferences.getFileNamePattern()).thenReturn("[citationkey]");
        RenamePdfCleanup defaultCleanup = new RenamePdfCleanup(false, false, false, () -> context, filePreferences);
        defaultCleanup.cleanup(entry);

        // Default (historical) behavior: the file is renamed onto the plain pattern, dropping "-fig6".
        LinkedFile newFileField = new LinkedFile("", Path.of("Toot.jpg"), "");
        assertEquals(Optional.of(FileFieldWriter.getStringRepresentation(newFileField)), entry.getField(StandardField.FILE));
    }

    @Test
    void cleanupPreserveCustomSuffixUsesDetectedOriginalPatternWhenKeyChanged() throws IOException {
        // The files were named with a previous pattern ("ogart"); the citation key (and thus the pattern) changed since.
        Files.createFile(testFolder.resolve("ogart-teste1.jpg"));
        Files.createFile(testFolder.resolve("ogart-teste2.pdf"));
        LinkedFile file1 = new LinkedFile("", Path.of("ogart-teste1.jpg"), "");
        LinkedFile file2 = new LinkedFile("", Path.of("ogart-teste2.pdf"), "PDF");
        entry.setFiles(List.of(file1, file2));

        when(filePreferences.getFileNamePattern()).thenReturn("[citationkey]");
        RenamePdfCleanup preserveSuffixCleanup = new RenamePdfCleanup(false, false, true, () -> context, filePreferences);
        preserveSuffixCleanup.cleanup(entry);

        // The shared "ogart" prefix is detected as the original pattern, so each "-testeN" suffix survives onto the new key.
        assertEquals(List.of("Toot-teste1.jpg", "Toot-teste2.pdf"),
                entry.getFiles().stream().map(LinkedFile::getLink).toList());
    }

    @Test
    void detectOriginalPatternSkipsSingleFileEntries() {
        assertEquals(Optional.empty(),
                RenamePdfCleanup.detectOriginalPattern(List.of(new LinkedFile("", "asdf-fig6.jpg", ""))));
    }

    @ParameterizedTest(name = "{0} -> \"{1}\"")
    @CsvSource(delimiter = '|', textBlock = """
                ogart-teste1;ogart-teste2 | ogart
                key-fig6;key              | key
                asdf-fig6;asdf_extra      | asdf
                ogart-teste;ogart-teste   | ogart-teste
                abc;xyz                   |
            """)
    void commonLeadingTokenPrefixDetectsSharedPattern(String namesJoinedBySemicolon, String expected) {
        List<String> baseNames = Arrays.stream(namesJoinedBySemicolon.split(";")).toList();
        assertEquals(expected == null ? "" : expected, RenamePdfCleanup.commonLeadingTokenPrefix(baseNames));
    }
}
