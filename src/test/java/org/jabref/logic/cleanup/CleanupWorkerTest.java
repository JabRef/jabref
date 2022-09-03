package org.jabref.logic.cleanup;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.bibtex.FileFieldWriter;
import org.jabref.logic.formatter.bibtexfields.HtmlToLatexFormatter;
import org.jabref.logic.formatter.bibtexfields.LatexCleanupFormatter;
import org.jabref.logic.formatter.bibtexfields.NormalizeDateFormatter;
import org.jabref.logic.formatter.bibtexfields.NormalizeMonthFormatter;
import org.jabref.logic.formatter.bibtexfields.NormalizePagesFormatter;
import org.jabref.logic.formatter.bibtexfields.UnitsToLatexFormatter;
import org.jabref.logic.formatter.casechanger.ProtectTermsFormatter;
import org.jabref.logic.preferences.TimestampPreferences;
import org.jabref.logic.protectedterms.ProtectedTermsLoader;
import org.jabref.logic.protectedterms.ProtectedTermsPreferences;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.metadata.MetaData;
import org.jabref.preferences.FilePreferences;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CleanupWorkerTest {

    private final CleanupPreferences emptyPreset = new CleanupPreferences(EnumSet.noneOf(CleanupPreferences.CleanupStep.class));
    private CleanupWorker worker;

    // Ensure that the folder stays the same for all tests. By default @TempDir creates a new folder for each usage
    private Path bibFolder;

    @BeforeEach
    void setUp(@TempDir Path bibFolder) throws IOException {

        this.bibFolder = bibFolder;
        Path path = bibFolder.resolve("ARandomlyNamedFolder");
        Files.createDirectory(path);
        File pdfFolder = path.toFile();

        MetaData metaData = new MetaData();
        metaData.setDefaultFileDirectory(pdfFolder.getAbsolutePath());
        BibDatabaseContext context = new BibDatabaseContext(new BibDatabase(), metaData);
        Files.createFile(bibFolder.resolve("test.bib"));
        context.setDatabasePath(bibFolder.resolve("test.bib"));

        FilePreferences fileDirPrefs = mock(FilePreferences.class, Answers.RETURNS_SMART_NULLS);
        // Search and store files relative to bib file overwrites all other dirs
        when(fileDirPrefs.shouldStoreFilesRelativeToBibFile()).thenReturn(true);

        worker = new CleanupWorker(context, fileDirPrefs, mock(TimestampPreferences.class));
    }

    @Test
    void cleanupWithNullPresetThrowsException() {
        assertThrows(NullPointerException.class, () -> worker.cleanup(null, new BibEntry()));
    }

    @Test
    void cleanupNullEntryThrowsException() {
        assertThrows(NullPointerException.class, () -> worker.cleanup(emptyPreset, null));
    }

    @Test
    void cleanupDoesNothingByDefault(@TempDir Path bibFolder) throws IOException {
        BibEntry entry = new BibEntry();
        entry.setCitationKey("Toot");
        entry.setField(StandardField.PDF, "aPdfFile");
        entry.setField(new UnknownField("some"), "1st");
        entry.setField(StandardField.DOI, "http://dx.doi.org/10.1016/0001-8708(80)90035-3");
        entry.setField(StandardField.MONTH, "01");
        entry.setField(StandardField.PAGES, "1-2");
        entry.setField(StandardField.DATE, "01/1999");
        entry.setField(StandardField.PDF, "aPdfFile");
        entry.setField(StandardField.ISSN, "aPsFile");
        entry.setField(StandardField.FILE, "link::");
        entry.setField(StandardField.JOURNAL, "test");
        entry.setField(StandardField.TITLE, "<b>hallo</b> units 1 A case AlGaAs and latex $\\alpha$$\\beta$");
        entry.setField(StandardField.ABSTRACT, "Réflexions");
        Path path = bibFolder.resolve("ARandomlyNamedFile");
        Files.createFile(path);
        LinkedFile fileField = new LinkedFile("", path.toAbsolutePath(), "");
        entry.setField(StandardField.FILE, FileFieldWriter.getStringRepresentation(fileField));

        List<FieldChange> changes = worker.cleanup(emptyPreset, entry);
        assertEquals(Collections.emptyList(), changes);
    }

    @Test
    void upgradeExternalLinksMoveFromPdfToFile() {
        CleanupPreferences preset = new CleanupPreferences(CleanupPreferences.CleanupStep.CLEAN_UP_UPGRADE_EXTERNAL_LINKS);
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.PDF, "aPdfFile");

        worker.cleanup(preset, entry);
        assertEquals(Optional.empty(), entry.getField(StandardField.PDF));
        assertEquals(Optional.of("aPdfFile:aPdfFile:PDF"), entry.getField(StandardField.FILE));
    }

    @Test
    void upgradeExternalLinksMoveFromPsToFile() {
        CleanupPreferences preset = new CleanupPreferences(CleanupPreferences.CleanupStep.CLEAN_UP_UPGRADE_EXTERNAL_LINKS);
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.PS, "aPsFile");

        worker.cleanup(preset, entry);
        assertEquals(Optional.empty(), entry.getField(StandardField.PDF));
        assertEquals(Optional.of("aPsFile:aPsFile:PostScript"), entry.getField(StandardField.FILE));
    }

    @Test
    void cleanupDoiRemovesLeadingHttp() {
        CleanupPreferences preset = new CleanupPreferences(CleanupPreferences.CleanupStep.CLEAN_UP_DOI);
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.DOI, "http://dx.doi.org/10.1016/0001-8708(80)90035-3");

        worker.cleanup(preset, entry);
        assertEquals(Optional.of("10.1016/0001-8708(80)90035-3"), entry.getField(StandardField.DOI));
    }

    @Test
    void cleanupDoiReturnsChanges() {
        CleanupPreferences preset = new CleanupPreferences(CleanupPreferences.CleanupStep.CLEAN_UP_DOI);
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.DOI, "http://dx.doi.org/10.1016/0001-8708(80)90035-3");

        List<FieldChange> changes = worker.cleanup(preset, entry);

        FieldChange expectedChange = new FieldChange(entry, StandardField.DOI, "http://dx.doi.org/10.1016/0001-8708(80)90035-3", "10.1016/0001-8708(80)90035-3");
        assertEquals(Collections.singletonList(expectedChange), changes);
    }

    @Test
    void cleanupDoiFindsDoiInURLFieldAndMoveItToDOIField() {
        CleanupPreferences preset = new CleanupPreferences(CleanupPreferences.CleanupStep.CLEAN_UP_DOI);
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.URL, "http://dx.doi.org/10.1016/0001-8708(80)90035-3");

        worker.cleanup(preset, entry);
        assertEquals(Optional.of("10.1016/0001-8708(80)90035-3"), entry.getField(StandardField.DOI));
        assertEquals(Optional.empty(), entry.getField(StandardField.URL));
    }

    @Test
    void cleanupDoiReturnsChangeWhenDoiInURLField() {
        CleanupPreferences preset = new CleanupPreferences(CleanupPreferences.CleanupStep.CLEAN_UP_DOI);
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.URL, "http://dx.doi.org/10.1016/0001-8708(80)90035-3");

        List<FieldChange> changes = worker.cleanup(preset, entry);
        List<FieldChange> changeList = new ArrayList<>();
        changeList.add(new FieldChange(entry, StandardField.DOI, null, "10.1016/0001-8708(80)90035-3"));
        changeList.add(new FieldChange(entry, StandardField.URL, "http://dx.doi.org/10.1016/0001-8708(80)90035-3", null));
        assertEquals(changeList, changes);
    }

    @Test
    void cleanupMonthChangesNumberToBibtex() {
        CleanupPreferences preset = new CleanupPreferences(new FieldFormatterCleanups(true,
                Collections.singletonList(new FieldFormatterCleanup(StandardField.MONTH, new NormalizeMonthFormatter()))));
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.MONTH, "01");

        worker.cleanup(preset, entry);
        assertEquals(Optional.of("#jan#"), entry.getField(StandardField.MONTH));
    }

    @Test
    void cleanupPageNumbersConvertsSingleDashToDouble() {
        CleanupPreferences preset = new CleanupPreferences(new FieldFormatterCleanups(true,
                Collections.singletonList(new FieldFormatterCleanup(StandardField.PAGES, new NormalizePagesFormatter()))));
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.PAGES, "1-2");

        worker.cleanup(preset, entry);
        assertEquals(Optional.of("1--2"), entry.getField(StandardField.PAGES));
    }

    @Test
    void cleanupDatesConvertsToCorrectFormat() {
        CleanupPreferences preset = new CleanupPreferences(new FieldFormatterCleanups(true,
                Collections.singletonList(new FieldFormatterCleanup(StandardField.DATE, new NormalizeDateFormatter()))));
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.DATE, "01/1999");

        worker.cleanup(preset, entry);
        assertEquals(Optional.of("1999-01"), entry.getField(StandardField.DATE));
    }

    @Test
    void cleanupFixFileLinksMovesSingleDescriptionToLink() {
        CleanupPreferences preset = new CleanupPreferences(CleanupPreferences.CleanupStep.FIX_FILE_LINKS);
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.FILE, "link::");

        worker.cleanup(preset, entry);
        assertEquals(Optional.of(":link:"), entry.getField(StandardField.FILE));
    }

    @Test
    void cleanupMoveFilesMovesFileFromSubfolder(@TempDir Path bibFolder) throws IOException {
        CleanupPreferences preset = new CleanupPreferences(CleanupPreferences.CleanupStep.MOVE_PDF);

        Path path = bibFolder.resolve("AnotherRandomlyNamedFolder");
        Files.createDirectory(path);
        Path tempFile = Files.createFile(path.resolve("test.pdf"));
        BibEntry entry = new BibEntry();
        LinkedFile fileField = new LinkedFile("", tempFile.toAbsolutePath(), "");
        entry.setField(StandardField.FILE, FileFieldWriter.getStringRepresentation(fileField));

        worker.cleanup(preset, entry);
        LinkedFile newFileField = new LinkedFile("", tempFile.getFileName(), "");
        assertEquals(Optional.of(FileFieldWriter.getStringRepresentation(newFileField)), entry.getField(StandardField.FILE));
    }

    @Test
    void cleanupRelativePathsConvertAbsoluteToRelativePath() throws IOException {
        CleanupPreferences preset = new CleanupPreferences(CleanupPreferences.CleanupStep.MAKE_PATHS_RELATIVE);

        Path path = bibFolder.resolve("AnotherRandomlyNamedFile");
        Files.createFile(path);
        BibEntry entry = new BibEntry();
        LinkedFile fileField = new LinkedFile("", path.toAbsolutePath(), "");
        entry.setField(StandardField.FILE, FileFieldWriter.getStringRepresentation(fileField));

        worker.cleanup(preset, entry);
        LinkedFile newFileField = new LinkedFile("", path.getFileName(), "");
        assertEquals(Optional.of(FileFieldWriter.getStringRepresentation(newFileField)), entry.getField(StandardField.FILE));
    }

    @Test
    void cleanupRenamePdfRenamesRelativeFile() throws IOException {
        CleanupPreferences preset = new CleanupPreferences(CleanupPreferences.CleanupStep.RENAME_PDF);

        Path path = bibFolder.resolve("AnotherRandomlyNamedFile.tmp");
        Files.createFile(path);
        BibEntry entry = new BibEntry();
        entry.setCitationKey("Toot");
        LinkedFile fileField = new LinkedFile("", path.toAbsolutePath(), "");
        entry.setField(StandardField.FILE, FileFieldWriter.getStringRepresentation(fileField));

        worker.cleanup(preset, entry);
        LinkedFile newFileField = new LinkedFile("", Path.of("Toot.tmp"), "");
        assertEquals(Optional.of(FileFieldWriter.getStringRepresentation(newFileField)), entry.getField(StandardField.FILE));
    }

    @Test
    void cleanupHtmlToLatexConvertsEpsilonToLatex() {
        CleanupPreferences preset = new CleanupPreferences(new FieldFormatterCleanups(true,
                Collections.singletonList(new FieldFormatterCleanup(StandardField.TITLE, new HtmlToLatexFormatter()))));
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.TITLE, "&Epsilon;");

        worker.cleanup(preset, entry);
        assertEquals(Optional.of("{{$\\Epsilon$}}"), entry.getField(StandardField.TITLE));
    }

    @Test
    void cleanupUnitsConvertsOneAmpereToLatex() {
        CleanupPreferences preset = new CleanupPreferences(new FieldFormatterCleanups(true,
                Collections.singletonList(new FieldFormatterCleanup(StandardField.TITLE, new UnitsToLatexFormatter()))));
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.TITLE, "1 A");

        worker.cleanup(preset, entry);
        assertEquals(Optional.of("1~{A}"), entry.getField(StandardField.TITLE));
    }

    @Test
    void cleanupCasesAddsBracketAroundAluminiumGalliumArsenid() {
        ProtectedTermsLoader protectedTermsLoader = new ProtectedTermsLoader(
                new ProtectedTermsPreferences(ProtectedTermsLoader.getInternalLists(), Collections.emptyList(),
                        Collections.emptyList(), Collections.emptyList()));
        assertNotEquals(Collections.emptyList(), protectedTermsLoader.getProtectedTerms());
        CleanupPreferences preset = new CleanupPreferences(new FieldFormatterCleanups(true, Collections
                .singletonList(new FieldFormatterCleanup(StandardField.TITLE, new ProtectTermsFormatter(protectedTermsLoader)))));
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.TITLE, "AlGaAs");

        worker.cleanup(preset, entry);
        assertEquals(Optional.of("{AlGaAs}"), entry.getField(StandardField.TITLE));
    }

    @Test
    void cleanupLatexMergesTwoLatexMathEnvironments() {
        CleanupPreferences preset = new CleanupPreferences(new FieldFormatterCleanups(true,
                Collections.singletonList(new FieldFormatterCleanup(StandardField.TITLE, new LatexCleanupFormatter()))));
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.TITLE, "$\\alpha$$\\beta$");

        worker.cleanup(preset, entry);
        assertEquals(Optional.of("$\\alpha\\beta$"), entry.getField(StandardField.TITLE));
    }

    @Test
    void convertToBiblatexMovesAddressToLocation() {
        CleanupPreferences preset = new CleanupPreferences(CleanupPreferences.CleanupStep.CONVERT_TO_BIBLATEX);
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.ADDRESS, "test");

        worker.cleanup(preset, entry);
        assertEquals(Optional.empty(), entry.getField(StandardField.ADDRESS));
        assertEquals(Optional.of("test"), entry.getField(StandardField.LOCATION));
    }

    @Test
    void convertToBiblatexMovesJournalToJournalTitle() {
        CleanupPreferences preset = new CleanupPreferences(CleanupPreferences.CleanupStep.CONVERT_TO_BIBLATEX);
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.JOURNAL, "test");

        worker.cleanup(preset, entry);
        assertEquals(Optional.empty(), entry.getField(StandardField.JOURNAL));
        assertEquals(Optional.of("test"), entry.getField(StandardField.JOURNALTITLE));
    }

    @Test
    void cleanupWithDisabledFieldFormatterChangesNothing() {
        CleanupPreferences preset = new CleanupPreferences(new FieldFormatterCleanups(false,
                Collections.singletonList(new FieldFormatterCleanup(StandardField.MONTH, new NormalizeMonthFormatter()))));
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.MONTH, "01");

        worker.cleanup(preset, entry);
        assertEquals(Optional.of("01"), entry.getField(StandardField.MONTH));
    }
}
