package net.sf.jabref.exporter;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.logic.cleanup.FieldFormatterCleanup;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibtexEntryTypes;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

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
    public void checkSimpleUseCase() throws IOException {

        FieldFormatterCleanups actions = new FieldFormatterCleanups(true, "title[IdentityFormatter]");

        actions.applySaveActions(entry);

        assertEquals("Educational session 1", entry.getField("title"));
    }

    @Test
    public void invalidSaveActionSting() throws IOException {

        FieldFormatterCleanups actions = new FieldFormatterCleanups(true, "title");

        actions.applySaveActions(entry);

        assertEquals("Educational session 1", entry.getField("title"));
    }

    @Test
    public void checkLowerCaseSaveAction() throws IOException {

        FieldFormatterCleanups actions = new FieldFormatterCleanups(true, "title[LowerCaseChanger]");

        actions.applySaveActions(entry);

        assertEquals("educational session 1", entry.getField("title"));
    }

    @Test
    public void checkTwoSaveActionsForOneField() throws IOException {
        FieldFormatterCleanups actions = new FieldFormatterCleanups(true, "title[LowerCaseChanger,IdentityFormatter]");

        assertEquals(2, actions.getConfiguredActions().size());

        actions.applySaveActions(entry);

        assertEquals("educational session 1", entry.getField("title"));
    }

    @Test
    public void checkThreeSaveActionsForOneField() throws IOException {

        FieldFormatterCleanups actions = new FieldFormatterCleanups(true, "title[LowerCaseChanger,IdentityFormatter,DateFormatter]");

        assertEquals(3, actions.getConfiguredActions().size());

        actions.applySaveActions(entry);

        assertEquals("educational session 1", entry.getField("title"));
    }


    @Test
    public void checkMultipleSaveActions() throws IOException {

        FieldFormatterCleanups actions = new FieldFormatterCleanups(true, "pages[PageNumbersFormatter]title[LowerCaseChanger]");

        List<FieldFormatterCleanup> formatterCleanups = actions.getConfiguredActions();

        assertEquals(2, formatterCleanups.size());

        for (FieldFormatterCleanup cleanup : formatterCleanups) {
            if (cleanup.getField().equals("title")) {
                assertEquals("LowerCaseChanger", cleanup.getFormatter().getKey());
            } else if (cleanup.getField().equals("pages")) {
                assertEquals("PageNumbersFormatter", cleanup.getFormatter().getKey());
            }
        }

        actions.applySaveActions(entry);

        assertEquals("educational session 1", entry.getField("title"));
        assertEquals("1--7", entry.getField("pages"));
    }

    @Test
    public void checkMultipleSaveActionsWithMultipleFormatters() throws IOException {

        FieldFormatterCleanups actions = new FieldFormatterCleanups(true, "pages[PageNumbersFormatter,DateFormatter]title[LowerCaseChanger]");
        List<FieldFormatterCleanup> formatterCleanups = actions.getConfiguredActions();

        assertEquals(3, formatterCleanups.size());

        for (FieldFormatterCleanup cleanup : formatterCleanups) {
            if (cleanup.getField().equals("title")) {
                assertEquals("LowerCaseChanger", cleanup.getFormatter().getKey());
            } else if (cleanup.getField().equals("pages")) {
                if (!("PageNumbersFormatter".equals(cleanup.getFormatter().getKey()) || "DateFormatter".equals(cleanup.getFormatter().getKey()))) {
                    fail("Wrong formatter for pages field: " + cleanup.getFormatter().getKey());
                }
            }
        }

        actions.applySaveActions(entry);

        assertEquals("educational session 1", entry.getField("title"));
        assertEquals("1--7", entry.getField("pages"));
    }

    @Test
    public void eraseFormatterRemovesField() {
        FieldFormatterCleanups actions = new FieldFormatterCleanups(true, "mont[EraseFormatter]");
        actions.applySaveActions(entry);

        assertEquals(Optional.empty(), entry.getFieldOptional("mont"));
    }
}
