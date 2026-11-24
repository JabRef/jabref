package org.jabref.logic.lint;

import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.lint.rule.Finding;
import org.jabref.logic.lint.rule.Rule;
import org.jabref.logic.lint.rule.RuleSet;
import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JabFixTest {

    /// Reports every field of an entry, so that a magic comment naming one field can be told apart
    /// from one naming the rule as a whole.
    private static final Rule EVERY_FIELD = new Rule() {
        @Override
        public String id() {
            return "every-field";
        }

        @Override
        public String description() {
            return "Reports every field of an entry.";
        }

        @Override
        public List<Finding> scan(BibEntry entry) {
            return entry.getFields().stream()
                        .filter(field -> field != InternalField.KEY_FIELD)
                        .map(field -> new Finding(this, entry, Optional.of(field), "reported", Optional.empty()))
                        .toList();
        }
    };

    private static final String SLOPPY = """
            @ARTICLE{key,
            author = " Doe, Jane ",
                YEAR="2024"
            }
            """;

    private static final FieldPreferences FIELD_PREFERENCES = new FieldPreferences(true, List.of(), List.of());

    private ImportFormatPreferences importFormatPreferences;

    @BeforeEach
    void setUp() {
        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.fieldPreferences()).thenReturn(FIELD_PREFERENCES);
    }

    @Test
    void repairsWhatTheRulesReport() throws IOException {
        List<BibEntry> entries = parse(SLOPPY);

        new JabFix(RuleSet.all(FIELD_PREFERENCES)).apply(entries);

        assertEquals("Doe, Jane", entries.getFirst().getField(StandardField.AUTHOR).orElseThrow());
    }

    @Test
    void reportsWhatItRepaired() throws IOException {
        JabFixResult result = new JabFix(RuleSet.all(FIELD_PREFERENCES)).apply(parse(SLOPPY));

        assertEquals(List.of("surrounding-whitespace"),
                result.findings().stream().map(finding -> finding.rule().id()).toList());
    }

    @Test
    void everyFindingCarriesTheEntryAndFieldItConcerns() throws IOException {
        Finding finding = new JabFix(RuleSet.all(FIELD_PREFERENCES)).apply(parse(SLOPPY)).findings().getFirst();

        assertEquals("key", finding.citationKey());
        assertEquals("author", finding.field().orElseThrow().getName());
        assertTrue(finding.isFixable());
    }

    /// What a repair changed is what an undo manager collects.
    @Test
    void reportsWhatEveryRepairChanged() throws IOException {
        JabFixResult result = new JabFix(RuleSet.all(FIELD_PREFERENCES)).apply(parse(SLOPPY));

        assertEquals(List.of(" Doe, Jane "), result.changes().stream().map(FieldChange::oldValue).toList());
        assertEquals(List.of("Doe, Jane"), result.changes().stream().map(FieldChange::newValue).toList());
    }

    @Test
    void anEmptyRuleSetChangesNothing() throws IOException {
        List<BibEntry> entries = parse(SLOPPY);

        JabFixResult result = new JabFix(RuleSet.of()).apply(entries);

        assertEquals(" Doe, Jane ", entries.getFirst().getField(StandardField.AUTHOR).orElseThrow());
        assertEquals(List.of(), result.changes());
    }

    /// Scanning stays on the calling thread; only the repairs are handed to the scheduler, which a
    /// GUI uses to keep entry mutations on the JavaFX thread.
    @Test
    void everyRepairGoesThroughTheMutationScheduler() throws IOException {
        List<BibEntry> entries = parse(SLOPPY);
        AtomicInteger scheduled = new AtomicInteger();

        new JabFix(RuleSet.all(FIELD_PREFERENCES)).apply(entries, mutation -> {
            scheduled.incrementAndGet();
            mutation.run();
        });

        assertEquals(1, scheduled.get());
        assertEquals("Doe, Jane", entries.getFirst().getField(StandardField.AUTHOR).orElseThrow());
    }

    /// A magic comment above an entry switches the rule off for it, so nothing is reported and
    /// nothing repaired.
    @Test
    void aMagicCommentSwitchesARuleOffForItsEntry() throws IOException {
        List<BibEntry> entries = parse("""
                % jabref-format-ignore surrounding-whitespace
                @ARTICLE{key,
                author = " Doe, Jane ",
                    YEAR="2024"
                }
                """);

        JabFixResult result = new JabFix(RuleSet.all(FIELD_PREFERENCES)).apply(entries);

        assertEquals(List.of(), result.findings());
        assertEquals(" Doe, Jane ", entries.getFirst().getField(StandardField.AUTHOR).orElseThrow());
    }

    /// `field:rule` covers that one field, and leaves the rest of the entry to the rule.
    @Test
    void aFieldScopedMagicCommentLeavesTheOtherFieldsToTheRule() throws IOException {
        JabFixResult result = new JabFix(RuleSet.of(EVERY_FIELD)).apply(parse("""
                % jabref-format-ignore author:every-field
                @ARTICLE{key,
                author = " Doe, Jane ",
                title = " A Title "
                }
                """));

        assertEquals(Set.of("title"), reportedFields(result));
    }

    @Test
    void aMagicCommentCoversTheFieldsItListsAndTheOnesItsRegexMatches() throws IOException {
        JabFixResult result = new JabFix(RuleSet.of(EVERY_FIELD)).apply(parse("""
                % jabref-format-ignore year,/.*title/:every-field
                @ARTICLE{key,
                author = "Doe, Jane",
                title = "A Title",
                booktitle = "A Book",
                year = "2024"
                }
                """));

        assertEquals(Set.of("author"), reportedFields(result));
    }

    /// BibTeX allows a colon in a field name, where the magic comment otherwise separates fields from rules.
    @Test
    void aMagicCommentNamesAFieldWhoseNameContainsAColon() throws IOException {
        JabFixResult result = new JabFix(RuleSet.of(EVERY_FIELD)).apply(parse("""
                % jabref-format-ignore note:de:every-field
                @ARTICLE{key,
                author = "Doe, Jane",
                note:de = "eine Notiz"
                }
                """));

        assertEquals(Set.of("author"), reportedFields(result));
    }

    /// A comment naming no rule of the run switches nothing off, which is reported rather than
    /// passed over: the entry is repaired as if the comment were not there.
    @Test
    void aMagicCommentThatNamesNoRuleIsReported() throws IOException {
        List<BibEntry> entries = parse("""
                % jabref-format-ignore surounding-whitespace
                @ARTICLE{key,
                author = " Doe, Jane ",
                    YEAR="2024"
                }
                """);

        JabFixResult result = new JabFix(RuleSet.all(FIELD_PREFERENCES)).apply(entries);

        assertEquals(List.of("magic-comment", "surrounding-whitespace"),
                result.findings().stream().map(finding -> finding.rule().id()).toList());
        assertEquals("Doe, Jane", entries.getFirst().getField(StandardField.AUTHOR).orElseThrow());
    }

    @Test
    void aMagicCommentCanSwitchOffTheReportAboutItself() throws IOException {
        JabFixResult result = new JabFix(RuleSet.all(FIELD_PREFERENCES)).apply(parse("""
                % jabref-format-ignore surounding-whitespace magic-comment
                @ARTICLE{key,
                author = " Doe, Jane ",
                    YEAR="2024"
                }
                """));

        assertEquals(List.of("surrounding-whitespace"),
                result.findings().stream().map(finding -> finding.rule().id()).toList());
    }

    private static Set<String> reportedFields(JabFixResult result) {
        return result.findings().stream()
                     .map(finding -> finding.field().orElseThrow().getName())
                     .collect(Collectors.toSet());
    }

    private List<BibEntry> parse(String bibtex) throws IOException {
        return new BibtexParser(importFormatPreferences)
                .parse(Reader.of(bibtex))
                .getDatabaseContext()
                .getEntries();
    }
}
