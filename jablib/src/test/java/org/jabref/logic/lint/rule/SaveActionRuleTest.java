package org.jabref.logic.lint.rule;

import java.util.List;

import org.jabref.logic.cleanup.FieldFormatterCleanup;
import org.jabref.logic.formatter.bibtexfields.ClearFormatter;
import org.jabref.logic.formatter.bibtexfields.NormalizePagesFormatter;
import org.jabref.logic.formatter.bibtexfields.TrimWhitespaceFormatter;
import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SaveActionRuleTest {

    private final SaveActionRule normalizePages = new SaveActionRule(
            new FieldFormatterCleanup(StandardField.PAGES, new NormalizePagesFormatter()));

    @Test
    void idIsTheFormattersKey() {
        assertEquals("normalize-page-numbers", normalizePages.id());
    }

    /// Scanning reports; only applying the finding's fix may change anything.
    @Test
    void reportsWhatTheSaveActionWouldChangeWithoutChangingIt() {
        BibEntry entry = new BibEntry(StandardEntryType.Article).withField(StandardField.PAGES, "21-45");

        List<Finding> findings = normalizePages.scan(entry);

        assertEquals(List.of(StandardField.PAGES), findings.stream().map(finding -> finding.field().orElseThrow()).toList());
        assertEquals("21-45", entry.getField(StandardField.PAGES).orElseThrow());
    }

    @Test
    void theFixSetsTheValueTheSaveActionProduces() {
        BibEntry entry = new BibEntry(StandardEntryType.Article).withField(StandardField.PAGES, "21-45");

        normalizePages.scan(entry).forEach(finding -> finding.fix().orElseThrow().applyTo(entry));

        assertEquals("21--45", entry.getField(StandardField.PAGES).orElseThrow());
    }

    /// What the repair changed is what an undo manager collects.
    @Test
    void theFixReportsTheChangeItMade() {
        BibEntry entry = new BibEntry(StandardEntryType.Article).withField(StandardField.PAGES, "21-45");

        FieldChange change = normalizePages.scan(entry).getFirst().fix().orElseThrow().applyTo(entry).orElseThrow();

        assertEquals("21-45", change.oldValue());
        assertEquals("21--45", change.newValue());
    }

    @Test
    void theMessageNamesTheValueAndWhatItShouldBe() {
        BibEntry entry = new BibEntry(StandardEntryType.Article).withField(StandardField.PAGES, "21-45");

        assertEquals("\"21-45\", should be \"21--45\"", normalizePages.scan(entry).getFirst().message());
    }

    @Test
    void theMessageOfAFieldTheSaveActionEmptiesSaysSo() {
        SaveActionRule clearNote = new SaveActionRule(new FieldFormatterCleanup(StandardField.NOTE, new ClearFormatter()));
        BibEntry entry = new BibEntry(StandardEntryType.Article).withField(StandardField.NOTE, "to be removed");

        assertEquals("\"to be removed\", should be removed", clearNote.scan(entry).getFirst().message());
    }

    /// A field value can be as long as an abstract, while a finding is one line.
    @Test
    void aLongValueIsCutShortInTheMessage() {
        SaveActionRule trimNote = new SaveActionRule(new FieldFormatterCleanup(StandardField.NOTE, new TrimWhitespaceFormatter()));
        BibEntry entry = new BibEntry(StandardEntryType.Article).withField(StandardField.NOTE, " " + "a".repeat(80) + " ");

        assertEquals("\" %s...\", should be \"%s...\"".formatted("a".repeat(56), "a".repeat(57)),
                trimNote.scan(entry).getFirst().message());
    }

    @Test
    void aValueTheSaveActionLeavesAloneIsNotReported() {
        BibEntry entry = new BibEntry(StandardEntryType.Article).withField(StandardField.PAGES, "21--45");

        assertEquals(List.of(), normalizePages.scan(entry));
    }

    /// Each finding's fix repairs its own field only, even when the Save Action covers all fields.
    @Test
    void aSaveActionOnAllFieldsReportsEachFieldSeparately() {
        SaveActionRule trimAll = new SaveActionRule(
                new FieldFormatterCleanup(InternalField.INTERNAL_ALL_FIELD, new TrimWhitespaceFormatter()));
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, " Doe, Jane ")
                .withField(StandardField.TITLE, " A Title ")
                .withField(StandardField.YEAR, "2024");

        List<Finding> findings = trimAll.scan(entry);
        findings.stream()
                .filter(finding -> finding.field().orElseThrow() == StandardField.TITLE)
                .forEach(finding -> finding.fix().orElseThrow().applyTo(entry));

        assertEquals(2, findings.size());
        assertEquals("A Title", entry.getField(StandardField.TITLE).orElseThrow());
        assertEquals(" Doe, Jane ", entry.getField(StandardField.AUTHOR).orElseThrow());
    }

    @Test
    void aFieldTheSaveActionEmptiesIsRemoved() {
        SaveActionRule clearNote = new SaveActionRule(new FieldFormatterCleanup(StandardField.NOTE, new ClearFormatter()));
        BibEntry entry = new BibEntry(StandardEntryType.Article).withField(StandardField.NOTE, "to be removed");

        clearNote.scan(entry).forEach(finding -> finding.fix().orElseThrow().applyTo(entry));

        assertFalse(entry.hasField(StandardField.NOTE));
    }
}
