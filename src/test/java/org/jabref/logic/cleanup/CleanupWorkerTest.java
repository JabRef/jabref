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

import org.jabref.logic.formatter.bibtexfields.HtmlToLatexFormatter;
import org.jabref.logic.formatter.bibtexfields.LatexCleanupFormatter;
import org.jabref.logic.formatter.bibtexfields.NormalizeDateFormatter;
import org.jabref.logic.formatter.bibtexfields.NormalizeMonthFormatter;
import org.jabref.logic.formatter.bibtexfields.NormalizePagesFormatter;
import org.jabref.logic.formatter.bibtexfields.UnitsToLatexFormatter;
import org.jabref.logic.formatter.casechanger.ProtectTermsFormatter;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.protectedterms.ProtectedTermsLoader;
import org.jabref.logic.protectedterms.ProtectedTermsPreferences;
import org.jabref.model.Defaults;
import org.jabref.model.FieldChange;
import org.jabref.model.cleanup.FieldFormatterCleanup;
import org.jabref.model.cleanup.FieldFormatterCleanups;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FileFieldWriter;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.metadata.FilePreferences;
import org.jabref.model.metadata.MetaData;

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

    private final CleanupPreset emptyPreset = new CleanupPreset(EnumSet.noneOf(CleanupPreset.CleanupStep.class));
    private CleanupWorker worker;

    @BeforeEach
    void setUp(@TempDir Path bibFolder) throws IOException {

        Path path = bibFolder.resolve("ARandomlyNamedFolder");
        Files.createDirectory(path);
        File pdfFolder = path.toFile();

        MetaData metaData = new MetaData();
        metaData.setDefaultFileDirectory(pdfFolder.getAbsolutePath());
        BibDatabaseContext context = new BibDatabaseContext(new BibDatabase(), metaData, new Defaults());
        Files.createFile(bibFolder.resolve("test.bib"));
        context.setDatabaseFile(bibFolder.resolve("test.bib").toFile());

        FilePreferences fileDirPrefs = mock(FilePreferences.class, Answers.RETURNS_SMART_NULLS);
        //Biblocation as Primary overwrites all other dirs
        when(fileDirPrefs.isBibLocationAsPrimary()).thenReturn(true);

        worker = new CleanupWorker(context,
                new CleanupPreferences(mock(LayoutFormatterPreferences.class), fileDirPrefs));
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
        entry.setCiteKey("Toot");
        entry.setField("pdf", "aPdfFile");
        entry.setField("some", "1st");
        entry.setField("doi", "http://dx.doi.org/10.1016/0001-8708(80)90035-3");
        entry.setField("month", "01");
        entry.setField("pages", "1-2");
        entry.setField("date", "01/1999");
        entry.setField("pdf", "aPdfFile");
        entry.setField("ps", "aPsFile");
        entry.setField("file", "link::");
        entry.setField("journal", "test");
        entry.setField("title", "<b>hallo</b> units 1 A case AlGaAs and latex $\\alpha$$\\beta$");
        entry.setField("abstract", "Réflexions");
        Path path = bibFolder.resolve("ARandomlyNamedFile");
        Files.createFile(path);
        File tempFile = path.toFile();
        LinkedFile fileField = new LinkedFile("", tempFile.getAbsolutePath(), "");
        entry.setField("file", FileFieldWriter.getStringRepresentation(fileField));

        List<FieldChange> changes = worker.cleanup(emptyPreset, entry);
        assertEquals(Collections.emptyList(), changes);
    }

    @Test
    void upgradeExternalLinksMoveFromPdfToFile() {
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.CLEAN_UP_UPGRADE_EXTERNAL_LINKS);
        BibEntry entry = new BibEntry();
        entry.setField("pdf", "aPdfFile");

        worker.cleanup(preset, entry);
        assertEquals(Optional.empty(), entry.getField("pdf"));
        assertEquals(Optional.of("aPdfFile:aPdfFile:PDF"), entry.getField("file"));
    }

    @Test
    void upgradeExternalLinksMoveFromPsToFile() {
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.CLEAN_UP_UPGRADE_EXTERNAL_LINKS);
        BibEntry entry = new BibEntry();
        entry.setField("ps", "aPsFile");

        worker.cleanup(preset, entry);
        assertEquals(Optional.empty(), entry.getField("pdf"));
        assertEquals(Optional.of("aPsFile:aPsFile:PostScript"), entry.getField("file"));
    }

    @Test
    void cleanupDoiRemovesLeadingHttp() {
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.CLEAN_UP_DOI);
        BibEntry entry = new BibEntry();
        entry.setField("doi", "http://dx.doi.org/10.1016/0001-8708(80)90035-3");

        worker.cleanup(preset, entry);
        assertEquals(Optional.of("10.1016/0001-8708(80)90035-3"), entry.getField("doi"));
    }

    @Test
    void cleanupDoiReturnsChanges() {
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.CLEAN_UP_DOI);
        BibEntry entry = new BibEntry();
        entry.setField("doi", "http://dx.doi.org/10.1016/0001-8708(80)90035-3");

        List<FieldChange> changes = worker.cleanup(preset, entry);

        FieldChange expectedChange = new FieldChange(entry, "doi", "http://dx.doi.org/10.1016/0001-8708(80)90035-3",
                "10.1016/0001-8708(80)90035-3");
        assertEquals(Collections.singletonList(expectedChange), changes);
    }

    @Test
    void cleanupDoiFindsDoiInURLFieldAndMoveItToDOIField() {
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.CLEAN_UP_DOI);
        BibEntry entry = new BibEntry();
        entry.setField("url", "http://dx.doi.org/10.1016/0001-8708(80)90035-3");

        worker.cleanup(preset, entry);
        assertEquals(Optional.of("10.1016/0001-8708(80)90035-3"), entry.getField("doi"));
        assertEquals(Optional.empty(), entry.getField("url"));
    }

    @Test
    void cleanupDoiReturnsChangeWhenDoiInURLField() {
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.CLEAN_UP_DOI);
        BibEntry entry = new BibEntry();
        entry.setField("url", "http://dx.doi.org/10.1016/0001-8708(80)90035-3");

        List<FieldChange> changes = worker.cleanup(preset, entry);
        List<FieldChange> changeList = new ArrayList<>();
        changeList.add(new FieldChange(entry, "doi", null, "10.1016/0001-8708(80)90035-3"));
        changeList.add(new FieldChange(entry, "url", "http://dx.doi.org/10.1016/0001-8708(80)90035-3", null));
        assertEquals(changeList, changes);
    }

    @Test
    void cleanupMonthChangesNumberToBibtex() {
        CleanupPreset preset = new CleanupPreset(new FieldFormatterCleanups(true,
                Collections.singletonList(new FieldFormatterCleanup("month", new NormalizeMonthFormatter()))));
        BibEntry entry = new BibEntry();
        entry.setField("month", "01");

        worker.cleanup(preset, entry);
        assertEquals(Optional.of("#jan#"), entry.getField("month"));
    }

    @Test
    void cleanupPageNumbersConvertsSingleDashToDouble() {
        CleanupPreset preset = new CleanupPreset(new FieldFormatterCleanups(true,
                Collections.singletonList(new FieldFormatterCleanup("pages", new NormalizePagesFormatter()))));
        BibEntry entry = new BibEntry();
        entry.setField("pages", "1-2");

        worker.cleanup(preset, entry);
        assertEquals(Optional.of("1--2"), entry.getField("pages"));
    }

    @Test
    void cleanupDatesConvertsToCorrectFormat() {
        CleanupPreset preset = new CleanupPreset(new FieldFormatterCleanups(true,
                Collections.singletonList(new FieldFormatterCleanup("date", new NormalizeDateFormatter()))));
        BibEntry entry = new BibEntry();
        entry.setField("date", "01/1999");

        worker.cleanup(preset, entry);
        assertEquals(Optional.of("1999-01"), entry.getField("date"));
    }

    @Test
    void cleanupFixFileLinksMovesSingleDescriptionToLink() {
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.FIX_FILE_LINKS);
        BibEntry entry = new BibEntry();
        entry.setField("file", "link::");

        worker.cleanup(preset, entry);
        assertEquals(Optional.of(":link:"), entry.getField("file"));
    }

    @Test
    void cleanupMoveFilesMovesFileFromSubfolder(@TempDir Path bibFolder) throws IOException {
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.MOVE_PDF);

        Path path = bibFolder.resolve("AnotherRandomlyNamedFolder");
        Files.createDirectory(path);
        File subfolder = path.toFile();
        File tempFile = new File(subfolder, "test.pdf");
        tempFile.createNewFile();
        BibEntry entry = new BibEntry();
        LinkedFile fileField = new LinkedFile("", tempFile.getAbsolutePath(), "");
        entry.setField("file", FileFieldWriter.getStringRepresentation(fileField));

        worker.cleanup(preset, entry);
        LinkedFile newFileField = new LinkedFile("", tempFile.getName(), "");
        assertEquals(Optional.of(FileFieldWriter.getStringRepresentation(newFileField)), entry.getField("file"));
    }

    @Test
    void cleanupRelativePathsConvertAbsoluteToRelativePath(@TempDir Path bibFolder) throws IOException {
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.MAKE_PATHS_RELATIVE);

        Path path = bibFolder.resolve("AnotherRandomlyNamedFile");
        Files.createFile(path);
        File tempFile = path.toFile();
        BibEntry entry = new BibEntry();
        LinkedFile fileField = new LinkedFile("", tempFile.getAbsolutePath(), "");
        entry.setField("file", FileFieldWriter.getStringRepresentation(fileField));

        worker.cleanup(preset, entry);
        LinkedFile newFileField = new LinkedFile("", tempFile.getName(), "");
        assertEquals(Optional.of(FileFieldWriter.getStringRepresentation(newFileField)), entry.getField("file"));
    }

    @Test
    void cleanupRenamePdfRenamesRelativeFile(@TempDir Path bibFolder) throws IOException {
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.RENAME_PDF);

        Path path = bibFolder.resolve("AnotherRandomlyNamedFile.tmp");
        Files.createFile(path);
        File tempFile = path.toFile();
        BibEntry entry = new BibEntry();
        entry.setCiteKey("Toot");
        LinkedFile fileField = new LinkedFile("", tempFile.getAbsolutePath(), "");
        entry.setField("file", FileFieldWriter.getStringRepresentation(fileField));

        worker.cleanup(preset, entry);
        LinkedFile newFileField = new LinkedFile("", "Toot.tmp", "");
        assertEquals(Optional.of(FileFieldWriter.getStringRepresentation(newFileField)), entry.getField("file"));
    }

    @Test
    void cleanupHtmlToLatexConvertsEpsilonToLatex() {
        CleanupPreset preset = new CleanupPreset(new FieldFormatterCleanups(true,
                Collections.singletonList(new FieldFormatterCleanup("title", new HtmlToLatexFormatter()))));
        BibEntry entry = new BibEntry();
        entry.setField("title", "&Epsilon;");

        worker.cleanup(preset, entry);
        assertEquals(Optional.of("{{$\\Epsilon$}}"), entry.getField("title"));
    }

    @Test
    void cleanupUnitsConvertsOneAmpereToLatex() {
        CleanupPreset preset = new CleanupPreset(new FieldFormatterCleanups(true,
                Collections.singletonList(new FieldFormatterCleanup("title", new UnitsToLatexFormatter()))));
        BibEntry entry = new BibEntry();
        entry.setField("title", "1 A");

        worker.cleanup(preset, entry);
        assertEquals(Optional.of("1~{A}"), entry.getField("title"));
    }

    @Test
    void cleanupCasesAddsBracketAroundAluminiumGalliumArsenid() {
        ProtectedTermsLoader protectedTermsLoader = new ProtectedTermsLoader(
                new ProtectedTermsPreferences(ProtectedTermsLoader.getInternalLists(), Collections.emptyList(),
                        Collections.emptyList(), Collections.emptyList()));
        assertNotEquals(Collections.emptyList(), protectedTermsLoader.getProtectedTerms());
        CleanupPreset preset = new CleanupPreset(new FieldFormatterCleanups(true, Collections
                .singletonList(new FieldFormatterCleanup("title", new ProtectTermsFormatter(protectedTermsLoader)))));
        BibEntry entry = new BibEntry();
        entry.setField("title", "AlGaAs");

        worker.cleanup(preset, entry);
        assertEquals(Optional.of("{AlGaAs}"), entry.getField("title"));
    }

    @Test
    void cleanupLatexMergesTwoLatexMathEnvironments() {
        CleanupPreset preset = new CleanupPreset(new FieldFormatterCleanups(true,
                Collections.singletonList(new FieldFormatterCleanup("title", new LatexCleanupFormatter()))));
        BibEntry entry = new BibEntry();
        entry.setField("title", "$\\alpha$$\\beta$");

        worker.cleanup(preset, entry);
        assertEquals(Optional.of("$\\alpha\\beta$"), entry.getField("title"));
    }

    @Test
    void convertToBiblatexMovesAddressToLocation() {
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.CONVERT_TO_BIBLATEX);
        BibEntry entry = new BibEntry();
        entry.setField("address", "test");

        worker.cleanup(preset, entry);
        assertEquals(Optional.empty(), entry.getField("address"));
        assertEquals(Optional.of("test"), entry.getField("location"));
    }

    @Test
    void convertToBiblatexMovesJournalToJournalTitle() {
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.CONVERT_TO_BIBLATEX);
        BibEntry entry = new BibEntry();
        entry.setField("journal", "test");

        worker.cleanup(preset, entry);
        assertEquals(Optional.empty(), entry.getField("journal"));
        assertEquals(Optional.of("test"), entry.getField("journaltitle"));
    }

    @Test
    void cleanupWithDisabledFieldFormatterChangesNothing() {
        CleanupPreset preset = new CleanupPreset(new FieldFormatterCleanups(false,
                Collections.singletonList(new FieldFormatterCleanup("month", new NormalizeMonthFormatter()))));
        BibEntry entry = new BibEntry();
        entry.setField("month", "01");

        worker.cleanup(preset, entry);
        assertEquals(Optional.of("01"), entry.getField("month"));
    }
}
