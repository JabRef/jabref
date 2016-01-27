package net.sf.jabref.logic.cleanup;

import net.sf.jabref.logic.FieldChange;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FileField;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class CleanupWorkerTest {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();


    @SuppressWarnings("unused")
    @Test(expected = NullPointerException.class)
    public void initWithNullPresetThrowsException() {
        new CleanupWorker(null);
    }

    @Test(expected = NullPointerException.class)
    public void cleanupNullThrowsException() {
        CleanupPreset preset = new CleanupPreset();
        CleanupWorker worker = new CleanupWorker(preset);
        worker.cleanup(null);
    }

    @Test
    public void cleanupDoesNothingByDefault() throws IOException {
        CleanupPreset preset = new CleanupPreset();
        CleanupWorker worker = new CleanupWorker(preset);
        BibEntry entry = new BibEntry();
        entry.setField(BibEntry.KEY_FIELD, "Toot");
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
        File tempFile = testFolder.newFile();
        FileField.ParsedFileField fileField = new FileField.ParsedFileField("", tempFile.getAbsolutePath(), "");
        entry.setField("file", FileField.getStringRepresentation(fileField));

        List<FieldChange> changes = worker.cleanup(entry);
        Assert.assertEquals(Collections.emptyList(), changes);
    }

    @Test
    public void upgradeExternalLinksMoveFromPdfToFile() {
        CleanupPreset preset = new CleanupPreset();
        preset.setCleanUpUpgradeExternalLinks(true);
        BibEntry entry = new BibEntry();
        entry.setField("pdf", "aPdfFile");

        CleanupWorker worker = new CleanupWorker(preset);
        worker.cleanup(entry);
        Assert.assertEquals(null, entry.getField("pdf"));
        Assert.assertEquals("aPdfFile:aPdfFile:PDF", entry.getField("file"));
    }

    @Test
    public void upgradeExternalLinksMoveFromPsToFile() {
        CleanupPreset preset = new CleanupPreset();
        preset.setCleanUpUpgradeExternalLinks(true);
        BibEntry entry = new BibEntry();
        entry.setField("ps", "aPsFile");

        CleanupWorker worker = new CleanupWorker(preset);
        worker.cleanup(entry);
        Assert.assertEquals(null, entry.getField("pdf"));
        Assert.assertEquals("aPsFile:aPsFile:PostScript", entry.getField("file"));
    }

    @Test
    public void cleanupSupercriptChangesFirstToLatex() {
        CleanupPreset preset = new CleanupPreset();
        preset.setCleanUpSuperscripts(true);
        BibEntry entry = new BibEntry();
        entry.setField("some", "1st");

        CleanupWorker worker = new CleanupWorker(preset);
        worker.cleanup(entry);
        Assert.assertEquals("1\\textsuperscript{st}", entry.getField("some"));
    }

    @Test
    public void cleanupDoiRemovesLeadingHttp() {
        CleanupPreset preset = new CleanupPreset();
        preset.setCleanUpDOI(true);
        BibEntry entry = new BibEntry();
        entry.setField("doi", "http://dx.doi.org/10.1016/0001-8708(80)90035-3");

        CleanupWorker worker = new CleanupWorker(preset);
        worker.cleanup(entry);
        Assert.assertEquals("10.1016/0001-8708(80)90035-3", entry.getField("doi"));
    }

    @Test
    public void cleanupDoiReturnsChanges() {
        CleanupPreset preset = new CleanupPreset();
        preset.setCleanUpDOI(true);
        BibEntry entry = new BibEntry();
        entry.setField("doi", "http://dx.doi.org/10.1016/0001-8708(80)90035-3");

        CleanupWorker worker = new CleanupWorker(preset);
        List<FieldChange> changes = worker.cleanup(entry);

        FieldChange expectedChange = new FieldChange(entry, "doi", "http://dx.doi.org/10.1016/0001-8708(80)90035-3",
                "10.1016/0001-8708(80)90035-3");
        Assert.assertEquals(Collections.singletonList(expectedChange), changes);
    }

    @Test
    public void cleanupMonthChangesNumberToBibtex() {
        CleanupPreset preset = new CleanupPreset();
        preset.setCleanUpMonth(true);
        BibEntry entry = new BibEntry();
        entry.setField("month", "01");

        CleanupWorker worker = new CleanupWorker(preset);
        worker.cleanup(entry);
        Assert.assertEquals("#jan#", entry.getField("month"));
    }

    @Test
    public void cleanupPagenumbersConvertsSingleDashToDouble() {
        CleanupPreset preset = new CleanupPreset();
        preset.setCleanUpPageNumbers(true);
        BibEntry entry = new BibEntry();
        entry.setField("pages", "1-2");

        CleanupWorker worker = new CleanupWorker(preset);
        worker.cleanup(entry);
        Assert.assertEquals("1--2", entry.getField("pages"));
    }

    @Test
    public void cleanupDatesConvertsToCorrectFormat() {
        CleanupPreset preset = new CleanupPreset();
        preset.setCleanUpDate(true);
        BibEntry entry = new BibEntry();
        entry.setField("date", "01/1999");

        CleanupWorker worker = new CleanupWorker(preset);
        worker.cleanup(entry);
        Assert.assertEquals("1999-01", entry.getField("date"));
    }

    @Test
    public void cleanupFixFileLinksMovesSingleDescriptionToLink() {
        CleanupPreset preset = new CleanupPreset();
        preset.setFixFileLinks(true);
        BibEntry entry = new BibEntry();
        entry.setField("file", "link::");

        CleanupWorker worker = new CleanupWorker(preset);
        worker.cleanup(entry);
        Assert.assertEquals(":link:", entry.getField("file"));
    }

    @Test
    public void cleanupRelativePathsConvertAbsoluteToRelativePath() throws IOException {
        CleanupPreset preset = new CleanupPreset();
        preset.setMakePathsRelative(true);

        File tempFile = testFolder.newFile();
        BibEntry entry = new BibEntry();
        FileField.ParsedFileField fileField = new FileField.ParsedFileField("", tempFile.getAbsolutePath(), "");
        entry.setField("file", FileField.getStringRepresentation(fileField));

        CleanupWorker worker = new CleanupWorker(preset,
                Collections.singletonList(testFolder.getRoot().getAbsolutePath()));
        worker.cleanup(entry);
        FileField.ParsedFileField newFileField = new FileField.ParsedFileField("", tempFile.getName(), "");
        Assert.assertEquals(FileField.getStringRepresentation(newFileField), entry.getField("file"));
    }

    @Test
    public void cleanupRenamePdfRenamesRelativeFile() throws IOException {
        CleanupPreset preset = new CleanupPreset();
        preset.setRenamePDF(true);

        File tempFile = testFolder.newFile();
        BibEntry entry = new BibEntry();
        entry.setField(BibEntry.KEY_FIELD, "Toot");
        FileField.ParsedFileField fileField = new FileField.ParsedFileField("", tempFile.getAbsolutePath(), "");
        entry.setField("file", FileField.getStringRepresentation(fileField));

        CleanupWorker worker = new CleanupWorker(preset,
                Collections.singletonList(testFolder.getRoot().getAbsolutePath()));
        worker.cleanup(entry);
        FileField.ParsedFileField newFileField = new FileField.ParsedFileField("", "Toot.tmp", "");
        Assert.assertEquals(FileField.getStringRepresentation(newFileField), entry.getField("file"));
    }

    @Test
    public void cleanupHtmlStripsHtmlTag() {
        CleanupPreset preset = new CleanupPreset();
        preset.setConvertHTMLToLatex(true);
        BibEntry entry = new BibEntry();
        entry.setField("title", "<b>hallo</b>");

        CleanupWorker worker = new CleanupWorker(preset);
        worker.cleanup(entry);
        Assert.assertEquals("hallo", entry.getField("title"));
    }

    @Test
    public void cleanupUnitsConvertsOneAmpereToLatex() {
        CleanupPreset preset = new CleanupPreset();
        preset.setConvertUnits(true);
        BibEntry entry = new BibEntry();
        entry.setField("title", "1 A");

        CleanupWorker worker = new CleanupWorker(preset);
        worker.cleanup(entry);
        Assert.assertEquals("1~{A}", entry.getField("title"));
    }

    @Test
    public void cleanupCasesAddsBracketAroundAluminiumGalliumArsenid() {
        CleanupPreset preset = new CleanupPreset();
        preset.setConvertCase(true);
        BibEntry entry = new BibEntry();
        entry.setField("title", "AlGaAs");

        CleanupWorker worker = new CleanupWorker(preset);
        worker.cleanup(entry);
        Assert.assertEquals("{AlGaAs}", entry.getField("title"));
    }

    @Test
    public void cleanupLatexMergesTwoLatexMathEnvironments() {
        CleanupPreset preset = new CleanupPreset();
        preset.setConvertLaTeX(true);
        BibEntry entry = new BibEntry();
        entry.setField("title", "$\\alpha$$\\beta$");

        CleanupWorker worker = new CleanupWorker(preset);
        worker.cleanup(entry);
        Assert.assertEquals("$\\alpha\\beta$", entry.getField("title"));
    }

    @Test
    public void cleanupUnicodeConvertsAcuteToLatex() {
        CleanupPreset preset = new CleanupPreset();
        preset.setConvertUnicodeToLatex(true);
        BibEntry entry = new BibEntry();
        entry.setField("abstract", "Réflexions");

        CleanupWorker worker = new CleanupWorker(preset);
        worker.cleanup(entry);
        Assert.assertEquals("R{\\'{e}}flexions", entry.getField("abstract"));
    }

    @Test
    public void convertToBiblatexMovesJournalToJournaltitle() {
        CleanupPreset preset = new CleanupPreset();
        preset.setConvertToBiblatex(true);
        BibEntry entry = new BibEntry();
        entry.setField("journal", "test");

        CleanupWorker worker = new CleanupWorker(preset);
        worker.cleanup(entry);
        Assert.assertEquals(null, entry.getField("journal"));
        Assert.assertEquals("test", entry.getField("journaltitle"));
    }
}
