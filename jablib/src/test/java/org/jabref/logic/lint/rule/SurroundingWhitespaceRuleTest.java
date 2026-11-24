package org.jabref.logic.lint.rule;

import java.util.List;

import org.jabref.logic.lint.rules.SurroundingWhitespaceRule;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ResourceLock("Localization.lang")
class SurroundingWhitespaceRuleTest {

    private final SurroundingWhitespaceRule rule = new SurroundingWhitespaceRule();

    @Test
    void repairsEveryPaddedFieldOfAnEntry() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "  Doe, Jane ")
                .withField(StandardField.TITLE, "\tA Title\n")
                .withField(StandardField.YEAR, "2024");

        List<Finding> findings = rule.scan(entry);
        findings.forEach(finding -> finding.fix().orElseThrow().applyTo(entry));

        assertEquals(2, findings.size());
        assertEquals("Doe, Jane", entry.getField(StandardField.AUTHOR).orElseThrow());
        assertEquals("A Title", entry.getField(StandardField.TITLE).orElseThrow());
    }

    /// Rewriting a citation key would invalidate every crossref pointing at it, so the rule passes
    /// over it just as JabRef's own field formatters do.
    @Test
    void leavesTheCitationKeyAlone() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey(" key ")
                .withField(StandardField.TITLE, " A Title ");

        List<Finding> findings = rule.scan(entry);

        assertEquals(List.of(StandardField.TITLE), findings.stream().map(finding -> finding.field().orElseThrow()).toList());
    }

    /// Whitespace between words can be deliberate, so only the ends are touched.
    @Test
    void leavesWhitespaceInsideAValueAlone() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "A  Deliberately   Spaced Title");

        assertEquals(List.of(), rule.scan(entry));
    }
}
