package net.sf.jabref.logic.cleanup;

import net.sf.jabref.BibDatabaseContext;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.exporter.FieldFormatterCleanups;
import net.sf.jabref.logic.FieldChange;
import net.sf.jabref.logic.journals.JournalAbbreviationLoader;
import net.sf.jabref.logic.journals.JournalAbbreviationRepository;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FileField;
import net.sf.jabref.model.entry.ParsedFileField;
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

    private CleanupPreset emptyPreset = new CleanupPreset(EnumSet.noneOf(CleanupPreset.CleanupStep.class));
    private CleanupWorker worker;


    @Before
    public void setUp() {
        if (Globals.prefs == null) {
            Globals.prefs = JabRefPreferences.getInstance();
        }
        if (Globals.journalAbbreviationLoader == null) {
            Globals.journalAbbreviationLoader = mock(JournalAbbreviationLoader.class);
        }
        worker = new CleanupWorker(Collections.singletonList(testFolder.getRoot().getAbsolutePath()),
                new BibDatabaseContext(), mock(JournalAbbreviationRepository.class));
    }


    @SuppressWarnings("unused")
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
        Assert.assertEquals(null, entry.getField("pdf"));
        Assert.assertEquals("aPdfFile:aPdfFile:PDF", entry.getField("file"));
    }

    @Test
    public void upgradeExternalLinksMoveFromPsToFile() {
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.CLEAN_UP_UPGRADE_EXTERNAL_LINKS);
        BibEntry entry = new BibEntry();
        entry.setField("ps", "aPsFile");

        worker.cleanup(preset, entry);
        Assert.assertEquals(null, entry.getField("pdf"));
        Assert.assertEquals("aPsFile:aPsFile:PostScript", entry.getField("file"));
    }

    @Test
    public void cleanupSupercriptChangesFirstToLatex() {
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.CLEAN_UP_SUPERSCRIPTS);
        BibEntry entry = new BibEntry();
        entry.setField("some", "1st");

        worker.cleanup(preset, entry);
        Assert.assertEquals("1\\textsuperscript{st}", entry.getField("some"));
    }

    @Test
    public void cleanupDoiRemovesLeadingHttp() {
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.CLEAN_UP_DOI);
        BibEntry entry = new BibEntry();
        entry.setField("doi", "http://dx.doi.org/10.1016/0001-8708(80)90035-3");

        worker.cleanup(preset, entry);
        Assert.assertEquals("10.1016/0001-8708(80)90035-3", entry.getField("doi"));
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
    public void cleanupMonthChangesNumberToBibtex() {
        CleanupPreset preset = new CleanupPreset(new FieldFormatterCleanups(true, "month[MonthFormatter]"));
        BibEntry entry = new BibEntry();
        entry.setField("month", "01");

        worker.cleanup(preset, entry);
        Assert.assertEquals("#jan#", entry.getField("month"));
    }

    @Test
    public void cleanupPageNumbersConvertsSingleDashToDouble() {
        CleanupPreset preset = new CleanupPreset(new FieldFormatterCleanups(true, "pages[PageNumbersFormatter]"));
        BibEntry entry = new BibEntry();
        entry.setField("pages", "1-2");

        worker.cleanup(preset, entry);
        Assert.assertEquals("1--2", entry.getField("pages"));
    }

    @Test
    public void cleanupDatesConvertsToCorrectFormat() {
        CleanupPreset preset = new CleanupPreset(new FieldFormatterCleanups(true, "date[DateFormatter]"));
        BibEntry entry = new BibEntry();
        entry.setField("date", "01/1999");

        worker.cleanup(preset, entry);
        Assert.assertEquals("1999-01", entry.getField("date"));
    }

    @Test
    public void cleanupFixFileLinksMovesSingleDescriptionToLink() {
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.FIX_FILE_LINKS);
        BibEntry entry = new BibEntry();
        entry.setField("file", "link::");

        worker.cleanup(preset, entry);
        Assert.assertEquals(":link:", entry.getField("file"));
    }

    @Test
    public void cleanupRelativePathsConvertAbsoluteToRelativePath() throws IOException {
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.MAKE_PATHS_RELATIVE);

        File tempFile = testFolder.newFile();
        BibEntry entry = new BibEntry();
        ParsedFileField fileField = new ParsedFileField("", tempFile.getAbsolutePath(), "");
        entry.setField("file", FileField.getStringRepresentation(fileField));

        worker.cleanup(preset, entry);
        ParsedFileField newFileField = new ParsedFileField("", tempFile.getName(), "");
        Assert.assertEquals(FileField.getStringRepresentation(newFileField), entry.getField("file"));
    }

    @Test
    public void cleanupRenamePdfRenamesRelativeFile() throws IOException {
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.RENAME_PDF);

        File tempFile = testFolder.newFile();
        BibEntry entry = new BibEntry();
        entry.setField(BibEntry.KEY_FIELD, "Toot");
        ParsedFileField fileField = new ParsedFileField("", tempFile.getAbsolutePath(), "");
        entry.setField("file", FileField.getStringRepresentation(fileField));

        worker.cleanup(preset, entry);
        ParsedFileField newFileField = new ParsedFileField("", "Toot.tmp", "");
        Assert.assertEquals(FileField.getStringRepresentation(newFileField), entry.getField("file"));
    }

    @Test
    public void cleanupHtmlStripsHtmlTag() {
        CleanupPreset preset = new CleanupPreset(new FieldFormatterCleanups(true, "title[HtmlConverter]"));
        BibEntry entry = new BibEntry();
        entry.setField("title", "<b>hallo</b>");

        worker.cleanup(preset, entry);
        Assert.assertEquals("hallo", entry.getField("title"));
    }

    @Test
    public void cleanupUnitsConvertsOneAmpereToLatex() {
        CleanupPreset preset = new CleanupPreset(new FieldFormatterCleanups(true, "title[UnitFormatter]"));
        BibEntry entry = new BibEntry();
        entry.setField("title", "1 A");

        worker.cleanup(preset, entry);
        Assert.assertEquals("1~{A}", entry.getField("title"));
    }

    @Test
    public void cleanupCasesAddsBracketAroundAluminiumGalliumArsenid() {
        CleanupPreset preset = new CleanupPreset(new FieldFormatterCleanups(true, "title[CaseKeeper]"));
        BibEntry entry = new BibEntry();
        entry.setField("title", "AlGaAs");

        worker.cleanup(preset, entry);
        Assert.assertEquals("{AlGaAs}", entry.getField("title"));
    }

    @Test
    public void cleanupLatexMergesTwoLatexMathEnvironments() {
        CleanupPreset preset = new CleanupPreset(new FieldFormatterCleanups(true, "title[LatexFormatter]"));
        BibEntry entry = new BibEntry();
        entry.setField("title", "$\\alpha$$\\beta$");

        worker.cleanup(preset, entry);
        Assert.assertEquals("$\\alpha\\beta$", entry.getField("title"));
    }

    @Test
    public void cleanupUnicodeConvertsAcuteToLatex() {
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.CONVERT_UNICODE_TO_LATEX);
        BibEntry entry = new BibEntry();
        entry.setField("abstract", "Réflexions");

        worker.cleanup(preset, entry);
        Assert.assertEquals("R{\\'{e}}flexions", entry.getField("abstract"));
    }

    @Test
    public void convertToBiblatexMovesJournalToJournalTitle() {
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.CONVERT_TO_BIBLATEX);
        BibEntry entry = new BibEntry();
        entry.setField("journal", "test");

        worker.cleanup(preset, entry);
        Assert.assertEquals(null, entry.getField("journal"));
        Assert.assertEquals("test", entry.getField("journaltitle"));
    }
}
