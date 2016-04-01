package net.sf.jabref.logic.cleanup;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.exporter.FieldFormatterCleanups;
import net.sf.jabref.logic.FieldChange;
import net.sf.jabref.logic.formatter.bibtexfields.*;
import net.sf.jabref.logic.formatter.casechanger.ProtectTermsFormatter;
import net.sf.jabref.logic.journals.JournalAbbreviationLoader;
import net.sf.jabref.logic.journals.JournalAbbreviationRepository;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FileField;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

public class CleanupWorkerTest {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();


    @Before
    public void setUp() {
        if (Globals.prefs == null) {
            Globals.prefs = JabRefPreferences.getInstance();
        }
        if (Globals.journalAbbreviationLoader == null) {
            Globals.journalAbbreviationLoader = mock(JournalAbbreviationLoader.class);
        }
    }


    @SuppressWarnings("unused")
    @Test(expected = NullPointerException.class)
    public void initWithNullPresetThrowsException() {
        new CleanupWorker(null);
    }

    @Test(expected = NullPointerException.class)
    public void cleanupNullThrowsException() {
        CleanupPreset preset = new CleanupPreset(EnumSet.noneOf(CleanupPreset.CleanupStep.class));
        CleanupWorker worker = new CleanupWorker(preset);
        worker.cleanup(null);
    }

    @Test
    public void cleanupDoesNothingByDefault() throws IOException {
        CleanupPreset preset = new CleanupPreset(EnumSet.noneOf(CleanupPreset.CleanupStep.class));
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
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.CLEAN_UP_UPGRADE_EXTERNAL_LINKS);
        BibEntry entry = new BibEntry();
        entry.setField("pdf", "aPdfFile");

        CleanupWorker worker = new CleanupWorker(preset);
        worker.cleanup(entry);
        Assert.assertEquals(null, entry.getField("pdf"));
        Assert.assertEquals("aPdfFile:aPdfFile:PDF", entry.getField("file"));
    }

    @Test
    public void upgradeExternalLinksMoveFromPsToFile() {
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.CLEAN_UP_UPGRADE_EXTERNAL_LINKS);
        BibEntry entry = new BibEntry();
        entry.setField("ps", "aPsFile");

        CleanupWorker worker = new CleanupWorker(preset);
        worker.cleanup(entry);
        Assert.assertEquals(null, entry.getField("pdf"));
        Assert.assertEquals("aPsFile:aPsFile:PostScript", entry.getField("file"));
    }

    @Test
    public void cleanupSupercriptChangesFirstToLatex() {
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.CLEAN_UP_SUPERSCRIPTS);
        BibEntry entry = new BibEntry();
        entry.setField("some", "1st");

        CleanupWorker worker = new CleanupWorker(preset);
        worker.cleanup(entry);
        Assert.assertEquals("1\\textsuperscript{st}", entry.getField("some"));
    }

    @Test
    public void cleanupDoiRemovesLeadingHttp() {
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.CLEAN_UP_DOI);
        BibEntry entry = new BibEntry();
        entry.setField("doi", "http://dx.doi.org/10.1016/0001-8708(80)90035-3");

        CleanupWorker worker = new CleanupWorker(preset);
        worker.cleanup(entry);
        Assert.assertEquals("10.1016/0001-8708(80)90035-3", entry.getField("doi"));
    }

    @Test
    public void cleanupDoiReturnsChanges() {
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.CLEAN_UP_DOI);
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
        CleanupPreset preset = new CleanupPreset(new FieldFormatterCleanups(true,
                Collections.singletonList(new FieldFormatterCleanup("month", new NormalizeMonthFormatter()))));
        BibEntry entry = new BibEntry();
        entry.setField("month", "01");

        CleanupWorker worker = new CleanupWorker(preset);
        worker.cleanup(entry);
        Assert.assertEquals("#jan#", entry.getField("month"));
    }

    @Test
    public void cleanupPageNumbersConvertsSingleDashToDouble() {
        CleanupPreset preset = new CleanupPreset(new FieldFormatterCleanups(true,
                Collections.singletonList(new FieldFormatterCleanup("pages", new NormalizePagesFormatter()))));
        BibEntry entry = new BibEntry();
        entry.setField("pages", "1-2");

        CleanupWorker worker = new CleanupWorker(preset);
        worker.cleanup(entry);
        Assert.assertEquals("1--2", entry.getField("pages"));
    }

    @Test
    public void cleanupDatesConvertsToCorrectFormat() {
        CleanupPreset preset = new CleanupPreset(new FieldFormatterCleanups(true,
                Collections.singletonList(new FieldFormatterCleanup("date", new NormalizeDateFormatter()))));
        BibEntry entry = new BibEntry();
        entry.setField("date", "01/1999");

        CleanupWorker worker = new CleanupWorker(preset);
        worker.cleanup(entry);
        Assert.assertEquals("1999-01", entry.getField("date"));
    }

    @Test
    public void cleanupFixFileLinksMovesSingleDescriptionToLink() {
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.FIX_FILE_LINKS);
        BibEntry entry = new BibEntry();
        entry.setField("file", "link::");

        CleanupWorker worker = new CleanupWorker(preset);
        worker.cleanup(entry);
        Assert.assertEquals(":link:", entry.getField("file"));
    }

    @Test
    public void cleanupRelativePathsConvertAbsoluteToRelativePath() throws IOException {
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.MAKE_PATHS_RELATIVE);

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
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.RENAME_PDF);

        File tempFile = testFolder.newFile();
        BibEntry entry = new BibEntry();
        entry.setField(BibEntry.KEY_FIELD, "Toot");
        FileField.ParsedFileField fileField = new FileField.ParsedFileField("", tempFile.getAbsolutePath(), "");
        entry.setField("file", FileField.getStringRepresentation(fileField));

        CleanupWorker worker = new CleanupWorker(preset,
                Collections.singletonList(testFolder.getRoot().getAbsolutePath()), null,
                mock(JournalAbbreviationRepository.class));
        worker.cleanup(entry);
        FileField.ParsedFileField newFileField = new FileField.ParsedFileField("", "Toot.tmp", "");
        Assert.assertEquals(FileField.getStringRepresentation(newFileField), entry.getField("file"));
    }

    @Test
    public void cleanupHtmlToLatexConvertsEpsilonToLatex() {
        CleanupPreset preset = new CleanupPreset(new FieldFormatterCleanups(true,
                Collections.singletonList(new FieldFormatterCleanup("title", new HtmlToLatexFormatter()))));
        BibEntry entry = new BibEntry();
        entry.setField("title", "&Epsilon;");

        CleanupWorker worker = new CleanupWorker(preset);
        worker.cleanup(entry);
        Assert.assertEquals("{$\\Epsilon$}", entry.getField("title"));
    }

    @Test
    public void cleanupUnitsConvertsOneAmpereToLatex() {
        CleanupPreset preset = new CleanupPreset(new FieldFormatterCleanups(true,
                Collections.singletonList(new FieldFormatterCleanup("title", new UnitsToLatexFormatter()))));
        BibEntry entry = new BibEntry();
        entry.setField("title", "1 A");

        CleanupWorker worker = new CleanupWorker(preset);
        worker.cleanup(entry);
        Assert.assertEquals("1~{A}", entry.getField("title"));
    }

    @Test
    public void cleanupCasesAddsBracketAroundAluminiumGalliumArsenid() {
        CleanupPreset preset = new CleanupPreset(new FieldFormatterCleanups(true,
                Collections.singletonList(new FieldFormatterCleanup("title", new ProtectTermsFormatter()))));
        BibEntry entry = new BibEntry();
        entry.setField("title", "AlGaAs");

        CleanupWorker worker = new CleanupWorker(preset);
        worker.cleanup(entry);
        Assert.assertEquals("{AlGaAs}", entry.getField("title"));
    }

    @Test
    public void cleanupLatexMergesTwoLatexMathEnvironments() {
        CleanupPreset preset = new CleanupPreset(new FieldFormatterCleanups(true,
                Collections.singletonList(new FieldFormatterCleanup("title", new LatexCleanupFormatter()))));
        BibEntry entry = new BibEntry();
        entry.setField("title", "$\\alpha$$\\beta$");

        CleanupWorker worker = new CleanupWorker(preset);
        worker.cleanup(entry);
        Assert.assertEquals("$\\alpha\\beta$", entry.getField("title"));
    }

    @Test
    public void cleanupUnicodeConvertsAcuteToLatex() {
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.CONVERT_UNICODE_TO_LATEX);
        BibEntry entry = new BibEntry();
        entry.setField("abstract", "Réflexions");

        CleanupWorker worker = new CleanupWorker(preset);
        worker.cleanup(entry);
        Assert.assertEquals("R{\\'{e}}flexions", entry.getField("abstract"));
    }

    @Test
    public void convertToBiblatexMovesJournalToJournalTitle() {
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.CONVERT_TO_BIBLATEX);
        BibEntry entry = new BibEntry();
        entry.setField("journal", "test");

        CleanupWorker worker = new CleanupWorker(preset);
        worker.cleanup(entry);
        Assert.assertEquals(null, entry.getField("journal"));
        Assert.assertEquals("test", entry.getField("journaltitle"));
    }
}
