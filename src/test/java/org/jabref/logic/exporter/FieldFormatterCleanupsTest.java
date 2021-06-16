package org.jabref.logic.exporter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.cleanup.Cleanups;
import org.jabref.logic.cleanup.FieldFormatterCleanup;
import org.jabref.logic.cleanup.FieldFormatterCleanups;
import org.jabref.logic.formatter.IdentityFormatter;
import org.jabref.logic.formatter.bibtexfields.NormalizeDateFormatter;
import org.jabref.logic.formatter.bibtexfields.NormalizePagesFormatter;
import org.jabref.logic.formatter.casechanger.LowerCaseFormatter;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FieldFormatterCleanupsTest {

    private BibEntry entry;

    @BeforeEach
    public void setUp() {
        entry = new BibEntry();
        entry.setType(StandardEntryType.InProceedings);
        entry.setCitationKey("6055279");
        entry.setField(StandardField.TITLE, "Educational session 1");
        entry.setField(StandardField.BOOKTITLE, "Custom Integrated Circuits Conference (CICC), 2011 IEEE");
        entry.setField(StandardField.YEAR, "2011");
        entry.setField(StandardField.MONTH, "Sept.");
        entry.setField(StandardField.PAGES, "1-7");
        entry.setField(StandardField.ABSTRACT, "Start of the above-titled section of the conference proceedings record.");
        entry.setField(StandardField.DOI, "10.1109/CICC.2011.6055279");
        entry.setField(StandardField.ISSN, "0886-5930");
    }

    @Test
    public void checkSimpleUseCase() {

        FieldFormatterCleanups actions = new FieldFormatterCleanups(true, Cleanups.parse("title[identity]"));

        FieldFormatterCleanup identityInTitle = new FieldFormatterCleanup(StandardField.TITLE, new IdentityFormatter());
        assertEquals(Collections.singletonList(identityInTitle), actions.getConfiguredActions());

        actions.applySaveActions(entry);

        assertEquals(Optional.of("Educational session 1"), entry.getField(StandardField.TITLE));
    }

    @Test
    public void invalidSaveActionSting() {
        FieldFormatterCleanups actions = new FieldFormatterCleanups(true, Cleanups.parse("title"));

        assertEquals(Collections.emptyList(), actions.getConfiguredActions());

        actions.applySaveActions(entry);

        assertEquals(Optional.of("Educational session 1"), entry.getField(StandardField.TITLE));
    }

    @Test
    public void checkLowerCaseSaveAction() {

        FieldFormatterCleanups actions = new FieldFormatterCleanups(true, Cleanups.parse("title[lower_case]"));

        FieldFormatterCleanup lowerCaseTitle = new FieldFormatterCleanup(StandardField.TITLE, new LowerCaseFormatter());
        assertEquals(Collections.singletonList(lowerCaseTitle), actions.getConfiguredActions());

        actions.applySaveActions(entry);

        assertEquals(Optional.of("educational session 1"), entry.getField(StandardField.TITLE));
    }

    @Test
    public void checkTwoSaveActionsForOneField() {
        FieldFormatterCleanups actions = new FieldFormatterCleanups(true, Cleanups.parse("title[lower_case,identity]"));

        FieldFormatterCleanup lowerCaseTitle = new FieldFormatterCleanup(StandardField.TITLE, new LowerCaseFormatter());
        FieldFormatterCleanup identityInTitle = new FieldFormatterCleanup(StandardField.TITLE, new IdentityFormatter());
        assertEquals(Arrays.asList(lowerCaseTitle, identityInTitle), actions.getConfiguredActions());

        actions.applySaveActions(entry);

        assertEquals(Optional.of("educational session 1"), entry.getField(StandardField.TITLE));
    }

    @Test
    public void checkThreeSaveActionsForOneField() {

        FieldFormatterCleanups actions = new FieldFormatterCleanups(true, Cleanups.parse("title[lower_case,identity,normalize_date]"));

        FieldFormatterCleanup lowerCaseTitle = new FieldFormatterCleanup(StandardField.TITLE, new LowerCaseFormatter());
        FieldFormatterCleanup identityInTitle = new FieldFormatterCleanup(StandardField.TITLE, new IdentityFormatter());
        FieldFormatterCleanup normalizeDatesInTitle = new FieldFormatterCleanup(StandardField.TITLE, new NormalizeDateFormatter());
        assertEquals(Arrays.asList(lowerCaseTitle, identityInTitle, normalizeDatesInTitle), actions.getConfiguredActions());

        actions.applySaveActions(entry);

        assertEquals(Optional.of("educational session 1"), entry.getField(StandardField.TITLE));
    }

    @Test
    public void checkMultipleSaveActions() {

        FieldFormatterCleanups actions = new FieldFormatterCleanups(true, Cleanups.parse("pages[normalize_page_numbers]title[lower_case]"));
        List<FieldFormatterCleanup> formatterCleanups = actions.getConfiguredActions();

        FieldFormatterCleanup normalizePages = new FieldFormatterCleanup(StandardField.PAGES, new NormalizePagesFormatter());
        FieldFormatterCleanup lowerCaseTitle = new FieldFormatterCleanup(StandardField.TITLE, new LowerCaseFormatter());
        assertEquals(Arrays.asList(normalizePages, lowerCaseTitle), formatterCleanups);

        actions.applySaveActions(entry);

        assertEquals(Optional.of("educational session 1"), entry.getField(StandardField.TITLE));
        assertEquals(Optional.of("1--7"), entry.getField(StandardField.PAGES));
    }

    @Test
    public void checkMultipleSaveActionsWithMultipleFormatters() {

        FieldFormatterCleanups actions = new FieldFormatterCleanups(true,
                Cleanups.parse("pages[normalize_page_numbers,normalize_date]title[lower_case]"));
        List<FieldFormatterCleanup> formatterCleanups = actions.getConfiguredActions();

        FieldFormatterCleanup normalizePages = new FieldFormatterCleanup(StandardField.PAGES, new NormalizePagesFormatter());
        FieldFormatterCleanup normalizeDatesInPages = new FieldFormatterCleanup(StandardField.PAGES, new NormalizeDateFormatter());
        FieldFormatterCleanup lowerCaseTitle = new FieldFormatterCleanup(StandardField.TITLE, new LowerCaseFormatter());
        assertEquals(Arrays.asList(normalizePages, normalizeDatesInPages, lowerCaseTitle), formatterCleanups);

        actions.applySaveActions(entry);

        assertEquals(Optional.of("educational session 1"), entry.getField(StandardField.TITLE));
        assertEquals(Optional.of("1--7"), entry.getField(StandardField.PAGES));
    }

    @Test
    public void clearFormatterRemovesField() {
        FieldFormatterCleanups actions = new FieldFormatterCleanups(true, Cleanups.parse("month[clear]"));
        actions.applySaveActions(entry);

        assertEquals(Optional.empty(), entry.getField(StandardField.MONTH));
    }
}
