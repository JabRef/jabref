package org.jabref.logic.exporter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.cleanup.Cleanups;
import org.jabref.logic.formatter.IdentityFormatter;
import org.jabref.logic.formatter.bibtexfields.NormalizeDateFormatter;
import org.jabref.logic.formatter.bibtexfields.NormalizePagesFormatter;
import org.jabref.logic.formatter.casechanger.LowerCaseFormatter;
import org.jabref.model.cleanup.FieldFormatterCleanup;
import org.jabref.model.cleanup.FieldFormatterCleanups;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexEntryTypes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FieldFormatterCleanupsTest {

    private BibEntry entry;


    @BeforeEach
    public void setUp() {
        entry = new BibEntry();
        entry.setType(BibtexEntryTypes.INPROCEEDINGS);
        entry.setCiteKey("6055279");
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

        FieldFormatterCleanups actions = new FieldFormatterCleanups(true, Cleanups.parse("title[identity]"));

        FieldFormatterCleanup identityInTitle = new FieldFormatterCleanup("title", new IdentityFormatter());
        assertEquals(Collections.singletonList(identityInTitle), actions.getConfiguredActions());

        actions.applySaveActions(entry);

        assertEquals(Optional.of("Educational session 1"), entry.getField("title"));
    }

    @Test
    public void invalidSaveActionSting() {

        FieldFormatterCleanups actions = new FieldFormatterCleanups(true, Cleanups.parse("title"));

        assertEquals(Collections.emptyList(), actions.getConfiguredActions());

        actions.applySaveActions(entry);

        assertEquals(Optional.of("Educational session 1"), entry.getField("title"));
    }

    @Test
    public void checkLowerCaseSaveAction() {

        FieldFormatterCleanups actions = new FieldFormatterCleanups(true, Cleanups.parse("title[lower_case]"));

        FieldFormatterCleanup lowerCaseTitle = new FieldFormatterCleanup("title", new LowerCaseFormatter());
        assertEquals(Collections.singletonList(lowerCaseTitle), actions.getConfiguredActions());

        actions.applySaveActions(entry);

        assertEquals(Optional.of("educational session 1"), entry.getField("title"));
    }

    @Test
    public void checkTwoSaveActionsForOneField() {
        FieldFormatterCleanups actions = new FieldFormatterCleanups(true, Cleanups.parse("title[lower_case,identity]"));

        FieldFormatterCleanup lowerCaseTitle = new FieldFormatterCleanup("title", new LowerCaseFormatter());
        FieldFormatterCleanup identityInTitle = new FieldFormatterCleanup("title", new IdentityFormatter());
        assertEquals(Arrays.asList(lowerCaseTitle, identityInTitle), actions.getConfiguredActions());

        actions.applySaveActions(entry);

        assertEquals(Optional.of("educational session 1"), entry.getField("title"));
    }

    @Test
    public void checkThreeSaveActionsForOneField() {

        FieldFormatterCleanups actions = new FieldFormatterCleanups(true, Cleanups.parse("title[lower_case,identity,normalize_date]"));

        FieldFormatterCleanup lowerCaseTitle = new FieldFormatterCleanup("title", new LowerCaseFormatter());
        FieldFormatterCleanup identityInTitle = new FieldFormatterCleanup("title", new IdentityFormatter());
        FieldFormatterCleanup normalizeDatesInTitle = new FieldFormatterCleanup("title", new NormalizeDateFormatter());
        assertEquals(Arrays.asList(lowerCaseTitle, identityInTitle, normalizeDatesInTitle), actions.getConfiguredActions());

        actions.applySaveActions(entry);

        assertEquals(Optional.of("educational session 1"), entry.getField("title"));
    }

    @Test
    public void checkMultipleSaveActions() {

        FieldFormatterCleanups actions = new FieldFormatterCleanups(true, Cleanups.parse("pages[normalize_page_numbers]title[lower_case]"));
        List<FieldFormatterCleanup> formatterCleanups = actions.getConfiguredActions();

        FieldFormatterCleanup normalizePages = new FieldFormatterCleanup("pages", new NormalizePagesFormatter());
        FieldFormatterCleanup lowerCaseTitle = new FieldFormatterCleanup("title", new LowerCaseFormatter());
        assertEquals(Arrays.asList(normalizePages, lowerCaseTitle), formatterCleanups);

        actions.applySaveActions(entry);

        assertEquals(Optional.of("educational session 1"), entry.getField("title"));
        assertEquals(Optional.of("1--7"), entry.getField("pages"));
    }

    @Test
    public void checkMultipleSaveActionsWithMultipleFormatters() {

        FieldFormatterCleanups actions = new FieldFormatterCleanups(true,
                Cleanups.parse("pages[normalize_page_numbers,normalize_date]title[lower_case]"));
        List<FieldFormatterCleanup> formatterCleanups = actions.getConfiguredActions();

        FieldFormatterCleanup normalizePages = new FieldFormatterCleanup("pages", new NormalizePagesFormatter());
        FieldFormatterCleanup normalizeDatesInPages = new FieldFormatterCleanup("pages", new NormalizeDateFormatter());
        FieldFormatterCleanup lowerCaseTitle = new FieldFormatterCleanup("title", new LowerCaseFormatter());
        assertEquals(Arrays.asList(normalizePages, normalizeDatesInPages, lowerCaseTitle), formatterCleanups);

        actions.applySaveActions(entry);

        assertEquals(Optional.of("educational session 1"), entry.getField("title"));
        assertEquals(Optional.of("1--7"), entry.getField("pages"));
    }

    @Test
    public void clearFormatterRemovesField() {
        FieldFormatterCleanups actions = new FieldFormatterCleanups(true, Cleanups.parse("mont[clear]"));
        actions.applySaveActions(entry);

        assertEquals(Optional.empty(), entry.getField("mont"));
    }
}
