package org.jabref.logic.cleanup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumSet;
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
        cleanup = new RenamePdfCleanup(EnumSet.noneOf(RenamePdfCleanup.Option.class), () -> context, filePreferences);
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
        entry.withField(StandardField.FILE, FileFieldWriter.getStringRepresentation(fileField));

        when(filePreferences.getFileNamePattern()).thenReturn("[citationkey]");
        RenamePdfCleanup onlyPdfCleanup = new RenamePdfCleanup(EnumSet.of(RenamePdfCleanup.Option.ONLY_PDF_FILES), () -> context, filePreferences);
        onlyPdfCleanup.cleanup(entry);

        // The non-PDF supplementary file keeps its original (custom) name.
        assertEquals(Optional.of(FileFieldWriter.getStringRepresentation(fileField)), entry.getField(StandardField.FILE));
    }

    @Test
    void cleanupRenameOnlyPdfFilesRenamesPdfFile() throws IOException {
        Path path = testFolder.resolve("Toot.pdf");
        Files.createFile(path);
        LinkedFile fileField = new LinkedFile("", Path.of("Toot.pdf"), "PDF");
        entry.withField(StandardField.FILE, FileFieldWriter.getStringRepresentation(fileField))
             .withField(StandardField.TITLE, "test title");

        when(filePreferences.getFileNamePattern()).thenReturn("[citationkey] - [fulltitle]");
        RenamePdfCleanup onlyPdfCleanup = new RenamePdfCleanup(EnumSet.of(RenamePdfCleanup.Option.ONLY_PDF_FILES), () -> context, filePreferences);
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
        entry.withField(StandardField.FILE, FileFieldWriter.getStringRepresentation(Arrays.asList(relativePdf, relativeNonPdf)))
             .withField(StandardField.TITLE, "test title");

        when(filePreferences.getFileNamePattern()).thenReturn("[citationkey] - [fulltitle]");
        // Both modifiers active: only files that are both relative and PDF are renamed.
        RenamePdfCleanup onlyRelativePdf = new RenamePdfCleanup(EnumSet.of(RenamePdfCleanup.Option.ONLY_RELATIVE_PATHS, RenamePdfCleanup.Option.ONLY_PDF_FILES), () -> context, filePreferences);
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
        entry.withField(StandardField.FILE, FileFieldWriter.getStringRepresentation(fileField));

        when(filePreferences.getFileNamePattern()).thenReturn("[citationkey]");
        RenamePdfCleanup preserveSuffixCleanup = new RenamePdfCleanup(EnumSet.of(RenamePdfCleanup.Option.PRESERVE_CUSTOM_SUFFIX), () -> context, filePreferences);
        preserveSuffixCleanup.cleanup(entry);

        // The "-fig6" suffix is kept instead of being collapsed onto the plain pattern, so nothing changes.
        assertEquals(Optional.of(FileFieldWriter.getStringRepresentation(fileField)), entry.getField(StandardField.FILE));
    }

    @Test
    void cleanupWithoutPreserveCustomSuffixCollapsesSuffix() throws IOException {
        Path path = testFolder.resolve("Toot-fig6.jpg");
        Files.createFile(path);
        LinkedFile fileField = new LinkedFile("", Path.of("Toot-fig6.jpg"), "");
        entry.withField(StandardField.FILE, FileFieldWriter.getStringRepresentation(fileField));

        when(filePreferences.getFileNamePattern()).thenReturn("[citationkey]");
        RenamePdfCleanup defaultCleanup = new RenamePdfCleanup(EnumSet.noneOf(RenamePdfCleanup.Option.class), () -> context, filePreferences);
        defaultCleanup.cleanup(entry);

        // Default (historical) behavior: the file is renamed onto the plain pattern, dropping "-fig6".
        LinkedFile newFileField = new LinkedFile("", Path.of("Toot.jpg"), "");
        assertEquals(Optional.of(FileFieldWriter.getStringRepresentation(newFileField)), entry.getField(StandardField.FILE));
    }

    @Test
    void cleanupPreserveCustomSuffixUsesDetectedOriginalPatternWhenKeyChanged() throws IOException {
        // The files were named with a previous pattern ("ogart"); the citation key (and thus the pattern) changed since.
        Files.createFile(testFolder.resolve("ogart-test1.jpg"));
        Files.createFile(testFolder.resolve("ogart-test2.pdf"));
        LinkedFile file1 = new LinkedFile("", Path.of("ogart-test1.jpg"), "");
        LinkedFile file2 = new LinkedFile("", Path.of("ogart-test2.pdf"), "PDF");
        entry.setFiles(List.of(file1, file2));

        when(filePreferences.getFileNamePattern()).thenReturn("[citationkey]");
        RenamePdfCleanup preserveSuffixCleanup = new RenamePdfCleanup(EnumSet.of(RenamePdfCleanup.Option.PRESERVE_CUSTOM_SUFFIX), () -> context, filePreferences);
        preserveSuffixCleanup.cleanup(entry);

        // The shared "ogart" prefix is detected as the original pattern, so each "-testN" suffix survives onto the new key.
        assertEquals(List.of("Toot-test1.jpg", "Toot-test2.pdf"),
                entry.getFiles().stream().map(LinkedFile::getLink).toList());
    }

    @Test
    void detectOriginalPatternSkipsSingleFileEntries() {
        assertEquals(Optional.empty(),
                RenamePdfCleanup.detectOriginalPattern(List.of(new LinkedFile("", "asdf-fig6.jpg", ""))));
    }

    @Test
    void detectOriginalPatternHandlesEmptyLinksWithoutCrashing() {
        // Empty links yield no usable base name, leaving fewer than two names to compare, so detection is skipped.
        assertEquals(Optional.empty(),
                RenamePdfCleanup.detectOriginalPattern(List.of(
                        new LinkedFile("", "", ""),
                        new LinkedFile("", "Toot-fig6.jpg", ""))));
    }

    @Test
    void cleanupPreserveCustomSuffixHandlesFileNamesWithMultipleDashes() throws IOException {
        // The shared pattern itself contains a dash ("paper-2020"); only the trailing "-figN"/"-tblN" is a custom suffix.
        Files.createFile(testFolder.resolve("paper-2020-fig6.jpg"));
        Files.createFile(testFolder.resolve("paper-2020-tbl1.pdf"));
        LinkedFile file1 = new LinkedFile("", Path.of("paper-2020-fig6.jpg"), "");
        LinkedFile file2 = new LinkedFile("", Path.of("paper-2020-tbl1.pdf"), "PDF");
        entry.setFiles(List.of(file1, file2));

        when(filePreferences.getFileNamePattern()).thenReturn("[citationkey]");
        RenamePdfCleanup preserveSuffixCleanup = new RenamePdfCleanup(EnumSet.of(RenamePdfCleanup.Option.PRESERVE_CUSTOM_SUFFIX), () -> context, filePreferences);
        preserveSuffixCleanup.cleanup(entry);

        // Only the token after the shared "paper-2020" prefix is treated as the custom suffix and survives onto the new key.
        assertEquals(List.of("Toot-fig6.jpg", "Toot-tbl1.pdf"),
                entry.getFiles().stream().map(LinkedFile::getLink).toList());
    }

    @Test
    void cleanupPreserveCustomSuffixLeavesFilesUntouchedWhenDetectedPatternMatchesKey() throws IOException {
        // The detected original pattern ("Toot") already equals the freshly generated pattern, so nothing should change.
        Files.createFile(testFolder.resolve("Toot-fig6.jpg"));
        Files.createFile(testFolder.resolve("Toot-fig8.jpg"));
        LinkedFile file1 = new LinkedFile("", Path.of("Toot-fig6.jpg"), "");
        LinkedFile file2 = new LinkedFile("", Path.of("Toot-fig8.jpg"), "");
        entry.setFiles(List.of(file1, file2));

        when(filePreferences.getFileNamePattern()).thenReturn("[citationkey]");
        RenamePdfCleanup preserveSuffixCleanup = new RenamePdfCleanup(EnumSet.of(RenamePdfCleanup.Option.PRESERVE_CUSTOM_SUFFIX), () -> context, filePreferences);
        preserveSuffixCleanup.cleanup(entry);

        assertEquals(List.of("Toot-fig6.jpg", "Toot-fig8.jpg"),
                entry.getFiles().stream().map(LinkedFile::getLink).toList());
    }

    @Test
    void cleanupPreserveCustomSuffixFallsBackToPlainPatternWhenFilesShareNoPrefix() throws IOException {
        // The files follow no common pattern, so no suffix can be inferred and each file collapses onto the plain pattern.
        Files.createFile(testFolder.resolve("article-fig6.jpg"));
        Files.createFile(testFolder.resolve("report-data.pdf"));
        LinkedFile file1 = new LinkedFile("", Path.of("article-fig6.jpg"), "");
        LinkedFile file2 = new LinkedFile("", Path.of("report-data.pdf"), "PDF");
        entry.setFiles(List.of(file1, file2));

        when(filePreferences.getFileNamePattern()).thenReturn("[citationkey]");
        RenamePdfCleanup preserveSuffixCleanup = new RenamePdfCleanup(EnumSet.of(RenamePdfCleanup.Option.PRESERVE_CUSTOM_SUFFIX), () -> context, filePreferences);
        preserveSuffixCleanup.cleanup(entry);

        assertEquals(List.of("Toot.jpg", "Toot.pdf"),
                entry.getFiles().stream().map(LinkedFile::getLink).toList());
    }

    @Test
    void cleanupPreserveCustomSuffixCombinesWithOnlyPdfFiles() throws IOException {
        // Preserve-suffix and only-PDF are active together: the suffix survives on the PDF, the non-PDF file is untouched.
        Files.createFile(testFolder.resolve("paper-fig6.jpg"));
        Files.createFile(testFolder.resolve("paper-data.pdf"));
        LinkedFile nonPdf = new LinkedFile("", Path.of("paper-fig6.jpg"), "");
        LinkedFile pdf = new LinkedFile("", Path.of("paper-data.pdf"), "PDF");
        entry.setFiles(List.of(nonPdf, pdf));

        when(filePreferences.getFileNamePattern()).thenReturn("[citationkey]");
        RenamePdfCleanup onlyPdfPreserveSuffix = new RenamePdfCleanup(EnumSet.of(RenamePdfCleanup.Option.ONLY_PDF_FILES, RenamePdfCleanup.Option.PRESERVE_CUSTOM_SUFFIX), () -> context, filePreferences);
        onlyPdfPreserveSuffix.cleanup(entry);

        // The shared "paper" prefix is still detected across both files, but only the PDF is renamed (keeping its suffix).
        assertEquals(List.of("paper-fig6.jpg", "Toot-data.pdf"),
                entry.getFiles().stream().map(LinkedFile::getLink).toList());
    }

    @ParameterizedTest(name = "{0} -> \"{1}\"")
    @CsvSource(delimiter = '|', textBlock = """
                ogart-test1;ogart-test2         | ogart
                key-fig6;key                    | key
                asdf-fig6;asdf_extra            | asdf
                ogart-test;ogart-test           | ogart-test
                paper-2020-fig6;paper-2020-tbl1 | paper-2020
                a-b-c1;a-b-d2                   | a-b
                abc;xyz                         |
            """)
    void commonLeadingTokenPrefixDetectsSharedPattern(String namesJoinedBySemicolon, String expected) {
        List<String> baseNames = Arrays.stream(namesJoinedBySemicolon.split(";")).toList();
        assertEquals(expected == null ? "" : expected, RenamePdfCleanup.commonLeadingTokenPrefix(baseNames));
    }
}
