package net.sf.jabref.logic.cleanup;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import net.sf.jabref.logic.formatter.bibtexfields.HtmlToLatexFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.LatexCleanupFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.NormalizeDateFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.NormalizeMonthFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.NormalizePagesFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.UnitsToLatexFormatter;
import net.sf.jabref.logic.formatter.casechanger.ProtectTermsFormatter;
import net.sf.jabref.logic.layout.LayoutFormatterPreferences;
import net.sf.jabref.logic.protectedterms.ProtectedTermsLoader;
import net.sf.jabref.logic.protectedterms.ProtectedTermsPreferences;
import net.sf.jabref.model.FieldChange;
import net.sf.jabref.model.cleanup.FieldFormatterCleanup;
import net.sf.jabref.model.cleanup.FieldFormatterCleanups;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.database.BibDatabaseContext;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FileField;
import net.sf.jabref.model.entry.ParsedFileField;
import net.sf.jabref.model.metadata.MetaData;
import net.sf.jabref.preferences.JabRefPreferences;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.mockito.Mockito.mock;

public class CleanupWorkerTest {

    @Rule
    public TemporaryFolder bibFolder = new TemporaryFolder();

    private final CleanupPreset emptyPreset = new CleanupPreset(EnumSet.noneOf(CleanupPreset.CleanupStep.class));
    private CleanupWorker worker;
    private File pdfFolder;


    @Before
    public void setUp() throws IOException {
        pdfFolder = bibFolder.newFolder();

        MetaData metaData = new MetaData();
        metaData.setDefaultFileDirectory(pdfFolder.getAbsolutePath());
        BibDatabaseContext context = new BibDatabaseContext(new BibDatabase(), metaData, bibFolder.newFile("test.bib"));
        worker = new CleanupWorker(context,
                new CleanupPreferences(JabRefPreferences.getInstance().get(JabRefPreferences.IMPORT_FILENAMEPATTERN),
                        mock(LayoutFormatterPreferences.class),
                        JabRefPreferences.getInstance().getFileDirectoryPreferences()));
    }

    @Test(expected = NullPointerException.class)
    public void cleanupWithNullPresetThrowsException() {
        worker.cleanup(null, new BibEntry());
    }

    @Test(expected = NullPointerException.class)
    public void cleanupNullEntryThrowsException() {
        worker.cleanup(emptyPreset, null);
    }

    @Test
    public void cleanupDoesNothingByDefault() throws IOException {
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
        File tempFile = bibFolder.newFile();
        ParsedFileField fileField = new ParsedFileField("", tempFile.getAbsolutePath(), "");
        entry.setField("file", FileField.getStringRepresentation(fileField));

        List<FieldChange> changes = worker.cleanup(emptyPreset, entry);
        Assert.assertEquals(Collections.emptyList(), changes);
    }

    @Test
    public void upgradeExternalLinksMoveFromPdfToFile() {
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.CLEAN_UP_UPGRADE_EXTERNAL_LINKS);
        BibEntry entry = new BibEntry();
        entry.setField("pdf", "aPdfFile");

        worker.cleanup(preset, entry);
        Assert.assertEquals(Optional.empty(), entry.getField("pdf"));
        Assert.assertEquals(Optional.of("aPdfFile:aPdfFile:PDF"), entry.getField("file"));
    }

    @Test
    public void upgradeExternalLinksMoveFromPsToFile() {
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.CLEAN_UP_UPGRADE_EXTERNAL_LINKS);
        BibEntry entry = new BibEntry();
        entry.setField("ps", "aPsFile");

        worker.cleanup(preset, entry);
        Assert.assertEquals(Optional.empty(), entry.getField("pdf"));
        Assert.assertEquals(Optional.of("aPsFile:aPsFile:PostScript"), entry.getField("file"));
    }

    @Test
    public void cleanupDoiRemovesLeadingHttp() {
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.CLEAN_UP_DOI);
        BibEntry entry = new BibEntry();
        entry.setField("doi", "http://dx.doi.org/10.1016/0001-8708(80)90035-3");

        worker.cleanup(preset, entry);
        Assert.assertEquals(Optional.of("10.1016/0001-8708(80)90035-3"), entry.getField("doi"));
    }

    @Test
    public void cleanupDoiReturnsChanges() {
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.CLEAN_UP_DOI);
        BibEntry entry = new BibEntry();
        entry.setField("doi", "http://dx.doi.org/10.1016/0001-8708(80)90035-3");

        List<FieldChange> changes = worker.cleanup(preset, entry);

        FieldChange expectedChange = new FieldChange(entry, "doi", "http://dx.doi.org/10.1016/0001-8708(80)90035-3",
                "10.1016/0001-8708(80)90035-3");
        Assert.assertEquals(Collections.singletonList(expectedChange), changes);
    }

    @Test
    public void cleanupDoiFindsDoiInURLFieldAndMoveItToDOIField() {
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.CLEAN_UP_DOI);
        BibEntry entry = new BibEntry();
        entry.setField("url", "http://dx.doi.org/10.1016/0001-8708(80)90035-3");

        worker.cleanup(preset, entry);
        Assert.assertEquals(Optional.of("10.1016/0001-8708(80)90035-3"), entry.getField("doi"));
        Assert.assertEquals(Optional.empty(), entry.getField("url"));
    }

    @Test
    public void cleanupDoiReturnsChangeWhenDoiInURLField() {
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.CLEAN_UP_DOI);
        BibEntry entry = new BibEntry();
        entry.setField("url", "http://dx.doi.org/10.1016/0001-8708(80)90035-3");

        List<FieldChange> changes = worker.cleanup(preset, entry);
        List<FieldChange> changeList = new ArrayList<>();
        changeList.add(new FieldChange(entry, "doi", null, "10.1016/0001-8708(80)90035-3"));
        changeList.add(new FieldChange(entry, "url", "http://dx.doi.org/10.1016/0001-8708(80)90035-3", null));
        Assert.assertEquals(changeList, changes);
    }

    @Test
    public void cleanupMonthChangesNumberToBibtex() {
        CleanupPreset preset = new CleanupPreset(new FieldFormatterCleanups(true,
                Collections.singletonList(new FieldFormatterCleanup("month", new NormalizeMonthFormatter()))));
        BibEntry entry = new BibEntry();
        entry.setField("month", "01");

        worker.cleanup(preset, entry);
        Assert.assertEquals(Optional.of("#jan#"), entry.getField("month"));
    }

    @Test
    public void cleanupPageNumbersConvertsSingleDashToDouble() {
        CleanupPreset preset = new CleanupPreset(new FieldFormatterCleanups(true,
                Collections.singletonList(new FieldFormatterCleanup("pages", new NormalizePagesFormatter()))));
        BibEntry entry = new BibEntry();
        entry.setField("pages", "1-2");

        worker.cleanup(preset, entry);
        Assert.assertEquals(Optional.of("1--2"), entry.getField("pages"));
    }

    @Test
    public void cleanupDatesConvertsToCorrectFormat() {
        CleanupPreset preset = new CleanupPreset(new FieldFormatterCleanups(true,
                Collections.singletonList(new FieldFormatterCleanup("date", new NormalizeDateFormatter()))));
        BibEntry entry = new BibEntry();
        entry.setField("date", "01/1999");

        worker.cleanup(preset, entry);
        Assert.assertEquals(Optional.of("1999-01"), entry.getField("date"));
    }

    @Test
    public void cleanupFixFileLinksMovesSingleDescriptionToLink() {
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.FIX_FILE_LINKS);
        BibEntry entry = new BibEntry();
        entry.setField("file", "link::");

        worker.cleanup(preset, entry);
        Assert.assertEquals(Optional.of(":link:"), entry.getField("file"));
    }

    @Test
    public void cleanupMoveFilesMovesFileFromSubfolder() throws IOException {
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.MOVE_PDF);

        File subfolder = bibFolder.newFolder();
        File tempFile = new File(subfolder, "test.pdf");
        tempFile.createNewFile();
        BibEntry entry = new BibEntry();
        ParsedFileField fileField = new ParsedFileField("", tempFile.getAbsolutePath(), "");
        entry.setField("file", FileField.getStringRepresentation(fileField));

        worker.cleanup(preset, entry);
        ParsedFileField newFileField = new ParsedFileField("", tempFile.getName(), "");
        Assert.assertEquals(Optional.of(FileField.getStringRepresentation(newFileField)),
                entry.getField("file"));
    }

    @Test
    public void cleanupRelativePathsConvertAbsoluteToRelativePath() throws IOException {
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.MAKE_PATHS_RELATIVE);

        File tempFile = bibFolder.newFile();
        BibEntry entry = new BibEntry();
        ParsedFileField fileField = new ParsedFileField("", tempFile.getAbsolutePath(), "");
        entry.setField("file", FileField.getStringRepresentation(fileField));

        worker.cleanup(preset, entry);
        ParsedFileField newFileField = new ParsedFileField("", tempFile.getName(), "");
        Assert.assertEquals(Optional.of(FileField.getStringRepresentation(newFileField)),
                entry.getField("file"));
    }

    @Test
    public void cleanupRenamePdfRenamesRelativeFile() throws IOException {
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.RENAME_PDF);

        File tempFile = bibFolder.newFile();
        BibEntry entry = new BibEntry();
        entry.setCiteKey("Toot");
        ParsedFileField fileField = new ParsedFileField("", tempFile.getAbsolutePath(), "");
        entry.setField("file", FileField.getStringRepresentation(fileField));

        worker.cleanup(preset, entry);
        ParsedFileField newFileField = new ParsedFileField("", "Toot.tmp", "");
        Assert.assertEquals(Optional.of(FileField.getStringRepresentation(newFileField)),
                entry.getField("file"));
    }

    @Test
    public void cleanupHtmlToLatexConvertsEpsilonToLatex() {
        CleanupPreset preset = new CleanupPreset(new FieldFormatterCleanups(true,
                Collections.singletonList(new FieldFormatterCleanup("title", new HtmlToLatexFormatter()))));
        BibEntry entry = new BibEntry();
        entry.setField("title", "&Epsilon;");

        worker.cleanup(preset, entry);
        Assert.assertEquals(Optional.of("{{$\\Epsilon$}}"), entry.getField("title"));
    }

    @Test
    public void cleanupUnitsConvertsOneAmpereToLatex() {
        CleanupPreset preset = new CleanupPreset(new FieldFormatterCleanups(true,
                Collections.singletonList(new FieldFormatterCleanup("title", new UnitsToLatexFormatter()))));
        BibEntry entry = new BibEntry();
        entry.setField("title", "1 A");

        worker.cleanup(preset, entry);
        Assert.assertEquals(Optional.of("1~{A}"), entry.getField("title"));
    }

    @Test
    public void cleanupCasesAddsBracketAroundAluminiumGalliumArsenid() {
        ProtectedTermsLoader protectedTermsLoader = new ProtectedTermsLoader(
                new ProtectedTermsPreferences(ProtectedTermsLoader.getInternalLists(), Collections.emptyList(),
                        Collections.emptyList(), Collections.emptyList()));
        Assert.assertNotEquals(Collections.emptyList(), protectedTermsLoader.getProtectedTerms());
        CleanupPreset preset = new CleanupPreset(new FieldFormatterCleanups(true,
                Collections.singletonList(new FieldFormatterCleanup("title", new ProtectTermsFormatter(protectedTermsLoader)))));
        BibEntry entry = new BibEntry();
        entry.setField("title", "AlGaAs");

        worker.cleanup(preset, entry);
        Assert.assertEquals(Optional.of("{AlGaAs}"), entry.getField("title"));
    }

    @Test
    public void cleanupLatexMergesTwoLatexMathEnvironments() {
        CleanupPreset preset = new CleanupPreset(new FieldFormatterCleanups(true,
                Collections.singletonList(new FieldFormatterCleanup("title", new LatexCleanupFormatter()))));
        BibEntry entry = new BibEntry();
        entry.setField("title", "$\\alpha$$\\beta$");

        worker.cleanup(preset, entry);
        Assert.assertEquals(Optional.of("$\\alpha\\beta$"), entry.getField("title"));
    }

    @Test
    public void convertToBiblatexMovesJournalToJournalTitle() {
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.CONVERT_TO_BIBLATEX);
        BibEntry entry = new BibEntry();
        entry.setField("journal", "test");

        worker.cleanup(preset, entry);
        Assert.assertEquals(Optional.empty(), entry.getField("journal"));
        Assert.assertEquals(Optional.of("test"), entry.getField("journaltitle"));
    }

    @Test
    public void cleanupWithDisabledFieldFormatterChangesNothing() {
        CleanupPreset preset = new CleanupPreset(new FieldFormatterCleanups(false,
                Collections.singletonList(new FieldFormatterCleanup("month", new NormalizeMonthFormatter()))));
        BibEntry entry = new BibEntry();
        entry.setField("month", "01");

        worker.cleanup(preset, entry);
        Assert.assertEquals(Optional.of("01"), entry.getField("month"));
    }
}
