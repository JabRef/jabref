package org.jabref.logic.cleanup;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.nio.file.*;

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
import org.jabref.model.metadata.FileDirectoryPreferences;
import org.jabref.model.metadata.MetaData;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.TempDirectory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(TempDirectory.class)
public class CleanupWorkerTest {

    private final CleanupPreset emptyPreset = new CleanupPreset(EnumSet.noneOf(CleanupPreset.CleanupStep.class));
    private CleanupWorker worker;
    private File pdfFolder;

    @BeforeEach
    public void setUp(@TempDirectory.TempDir Path bibFolder) throws IOException {

        MetaData metaData = new MetaData();
        Path path = bibFolder.resolve("ARandomlyNamedFolder");
        Files.createDirectory(path);
        pdfFolder = path.toFile();
        metaData.setDefaultFileDirectory(pdfFolder.getAbsolutePath());
        BibDatabaseContext context = new BibDatabaseContext(new BibDatabase(), metaData, new Defaults());
        Files.createFile(path.resolve("test.bib"));
        context.setDatabaseFile(path.resolve("test.bib").toFile());

        FileDirectoryPreferences fileDirPrefs = mock(FileDirectoryPreferences.class);
        //Biblocation as Primary overwrites all other dirs
        when(fileDirPrefs.isBibLocationAsPrimary()).thenReturn(true);

        worker = new CleanupWorker(context,
                //empty fileDirPattern for backwards compatibility
                new CleanupPreferences("[bibtexkey]",
                        "",
                        mock(LayoutFormatterPreferences.class),
                        fileDirPrefs));

    }

    @ParameterizedTest
    @MethodSource("instancesToTest")
    public void cleanupWithNullPresetThrowsException() {
        worker.cleanup(null, new BibEntry());
    }

    @ParameterizedTest
    @MethodSource("instancesToTest")
    public void cleanupNullEntryThrowsException() {
        worker.cleanup(emptyPreset, null);
    }

    @ParameterizedTest
    @MethodSource("instancesToTest")
    public void cleanupDoesNothingByDefault(@TempDirectory.TempDir Path bibFolder) throws IOException {
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
        Path path = bibFolder.resolve("ARandomlyNamedFolder");
        Files.createDirectory(path);
        File tempFile = Files.createFile(path.resolve("")).toFile();
        LinkedFile fileField = new LinkedFile("", tempFile.getAbsolutePath(), "");
        entry.setField("file", FileFieldWriter.getStringRepresentation(fileField));

        List<FieldChange> changes = worker.cleanup(emptyPreset, entry);
        assertEquals(Collections.emptyList(), changes);
    }

    @ParameterizedTest
    @MethodSource("instancesToTest")
    public void upgradeExternalLinksMoveFromPdfToFile() {
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.CLEAN_UP_UPGRADE_EXTERNAL_LINKS);
        BibEntry entry = new BibEntry();
        entry.setField("pdf", "aPdfFile");

        worker.cleanup(preset, entry);
        assertEquals(Optional.empty(), entry.getField("pdf"));
        assertEquals(Optional.of("aPdfFile:aPdfFile:PDF"), entry.getField("file"));
    }

    @ParameterizedTest
    @MethodSource("instancesToTest")
    public void upgradeExternalLinksMoveFromPsToFile() {
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.CLEAN_UP_UPGRADE_EXTERNAL_LINKS);
        BibEntry entry = new BibEntry();
        entry.setField("ps", "aPsFile");

        worker.cleanup(preset, entry);
        assertEquals(Optional.empty(), entry.getField("pdf"));
        assertEquals(Optional.of("aPsFile:aPsFile:PostScript"), entry.getField("file"));
    }

    @ParameterizedTest
    @MethodSource("instancesToTest")
    public void cleanupDoiRemovesLeadingHttp() {
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.CLEAN_UP_DOI);
        BibEntry entry = new BibEntry();
        entry.setField("doi", "http://dx.doi.org/10.1016/0001-8708(80)90035-3");

        worker.cleanup(preset, entry);
        assertEquals(Optional.of("10.1016/0001-8708(80)90035-3"), entry.getField("doi"));
    }

    @ParameterizedTest
    @MethodSource("instancesToTest")
    public void cleanupDoiReturnsChanges() {
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.CLEAN_UP_DOI);
        BibEntry entry = new BibEntry();
        entry.setField("doi", "http://dx.doi.org/10.1016/0001-8708(80)90035-3");

        List<FieldChange> changes = worker.cleanup(preset, entry);

        FieldChange expectedChange = new FieldChange(entry, "doi", "http://dx.doi.org/10.1016/0001-8708(80)90035-3",
                "10.1016/0001-8708(80)90035-3");
        assertEquals(Collections.singletonList(expectedChange), changes);
    }

    @ParameterizedTest
    @MethodSource("instancesToTest")
    public void cleanupDoiFindsDoiInURLFieldAndMoveItToDOIField() {
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.CLEAN_UP_DOI);
        BibEntry entry = new BibEntry();
        entry.setField("url", "http://dx.doi.org/10.1016/0001-8708(80)90035-3");

        worker.cleanup(preset, entry);
        assertEquals(Optional.of("10.1016/0001-8708(80)90035-3"), entry.getField("doi"));
        assertEquals(Optional.empty(), entry.getField("url"));
    }

    @ParameterizedTest
    @MethodSource("instancesToTest")
    public void cleanupDoiReturnsChangeWhenDoiInURLField() {
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.CLEAN_UP_DOI);
        BibEntry entry = new BibEntry();
        entry.setField("url", "http://dx.doi.org/10.1016/0001-8708(80)90035-3");

        List<FieldChange> changes = worker.cleanup(preset, entry);
        List<FieldChange> changeList = new ArrayList<>();
        changeList.add(new FieldChange(entry, "doi", null, "10.1016/0001-8708(80)90035-3"));
        changeList.add(new FieldChange(entry, "url", "http://dx.doi.org/10.1016/0001-8708(80)90035-3", null));
        assertEquals(changeList, changes);
    }

    @ParameterizedTest
    @MethodSource("instancesToTest")
    public void cleanupMonthChangesNumberToBibtex() {
        CleanupPreset preset = new CleanupPreset(new FieldFormatterCleanups(true,
                Collections.singletonList(new FieldFormatterCleanup("month", new NormalizeMonthFormatter()))));
        BibEntry entry = new BibEntry();
        entry.setField("month", "01");

        worker.cleanup(preset, entry);
        assertEquals(Optional.of("#jan#"), entry.getField("month"));
    }

    @ParameterizedTest
    @MethodSource("instancesToTest")
    public void cleanupPageNumbersConvertsSingleDashToDouble() {
        CleanupPreset preset = new CleanupPreset(new FieldFormatterCleanups(true,
                Collections.singletonList(new FieldFormatterCleanup("pages", new NormalizePagesFormatter()))));
        BibEntry entry = new BibEntry();
        entry.setField("pages", "1-2");

        worker.cleanup(preset, entry);
        assertEquals(Optional.of("1--2"), entry.getField("pages"));
    }

    @ParameterizedTest
    @MethodSource("instancesToTest")
    public void cleanupDatesConvertsToCorrectFormat() {
        CleanupPreset preset = new CleanupPreset(new FieldFormatterCleanups(true,
                Collections.singletonList(new FieldFormatterCleanup("date", new NormalizeDateFormatter()))));
        BibEntry entry = new BibEntry();
        entry.setField("date", "01/1999");

        worker.cleanup(preset, entry);
        assertEquals(Optional.of("1999-01"), entry.getField("date"));
    }

    @ParameterizedTest
    @MethodSource("instancesToTest")
    public void cleanupFixFileLinksMovesSingleDescriptionToLink() {
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.FIX_FILE_LINKS);
        BibEntry entry = new BibEntry();
        entry.setField("file", "link::");

        worker.cleanup(preset, entry);
        assertEquals(Optional.of(":link:"), entry.getField("file"));
    }

    @ParameterizedTest
    @MethodSource("instancesToTest")
    public void cleanupMoveFilesMovesFileFromSubfolder(@TempDirectory.TempDir Path bibFolder) throws IOException {
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

    @ParameterizedTest
    @MethodSource("instancesToTest")
    public void cleanupRelativePathsConvertAbsoluteToRelativePath(@TempDirectory.TempDir Path bibFolder) throws IOException {
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.MAKE_PATHS_RELATIVE);

        Path path = bibFolder.resolve("AnotherRandomlyNamedFolder");
        Files.createDirectory(path);
        File tempFile = path.toFile();
        BibEntry entry = new BibEntry();
        LinkedFile fileField = new LinkedFile("", tempFile.getAbsolutePath(), "");
        entry.setField("file", FileFieldWriter.getStringRepresentation(fileField));

        worker.cleanup(preset, entry);
        LinkedFile newFileField = new LinkedFile("", tempFile.getName(), "");
        assertEquals(Optional.of(FileFieldWriter.getStringRepresentation(newFileField)), entry.getField("file"));
    }

    @ParameterizedTest
    @MethodSource("instancesToTest")
    public void cleanupRenamePdfRenamesRelativeFile(@TempDirectory.TempDir Path bibFolder) throws IOException {
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.RENAME_PDF);

        Path path = bibFolder.resolve("AnotherRandomlyNamedFolder");
        Files.createDirectory(path);
        File tempFile = path.toFile();
        BibEntry entry = new BibEntry();
        entry.setCiteKey("Toot");
        LinkedFile fileField = new LinkedFile("", tempFile.getAbsolutePath(), "");
        entry.setField("file", FileFieldWriter.getStringRepresentation(fileField));

        worker.cleanup(preset, entry);
        LinkedFile newFileField = new LinkedFile("", "Toot.tmp", "");
        assertEquals(Optional.of(FileFieldWriter.getStringRepresentation(newFileField)), entry.getField("file"));
    }

    @ParameterizedTest
    @MethodSource("instancesToTest")
    public void cleanupHtmlToLatexConvertsEpsilonToLatex() {
        CleanupPreset preset = new CleanupPreset(new FieldFormatterCleanups(true,
                Collections.singletonList(new FieldFormatterCleanup("title", new HtmlToLatexFormatter()))));
        BibEntry entry = new BibEntry();
        entry.setField("title", "&Epsilon;");

        worker.cleanup(preset, entry);
        assertEquals(Optional.of("{{$\\Epsilon$}}"), entry.getField("title"));
    }

    @ParameterizedTest
    @MethodSource("instancesToTest")
    public void cleanupUnitsConvertsOneAmpereToLatex() {
        CleanupPreset preset = new CleanupPreset(new FieldFormatterCleanups(true,
                Collections.singletonList(new FieldFormatterCleanup("title", new UnitsToLatexFormatter()))));
        BibEntry entry = new BibEntry();
        entry.setField("title", "1 A");

        worker.cleanup(preset, entry);
        assertEquals(Optional.of("1~{A}"), entry.getField("title"));
    }

    @ParameterizedTest
    @MethodSource("instancesToTest")
    public void cleanupCasesAddsBracketAroundAluminiumGalliumArsenid() {
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

    @ParameterizedTest
    @MethodSource("instancesToTest")
    public void cleanupLatexMergesTwoLatexMathEnvironments() {
        CleanupPreset preset = new CleanupPreset(new FieldFormatterCleanups(true,
                Collections.singletonList(new FieldFormatterCleanup("title", new LatexCleanupFormatter()))));
        BibEntry entry = new BibEntry();
        entry.setField("title", "$\\alpha$$\\beta$");

        worker.cleanup(preset, entry);
        assertEquals(Optional.of("$\\alpha\\beta$"), entry.getField("title"));
    }

    @ParameterizedTest
    @MethodSource("instancesToTest")
    public void convertToBiblatexMovesAddressToLocation() {
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.CONVERT_TO_BIBLATEX);
        BibEntry entry = new BibEntry();
        entry.setField("address", "test");

        worker.cleanup(preset, entry);
        assertEquals(Optional.empty(), entry.getField("address"));
        assertEquals(Optional.of("test"), entry.getField("location"));
    }

    @ParameterizedTest
    @MethodSource("instancesToTest")
    public void convertToBiblatexMovesJournalToJournalTitle() {
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.CONVERT_TO_BIBLATEX);
        BibEntry entry = new BibEntry();
        entry.setField("journal", "test");

        worker.cleanup(preset, entry);
        assertEquals(Optional.empty(), entry.getField("journal"));
        assertEquals(Optional.of("test"), entry.getField("journaltitle"));
    }

    @ParameterizedTest
    @MethodSource("instancesToTest")
    public void cleanupWithDisabledFieldFormatterChangesNothing() {
        CleanupPreset preset = new CleanupPreset(new FieldFormatterCleanups(false,
                Collections.singletonList(new FieldFormatterCleanup("month", new NormalizeMonthFormatter()))));
        BibEntry entry = new BibEntry();
        entry.setField("month", "01");

        worker.cleanup(preset, entry);
        assertEquals(Optional.of("01"), entry.getField("month"));
    }

}
