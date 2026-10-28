package org.jabref.logic.lint;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.cleanup.FieldFormatterCleanup;
import org.jabref.logic.cleanup.FieldFormatterCleanupActions;
import org.jabref.logic.formatter.casechanger.LowerCaseFormatter;
import org.jabref.logic.formatter.casechanger.UpperCaseFormatter;
import org.jabref.logic.lint.rule.RuleSet;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.metadata.LintSettings;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.metadata.RuleMode;
import org.jabref.model.metadata.RuleSelector;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JabFixConfigurationTest {

    private static final FieldPreferences FIELD_PREFERENCES = new FieldPreferences(true, List.of(), List.of());

    private static final FieldFormatterCleanup TITLE_LOWER_CASE = new FieldFormatterCleanup(StandardField.TITLE, new LowerCaseFormatter());
    private static final FieldFormatterCleanup TITLE_UPPER_CASE = new FieldFormatterCleanup(StandardField.TITLE, new UpperCaseFormatter());
    private static final FieldFormatterCleanup JOURNAL_UPPER_CASE = new FieldFormatterCleanup(StandardField.JOURNAL, new UpperCaseFormatter());
    private static final FieldFormatterCleanup TEXT_FIELDS_LOWER_CASE = new FieldFormatterCleanup(InternalField.INTERNAL_ALL_TEXT_FIELDS_FIELD, new LowerCaseFormatter());

    private static MetaData library(Optional<FieldFormatterCleanupActions> saveActions, Optional<LintSettings> formatting) {
        MetaData metaData = new MetaData();
        saveActions.ifPresent(metaData::setSaveActions);
        formatting.ifPresent(metaData::setFormatting);
        return metaData;
    }

    private static Optional<FieldFormatterCleanupActions> item(boolean enabled, FieldFormatterCleanup... saveActions) {
        return Optional.of(new FieldFormatterCleanupActions(enabled, List.of(saveActions)));
    }

    @Test
    void aLibraryThatSaysNothingAppliesNothing() {
        assertEquals(LintSettings.NONE, JabFixConfiguration.applied(new MetaData(), LintSettings.NONE));
    }

    @Test
    void theModeIsExpandedIntoItsRules() {
        MetaData metaData = library(Optional.empty(), Optional.of(LintSettings.NONE.withMode(JabFixMode.WHITESPACE.getKey())));

        assertEquals(JabFixMode.WHITESPACE.getSettings().ruleModes(),
                JabFixConfiguration.applied(metaData, LintSettings.NONE).ruleModes());
    }

    /// A rule the library names is more specific than its mode, so it outranks it.
    @Test
    void theLibrarysRulesOutrankItsMode() {
        MetaData metaData = library(Optional.empty(), Optional.of(
                LintSettings.of(RuleSelector.of("surrounding-whitespace"), RuleMode.CHECK).withMode(JabFixMode.WHITESPACE.getKey())));

        LintSettings applied = JabFixConfiguration.applied(metaData, LintSettings.NONE);

        assertEquals(RuleMode.CHECK, applied.modeOf("surrounding-whitespace", Optional.of(StandardField.TITLE)));
        assertEquals(RuleMode.FIX, applied.modeOf("repeated-whitespace", Optional.of(StandardField.TITLE)));
    }

    @Test
    void theRunsModeTakesThePlaceOfTheLibrarys() {
        MetaData metaData = library(Optional.empty(), Optional.of(LintSettings.NONE.withMode(JabFixMode.WHITESPACE.getKey())));

        LintSettings applied = JabFixConfiguration.applied(metaData, LintSettings.NONE.withMode(JabFixMode.REPORT.getKey()));

        assertEquals(RuleMode.CHECK, applied.modeOf("surrounding-whitespace", Optional.of(StandardField.TITLE)));
    }

    /// The run's mode is broader than a rule the library names, so the rule goes on applying.
    @Test
    void theLibrarysRulesOutrankTheRunsMode() {
        MetaData metaData = library(Optional.empty(), Optional.of(LintSettings.of(RuleSelector.of("surrounding-whitespace"), RuleMode.OFF)));

        LintSettings applied = JabFixConfiguration.applied(metaData, LintSettings.NONE.withMode(JabFixMode.WHITESPACE.getKey()));

        assertEquals(RuleMode.OFF, applied.modeOf("surrounding-whitespace", Optional.of(StandardField.TITLE)));
        assertEquals(RuleMode.FIX, applied.modeOf("repeated-whitespace", Optional.of(StandardField.TITLE)));
    }

    /// What the library names and the run does not still applies.
    @Test
    void theRunsRulesOutrankOnlyTheLibrarysRulesTheyName() {
        MetaData metaData = library(Optional.empty(), Optional.of(
                LintSettings.of(RuleSelector.of("surrounding-whitespace"), RuleMode.FIX)
                            .and(RuleSelector.on(StandardField.TITLE, "repeated-whitespace"), RuleMode.FIX)));

        LintSettings applied = JabFixConfiguration.applied(metaData, LintSettings.of(RuleSelector.of("repeated-whitespace"), RuleMode.CHECK));

        assertEquals(RuleMode.FIX, applied.modeOf("surrounding-whitespace", Optional.of(StandardField.TITLE)));
        assertEquals(RuleMode.CHECK, applied.modeOf("repeated-whitespace", Optional.of(StandardField.TITLE)));
    }

    @Test
    void aModeThisJabFixDoesNotKnowIsPassedOver() {
        MetaData metaData = library(Optional.empty(), Optional.of(
                LintSettings.of(RuleSelector.of("surrounding-whitespace"), RuleMode.FIX).withMode("mode-of-a-newer-jabfix")));

        assertEquals(List.of(RuleSelector.of("surrounding-whitespace")),
                List.copyOf(JabFixConfiguration.applied(metaData, LintSettings.NONE).ruleModes().keySet()));
    }

    /// Save Actions are rule settings of the library, laid over its mode.
    @Test
    void theSaveActionsApplyBesideTheMode() {
        MetaData metaData = library(
                item(true, new FieldFormatterCleanup(StandardField.PAGES, new LowerCaseFormatter())),
                Optional.of(LintSettings.NONE.withMode(JabFixMode.WHITESPACE.getKey())));

        LintSettings applied = JabFixConfiguration.applied(metaData, LintSettings.NONE);

        assertEquals(RuleMode.FIX, applied.modeOf("lower-case", Optional.of(StandardField.PAGES)));
        assertEquals(RuleMode.FIX, applied.modeOf("surrounding-whitespace", Optional.of(StandardField.PAGES)));
    }

    @Test
    void theLibrarysRulesOutrankItsSaveActions() {
        MetaData metaData = library(item(true, TITLE_LOWER_CASE),
                Optional.of(LintSettings.of(RuleSelector.on(StandardField.TITLE, "lower-case"), RuleMode.CHECK)));

        assertEquals(RuleMode.CHECK, JabFixConfiguration.applied(metaData, LintSettings.NONE)
                                                        .modeOf("lower-case", Optional.of(StandardField.TITLE)));
    }

    @Test
    void saveActionsSwitchedOffStateNothing() {
        assertEquals(LintSettings.NONE, JabFixConfiguration.applied(library(item(false, TITLE_LOWER_CASE), Optional.empty()), LintSettings.NONE));
    }

    @Test
    void configuringALibraryMovesItsSaveActionsBeneathTheNewSettings() {
        MetaData metaData = library(item(true, TITLE_LOWER_CASE, JOURNAL_UPPER_CASE), Optional.empty());

        JabFixConfiguration.configure(metaData, LintSettings.of(RuleSelector.on(StandardField.JOURNAL, "upper-case"), RuleMode.CHECK)
                                                            .withMode(JabFixMode.WHITESPACE.getKey()));

        assertEquals(Optional.of(LintSettings.of(RuleSelector.on(StandardField.TITLE, "lower-case"), RuleMode.FIX)
                                             .and(RuleSelector.on(StandardField.JOURNAL, "upper-case"), RuleMode.CHECK)
                                             .withMode(JabFixMode.WHITESPACE.getKey())),
                metaData.getFormatting());
        assertEquals(Optional.empty(), metaData.getSaveActions());
    }

    /// Nothing to state gives the library no configuration: one that is there but empty is still a
    /// configuration, and would be written to the file.
    @Test
    void configuringALibraryWithNothingToStateGivesItNoConfiguration() {
        MetaData metaData = new MetaData();

        JabFixConfiguration.configure(metaData, metaData.getFormatting().orElse(LintSettings.NONE));

        assertEquals(Optional.empty(), metaData.getFormatting());
    }

    /// Emptied, it still says that the library enables nothing.
    @Test
    void configuringALibraryThatHasAConfigurationKeepsOne() {
        MetaData metaData = library(Optional.empty(), Optional.of(LintSettings.NONE.withMode(JabFixMode.WHITESPACE.getKey())));

        JabFixConfiguration.configure(metaData, LintSettings.NONE);

        assertEquals(Optional.of(LintSettings.NONE), metaData.getFormatting());
    }

    @Test
    void configuringDropsAnItemThatSwitchesTheSaveActionsOff() {
        MetaData metaData = library(item(false, TITLE_LOWER_CASE), Optional.empty());

        JabFixConfiguration.configure(metaData, metaData.getFormatting().orElse(LintSettings.NONE));

        assertEquals(Optional.empty(), metaData.getSaveActions());
        assertEquals(Optional.empty(), metaData.getFormatting());
    }

    /// The point of the whole exercise: a run of the migrated library applies what it applied
    /// before, to the values and in what it reports.
    @ParameterizedTest
    @MethodSource
    void aMigratedLibraryIsFormattedTheSame(Optional<FieldFormatterCleanupActions> saveActions, Optional<LintSettings> formatting) {
        MetaData before = library(saveActions, formatting);
        MetaData after = library(saveActions, formatting);

        JabFixConfiguration.configure(after, after.getFormatting().orElse(LintSettings.NONE));

        assertEquals(run(before), run(after));
    }

    static Stream<Arguments> aMigratedLibraryIsFormattedTheSame() {
        String whitespace = JabFixMode.WHITESPACE.getKey();
        return Stream.of(
                Arguments.of(Optional.empty(), Optional.empty()),
                Arguments.of(item(true), Optional.empty()),
                Arguments.of(item(true, TITLE_LOWER_CASE, JOURNAL_UPPER_CASE), Optional.empty()),
                Arguments.of(item(false, TITLE_LOWER_CASE, JOURNAL_UPPER_CASE), Optional.empty()),
                Arguments.of(item(true, TITLE_LOWER_CASE, TITLE_UPPER_CASE), Optional.empty()),
                Arguments.of(item(true, TITLE_UPPER_CASE, TITLE_LOWER_CASE), Optional.empty()),
                Arguments.of(item(true, TITLE_LOWER_CASE),
                        Optional.of(LintSettings.of(RuleSelector.of("lower-case"), RuleMode.CHECK))),
                Arguments.of(item(true, TITLE_LOWER_CASE),
                        Optional.of(LintSettings.of(RuleSelector.of("surrounding-whitespace"), RuleMode.FIX))),
                Arguments.of(item(true, TITLE_LOWER_CASE),
                        Optional.of(LintSettings.of(RuleSelector.on(StandardField.AUTHOR, "lower-case"), RuleMode.FIX))),
                Arguments.of(item(false, TITLE_LOWER_CASE),
                        Optional.of(LintSettings.of(RuleSelector.of("lower-case"), RuleMode.FIX))),
                Arguments.of(item(true, TEXT_FIELDS_LOWER_CASE),
                        Optional.of(LintSettings.of(RuleSelector.of("lower-case"), RuleMode.FIX)
                                                .and(RuleSelector.on(StandardField.TITLE, "lower-case"), RuleMode.CHECK))),
                Arguments.of(item(true, TEXT_FIELDS_LOWER_CASE),
                        Optional.of(LintSettings.of(RuleSelector.on(StandardField.TITLE, "lower-case"), RuleMode.FIX))),
                Arguments.of(item(true, TITLE_LOWER_CASE), Optional.of(LintSettings.NONE.withMode(whitespace))),
                Arguments.of(item(false, TITLE_LOWER_CASE), Optional.of(LintSettings.NONE.withMode(whitespace))),
                Arguments.of(item(true, TITLE_LOWER_CASE),
                        Optional.of(LintSettings.of(RuleSelector.of("surrounding-whitespace"), RuleMode.OFF).withMode(whitespace))));
    }

    /// What a run that asks for nothing of its own applies.
    private static Run run(MetaData metaData) {
        RuleSet rules = RuleSet.configuredBy(JabFixConfiguration.applied(metaData, LintSettings.NONE), metaData, FIELD_PREFERENCES);
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, " Knuth, Donald E. ")
                .withField(StandardField.TITLE, "Some  TITLE")
                .withField(StandardField.JOURNAL, "Some Journal")
                .withField(StandardField.DOI, "10.1000/ABC");

        JabFixResult result = new JabFix(rules).apply(List.of(entry));

        return new Run(entry, result.findings().stream()
                                    .map(finding -> new Reported(finding.rule().id(), finding.field(), finding.message(), finding.isFixable()))
                                    .toList());
    }

    private record Run(BibEntry entry, List<Reported> findings) {
    }

    private record Reported(String ruleId, Optional<Field> field, String message, boolean fixable) {
    }
}
