package org.jabref.logic.lint.rules;

import java.util.List;

import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.lint.rule.Finding;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ResourceLock("Localization.lang")
class RepeatedWhitespaceRuleTest {

    private final RepeatedWhitespaceRule rule = new RepeatedWhitespaceRule(new FieldPreferences(true, List.of(), List.of()));

    @Test
    void collapsesRepeatedWhitespaceInAValue() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "A  Widely   Spaced Title");

        rule.scan(entry).forEach(finding -> finding.fix().orElseThrow().applyTo(entry));

        assertEquals("A Widely Spaced Title", entry.getField(StandardField.TITLE).orElseThrow());
    }

    /// A line break carries meaning in a field holding several lines, so it is left as it is.
    @Test
    void leavesAMultilineFieldAlone() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.ABSTRACT, "One line.\nAnd  another.");

        assertEquals(List.of(), rule.scan(entry));
    }

    /// A URL is not text: whitespace in it would be part of the address.
    @Test
    void leavesAFieldThatHoldsNoTextAlone() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.FILE, "a  file.pdf");

        assertEquals(List.of(), rule.scan(entry));
    }

    @Test
    void aValueWithSingleSpacesIsNotReported() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "A Title");

        assertEquals(List.of(), rule.scan(entry));
    }

    @Test
    void reportsWhatItWouldChangeWithoutChangingIt() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "A  Title");

        List<Finding> findings = rule.scan(entry);

        assertEquals(List.of(StandardField.TITLE), findings.stream().map(finding -> finding.field().orElseThrow()).toList());
        assertEquals("A  Title", entry.getField(StandardField.TITLE).orElseThrow());
    }
}
