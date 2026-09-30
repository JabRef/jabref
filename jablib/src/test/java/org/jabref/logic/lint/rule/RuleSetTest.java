package org.jabref.logic.lint.rule;

import java.util.LinkedHashMap;
import java.util.List;

import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.metadata.LintSettings;
import org.jabref.model.metadata.RuleMode;
import org.jabref.model.metadata.RuleSelector;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RuleSetTest {

    private static final FieldPreferences FIELD_PREFERENCES = new FieldPreferences(true, List.of(), List.of());

    /// Stands in for a rule implemented outside this module.
    private static final Rule EXTERNAL = new Rule() {
        @Override
        public String id() {
            return "external";
        }

        @Override
        public String description() {
            return "A rule that does not ship with JabFix.";
        }

        @Override
        public List<Finding> scan(BibEntry entry) {
            return List.of();
        }
    };

    @Test
    void allContainsEveryBuiltInRule() {
        assertEquals(List.of("surrounding-whitespace", "repeated-whitespace"), RuleSet.all(FIELD_PREFERENCES).ids());
    }

    @Test
    void withoutSwitchesTheNamedRuleOff() throws UnknownRuleException {
        assertEquals(List.of("repeated-whitespace"),
                RuleSet.all(FIELD_PREFERENCES).without(List.of("surrounding-whitespace")).ids());
    }

    @Test
    void withoutNothingChangesNothing() throws UnknownRuleException {
        assertEquals(RuleSet.all(FIELD_PREFERENCES).ids(), RuleSet.all(FIELD_PREFERENCES).without(List.of()).ids());
    }

    /// A misspelled id must not pass silently -- otherwise a user believes a rule is off while it
    /// goes on running.
    @Test
    void withoutRejectsAnIdThatNamesNoRule() {
        UnknownRuleException exception = assertThrows(UnknownRuleException.class,
                () -> RuleSet.all(FIELD_PREFERENCES).without(List.of("surounding-whitespace", "surrounding-whitespace")));

        assertEquals(List.of("surounding-whitespace"), exception.getUnknownIds());
        assertEquals(List.of("surrounding-whitespace", "repeated-whitespace"), exception.getKnownIds());
    }

    @Test
    void withAppendsARuleFromOutsideThisModule() {
        assertEquals(List.of("surrounding-whitespace", "repeated-whitespace", "external"),
                RuleSet.all(FIELD_PREFERENCES).with(EXTERNAL).ids());
    }

    @Test
    void anExternalRuleCanBeSwitchedOffByIdLikeAnyOther() throws UnknownRuleException {
        assertEquals(List.of("surrounding-whitespace", "repeated-whitespace"),
                RuleSet.all(FIELD_PREFERENCES).with(EXTERNAL).without(List.of("external")).ids());
    }

    @Test
    void asConfiguredByKeepsOnlyTheRulesTheLibraryNames() {
        LintSettings settings = LintSettings.of(RuleSelector.of("repeated-whitespace"), RuleMode.FIX);

        assertEquals(List.of("repeated-whitespace"),
                RuleSet.all(FIELD_PREFERENCES).asConfiguredBy(settings).ids());
    }

    /// Saying it out loud is the same as not naming the rule at all.
    @Test
    void asConfiguredByDropsARuleTheLibrarySwitchedOff() {
        LintSettings settings = LintSettings.of(RuleSelector.of("surrounding-whitespace"), RuleMode.OFF)
                                            .and(RuleSelector.of("repeated-whitespace"), RuleMode.FIX);

        assertEquals(List.of("repeated-whitespace"),
                RuleSet.all(FIELD_PREFERENCES).asConfiguredBy(settings).ids());
    }

    /// A checked rule stays in the run under its own id -- it only stops repairing.
    @Test
    void asConfiguredByKeepsACheckedRuleButTakesItsRepairAway() {
        LintSettings settings = LintSettings.of(RuleSelector.of("surrounding-whitespace"), RuleMode.CHECK);
        BibEntry entry = new BibEntry(StandardEntryType.Article).withField(StandardField.TITLE, " A Title ");

        List<Finding> findings = RuleSet.all(FIELD_PREFERENCES).asConfiguredBy(settings).rules().getFirst().scan(entry);

        assertEquals(List.of("surrounding-whitespace"), findings.stream().map(finding -> finding.rule().id()).toList());
        assertEquals(List.of(false), findings.stream().map(Finding::isFixable).toList());
    }

    /// A rule repairs where the library asks it to, which is what it does not do on its own.
    @Test
    void asConfiguredByLeavesTheRepairOfARuleTheLibraryWantsFixed() {
        LintSettings settings = LintSettings.of(RuleSelector.of("surrounding-whitespace"), RuleMode.FIX);
        BibEntry entry = new BibEntry(StandardEntryType.Article).withField(StandardField.TITLE, " A Title ");

        List<Finding> findings = RuleSet.all(FIELD_PREFERENCES).asConfiguredBy(settings)
                                        .rules().getFirst().scan(entry);

        assertEquals(List.of(true), findings.stream().map(Finding::isFixable).toList());
    }

    /// A library that names nothing has nothing done to it.
    @Test
    void asConfiguredByRunsNoRuleTheLibraryDoesNotName() {
        assertEquals(List.of(), RuleSet.all(FIELD_PREFERENCES)
                                       .asConfiguredBy(new LintSettings(new LinkedHashMap<>()))
                                       .ids());
    }

    /// The settings may have been written by a JabFix that knows a rule this one does not.
    @Test
    void asConfiguredByPassesOverAnIdThatNamesNoRule() {
        LintSettings settings = LintSettings.of(RuleSelector.of("rule-of-a-newer-jabfix"), RuleMode.FIX)
                                            .and(RuleSelector.of("surrounding-whitespace"), RuleMode.FIX);

        assertEquals(List.of("surrounding-whitespace"),
                RuleSet.all(FIELD_PREFERENCES).asConfiguredBy(settings).ids());
    }

    /// Unlike a library's own settings: what a user just typed is held against the rules.
    @Test
    void rejectUnknownRejectsAnIdThatNamesNoRule() {
        UnknownRuleException exception = assertThrows(UnknownRuleException.class,
                () -> RuleSet.all(FIELD_PREFERENCES).rejectUnknown(List.of("surounding-whitespace")));

        assertEquals(List.of("surounding-whitespace"), exception.getUnknownIds());
    }

    @Test
    void ofTakesExactlyTheRulesGiven() {
        assertEquals(List.of("external"), RuleSet.of(EXTERNAL).ids());
    }
}
