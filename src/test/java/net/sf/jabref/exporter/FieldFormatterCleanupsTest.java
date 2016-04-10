package net.sf.jabref.exporter;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.logic.cleanup.FieldFormatterCleanup;
import net.sf.jabref.logic.formatter.IdentityFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.NormalizeDateFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.NormalizePagesFormatter;
import net.sf.jabref.logic.formatter.casechanger.LowerCaseFormatter;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibtexEntryTypes;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;

public class FieldFormatterCleanupsTest {

    private BibEntry entry;


    @BeforeClass
    public static void setUpPreferences() {
        if (Globals.prefs == null) {
            Globals.prefs = JabRefPreferences.getInstance();
        }
    }

    @Before
    public void setUp() {
        entry = new BibEntry();
        entry.setType(BibtexEntryTypes.INPROCEEDINGS);
        entry.setField(BibEntry.KEY_FIELD, "6055279");
        entry.setField("title", "Educational session 1");
        entry.setField("booktitle", "Custom Integrated Circuits Conference (CICC), 2011 IEEE");
        entry.setField("year", "2011");
        entry.setField("mont", "Sept.");
        entry.setField("pages", "1-7");
        entry.setField("abstract", "Start of the above-titled section of the conference proceedings record.");
        entry.setField("doi", "10.1109/CICC.2011.6055279");
        entry.setField("issn", "0886-5930");
    }

    @Test
    public void checkSimpleUseCase() {

        FieldFormatterCleanups actions = new FieldFormatterCleanups(true, "title[identity]");

        FieldFormatterCleanup identityInTitle = new FieldFormatterCleanup("title", new IdentityFormatter());
        assertEquals(Collections.singletonList(identityInTitle), actions.getConfiguredActions());

        actions.applySaveActions(entry);

        assertEquals("Educational session 1", entry.getField("title"));
    }

    @Test
    public void invalidSaveActionSting() {

        FieldFormatterCleanups actions = new FieldFormatterCleanups(true, "title");

        assertEquals(Collections.emptyList(), actions.getConfiguredActions());

        actions.applySaveActions(entry);

        assertEquals("Educational session 1", entry.getField("title"));
    }

    @Test
    public void checkLowerCaseSaveAction() {

        FieldFormatterCleanups actions = new FieldFormatterCleanups(true, "title[lower_case]");

        FieldFormatterCleanup lowerCaseTitle = new FieldFormatterCleanup("title", new LowerCaseFormatter());
        assertEquals(Collections.singletonList(lowerCaseTitle), actions.getConfiguredActions());

        actions.applySaveActions(entry);

        assertEquals("educational session 1", entry.getField("title"));
    }

    @Test
    public void checkTwoSaveActionsForOneField() {
        FieldFormatterCleanups actions = new FieldFormatterCleanups(true, "title[lower_case,identity]");

        FieldFormatterCleanup lowerCaseTitle = new FieldFormatterCleanup("title", new LowerCaseFormatter());
        FieldFormatterCleanup identityInTitle = new FieldFormatterCleanup("title", new IdentityFormatter());
        assertEquals(Arrays.asList(lowerCaseTitle, identityInTitle), actions.getConfiguredActions());

        actions.applySaveActions(entry);

        assertEquals("educational session 1", entry.getField("title"));
    }

    @Test
    public void checkThreeSaveActionsForOneField() {

        FieldFormatterCleanups actions = new FieldFormatterCleanups(true, "title[lower_case,identity,normalize_date]");

        FieldFormatterCleanup lowerCaseTitle = new FieldFormatterCleanup("title", new LowerCaseFormatter());
        FieldFormatterCleanup identityInTitle = new FieldFormatterCleanup("title", new IdentityFormatter());
        FieldFormatterCleanup normalizeDatesInTitle = new FieldFormatterCleanup("title", new NormalizeDateFormatter());
        assertEquals(Arrays.asList(lowerCaseTitle, identityInTitle, normalizeDatesInTitle), actions.getConfiguredActions());

        actions.applySaveActions(entry);

        assertEquals("educational session 1", entry.getField("title"));
    }

    @Test
    public void checkMultipleSaveActions() {

        FieldFormatterCleanups actions = new FieldFormatterCleanups(true, "pages[normalize_page_numbers]title[lower_case]");
        List<FieldFormatterCleanup> formatterCleanups = actions.getConfiguredActions();

        FieldFormatterCleanup normalizePages = new FieldFormatterCleanup("pages", new NormalizePagesFormatter());
        FieldFormatterCleanup lowerCaseTitle = new FieldFormatterCleanup("title", new LowerCaseFormatter());
        assertEquals(Arrays.asList(normalizePages, lowerCaseTitle), formatterCleanups);

        actions.applySaveActions(entry);

        assertEquals("educational session 1", entry.getField("title"));
        assertEquals("1--7", entry.getField("pages"));
    }

    @Test
    public void checkMultipleSaveActionsWithMultipleFormatters() {

        FieldFormatterCleanups actions = new FieldFormatterCleanups(true,
                "pages[normalize_page_numbers,normalize_date]title[lower_case]");
        List<FieldFormatterCleanup> formatterCleanups = actions.getConfiguredActions();

        FieldFormatterCleanup normalizePages = new FieldFormatterCleanup("pages", new NormalizePagesFormatter());
        FieldFormatterCleanup normalizeDatesInPages = new FieldFormatterCleanup("pages", new NormalizeDateFormatter());
        FieldFormatterCleanup lowerCaseTitle = new FieldFormatterCleanup("title", new LowerCaseFormatter());
        assertEquals(Arrays.asList(normalizePages, normalizeDatesInPages, lowerCaseTitle), formatterCleanups);

        actions.applySaveActions(entry);

        assertEquals("educational session 1", entry.getField("title"));
        assertEquals("1--7", entry.getField("pages"));
    }

    @Test
    public void clearFormatterRemovesField() {
        FieldFormatterCleanups actions = new FieldFormatterCleanups(true, "mont[clear]");
        actions.applySaveActions(entry);

        assertEquals(Optional.empty(), entry.getFieldOptional("mont"));
    }
}
