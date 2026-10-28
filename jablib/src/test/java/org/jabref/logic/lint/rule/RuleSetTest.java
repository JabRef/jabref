package org.jabref.logic.lint.rule;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.metadata.LintSettings;
import org.jabref.model.metadata.MetaData;
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
    void builtInContainsEveryBuiltInRule() {
        assertEquals(List.of("surrounding-whitespace", "repeated-whitespace", "entry-parentheses", "magic-comment"), RuleSet.builtIn(FIELD_PREFERENCES).ids());
    }

    @Test
    void withoutSwitchesTheNamedRuleOff() throws UnknownRuleException {
        assertEquals(List.of("repeated-whitespace", "entry-parentheses", "magic-comment"),
                RuleSet.builtIn(FIELD_PREFERENCES).without(List.of("surrounding-whitespace")).ids());
    }

    @Test
    void withoutNothingChangesNothing() throws UnknownRuleException {
        assertEquals(RuleSet.builtIn(FIELD_PREFERENCES).ids(), RuleSet.builtIn(FIELD_PREFERENCES).without(List.of()).ids());
    }

    /// A misspelled id must not pass silently -- otherwise a user believes a rule is off while it
    /// goes on running.
    @Test
    void withoutRejectsAnIdThatNamesNoRule() {
        UnknownRuleException exception = assertThrows(UnknownRuleException.class,
                () -> RuleSet.builtIn(FIELD_PREFERENCES).without(List.of("surounding-whitespace", "surrounding-whitespace")));

        assertEquals(List.of("surounding-whitespace"), exception.getUnknownIds());
        assertEquals(List.of("surrounding-whitespace", "repeated-whitespace", "entry-parentheses", "magic-comment"), exception.getKnownIds());
    }

    @Test
    void withAppendsARuleFromOutsideThisModule() {
        assertEquals(List.of("surrounding-whitespace", "repeated-whitespace", "entry-parentheses", "magic-comment", "external"),
                RuleSet.builtIn(FIELD_PREFERENCES).with(EXTERNAL).ids());
    }

    @Test
    void anExternalRuleCanBeSwitchedOffByIdLikeAnyOther() throws UnknownRuleException {
        assertEquals(List.of("surrounding-whitespace", "repeated-whitespace", "entry-parentheses", "magic-comment"),
                RuleSet.builtIn(FIELD_PREFERENCES).with(EXTERNAL).without(List.of("external")).ids());
    }

    @Test
    void asConfiguredByKeepsOnlyTheRulesTheLibraryNames() {
        LintSettings settings = LintSettings.of(RuleSelector.of("repeated-whitespace"), RuleMode.FIX);

        assertEquals(List.of("repeated-whitespace"),
                RuleSet.builtIn(FIELD_PREFERENCES).asConfiguredBy(settings).ids());
    }

    /// Saying it out loud is the same as not naming the rule at all.
    @Test
    void asConfiguredByDropsARuleTheLibrarySwitchedOff() {
        LintSettings settings = LintSettings.of(RuleSelector.of("surrounding-whitespace"), RuleMode.OFF)
                                            .and(RuleSelector.of("repeated-whitespace"), RuleMode.FIX);

        assertEquals(List.of("repeated-whitespace"),
                RuleSet.builtIn(FIELD_PREFERENCES).asConfiguredBy(settings).ids());
    }

    /// A checked rule stays in the run under its own id -- it only stops repairing.
    @Test
    void asConfiguredByKeepsACheckedRuleButTakesItsRepairAway() {
        LintSettings settings = LintSettings.of(RuleSelector.of("surrounding-whitespace"), RuleMode.CHECK);
        BibEntry entry = new BibEntry(StandardEntryType.Article).withField(StandardField.TITLE, " A Title ");

        List<Finding> findings = RuleSet.builtIn(FIELD_PREFERENCES).asConfiguredBy(settings).rules().getFirst().scan(entry);

        assertEquals(List.of("surrounding-whitespace"), findings.stream().map(finding -> finding.rule().id()).toList());
        assertEquals(List.of(false), findings.stream().map(Finding::isFixable).toList());
    }

    /// A rule repairs where the library asks it to, which is what it does not do on its own.
    @Test
    void asConfiguredByLeavesTheRepairOfARuleTheLibraryWantsFixed() {
        LintSettings settings = LintSettings.of(RuleSelector.of("surrounding-whitespace"), RuleMode.FIX);
        BibEntry entry = new BibEntry(StandardEntryType.Article).withField(StandardField.TITLE, " A Title ");

        List<Finding> findings = RuleSet.builtIn(FIELD_PREFERENCES).asConfiguredBy(settings)
                                        .rules().getFirst().scan(entry);

        assertEquals(List.of(true), findings.stream().map(Finding::isFixable).toList());
    }

    /// A library that names nothing has nothing done to it.
    @Test
    void asConfiguredByRunsNoRuleTheLibraryDoesNotName() {
        assertEquals(List.of(), RuleSet.builtIn(FIELD_PREFERENCES)
                                       .asConfiguredBy(new LintSettings(new LinkedHashMap<>()))
                                       .ids());
    }

    /// The configuration is what says there is such a Save Action at all.
    @Test
    void configuredByBuildsASaveActionTheSettingsState() {
        LintSettings settings = LintSettings.of(RuleSelector.on(StandardField.TITLE, "lower-case"), RuleMode.FIX);

        assertEquals(List.of("lower-case"), configuredBy(settings).ids());
    }

    /// A Save Action switched off states no Save Action.
    @Test
    void configuredByStatesNoSaveActionForAnEntryThatIsSwitchedOff() {
        LintSettings settings = LintSettings.of(RuleSelector.on(StandardField.TITLE, "lower-case"), RuleMode.OFF)
                                            .and(RuleSelector.of("surrounding-whitespace"), RuleMode.FIX);

        assertEquals(List.of("surrounding-whitespace"), configuredBy(settings).ids());
    }

    /// A save has always applied the Save Actions before its own cleanups.
    @Test
    void configuredByPutsTheSaveActionsFirst() {
        LintSettings settings = LintSettings.of(RuleSelector.of("surrounding-whitespace"), RuleMode.FIX)
                                            .and(RuleSelector.on(StandardField.TITLE, "upper-case"), RuleMode.FIX);

        assertEquals(List.of("upper-case", "surrounding-whitespace"), configuredBy(settings).ids());
    }

    /// `title:lower-case` next to `lower-case` says how far the one Save Action goes on the title; a
    /// second Save Action on the title would report the same thing twice.
    @Test
    void configuredByStatesOneSaveActionWhereAFieldEntryOnlyNarrowsItsMode() {
        LintSettings settings = LintSettings.of(RuleSelector.of("lower-case"), RuleMode.FIX)
                                            .and(RuleSelector.on(StandardField.TITLE, "lower-case"), RuleMode.CHECK);
        BibEntry entry = new BibEntry(StandardEntryType.Article).withField(StandardField.TITLE, "A Title");

        List<Finding> findings = scan(configuredBy(settings), entry);

        assertEquals(List.of(Optional.of(StandardField.TITLE)), findings.stream().map(Finding::field).toList());
        assertEquals(List.of(false), findings.stream().map(Finding::isFixable).toList());
    }

    /// Every text field is not every field: a field that holds no text needs a Save Action of its own.
    @Test
    void configuredByKeepsASaveActionOnAFieldTheBroaderOneDoesNotVisit() {
        LintSettings settings = LintSettings.of(RuleSelector.on(InternalField.INTERNAL_ALL_TEXT_FIELDS_FIELD, "lower-case"), RuleMode.FIX)
                                            .and(RuleSelector.on(StandardField.DOI, "lower-case"), RuleMode.FIX);
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "A Title")
                .withField(StandardField.DOI, "10.1000/ABC");

        List<Finding> findings = scan(configuredBy(settings), entry);

        assertEquals(List.of(Optional.of(StandardField.TITLE), Optional.of(StandardField.DOI)),
                findings.stream().map(Finding::field).toList());
    }

    @Test
    void configuredByStatesOneSaveActionWhereEveryFieldCoversEveryTextField() {
        LintSettings settings = LintSettings.of(RuleSelector.of("lower-case"), RuleMode.FIX)
                                            .and(RuleSelector.on(InternalField.INTERNAL_ALL_TEXT_FIELDS_FIELD, "lower-case"), RuleMode.CHECK);

        assertEquals(List.of("lower-case"), configuredBy(settings).ids());
    }

    /// An entry naming a rule JabFix ships with is not a Save Action, so nothing is built from it.
    @Test
    void configuredByBuildsNoSaveActionFromARuleThatIsNotAFormatter() {
        LintSettings settings = LintSettings.of(RuleSelector.of("surrounding-whitespace"), RuleMode.FIX);

        assertEquals(List.of("surrounding-whitespace"), configuredBy(settings).ids());
    }

    /// Off by default: settings naming nothing run nothing.
    @Test
    void configuredByRunsNothingForSettingsThatNameNothing() {
        assertEquals(List.of(), configuredBy(LintSettings.NONE).ids());
    }

    /// Every formatter a Save Action can use is a rule that can be named, not only the ones a
    /// library happens to have.
    @Test
    void knownHasTheBuiltInRulesAndOneRulePerFormatter() {
        List<String> knownIds = RuleSet.known(FIELD_PREFERENCES).ids();

        assertEquals(RuleSet.builtIn(FIELD_PREFERENCES).ids(), knownIds.subList(0, 4));
        assertEquals(List.copyOf(SaveActionIds.ids()), knownIds.subList(4, knownIds.size()));
    }

    private static RuleSet configuredBy(LintSettings settings) {
        return RuleSet.configuredBy(settings, new MetaData(), FIELD_PREFERENCES);
    }

    private static List<Finding> scan(RuleSet rules, BibEntry entry) {
        return rules.rules().stream()
                    .flatMap(rule -> rule.scan(entry).stream())
                    .toList();
    }

    /// The settings may have been written by a JabFix that knows a rule this one does not.
    @Test
    void asConfiguredByPassesOverAnIdThatNamesNoRule() {
        LintSettings settings = LintSettings.of(RuleSelector.of("rule-of-a-newer-jabfix"), RuleMode.FIX)
                                            .and(RuleSelector.of("surrounding-whitespace"), RuleMode.FIX);

        assertEquals(List.of("surrounding-whitespace"),
                RuleSet.builtIn(FIELD_PREFERENCES).asConfiguredBy(settings).ids());
    }

    /// Unlike a library's own settings: what a user just typed is held against the rules.
    @Test
    void rejectUnknownRejectsAnIdThatNamesNoRule() {
        UnknownRuleException exception = assertThrows(UnknownRuleException.class,
                () -> RuleSet.builtIn(FIELD_PREFERENCES).rejectUnknown(List.of("surounding-whitespace")));

        assertEquals(List.of("surounding-whitespace"), exception.getUnknownIds());
    }

    @Test
    void ofTakesExactlyTheRulesGiven() {
        assertEquals(List.of("external"), RuleSet.of(EXTERNAL).ids());
    }
}
