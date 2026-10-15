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
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.metadata.LintSettings;
import org.jabref.model.metadata.RuleMode;
import org.jabref.model.metadata.RuleSelector;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SaveActionsMigrationTest {

    private static final FieldPreferences FIELD_PREFERENCES = new FieldPreferences(true, List.of(), List.of());

    private static final FieldFormatterCleanup TITLE_LOWER_CASE = new FieldFormatterCleanup(StandardField.TITLE, new LowerCaseFormatter());
    private static final FieldFormatterCleanup TITLE_UPPER_CASE = new FieldFormatterCleanup(StandardField.TITLE, new UpperCaseFormatter());
    private static final FieldFormatterCleanup JOURNAL_UPPER_CASE = new FieldFormatterCleanup(StandardField.JOURNAL, new UpperCaseFormatter());
    private static final FieldFormatterCleanup TEXT_FIELDS_LOWER_CASE = new FieldFormatterCleanup(InternalField.INTERNAL_ALL_TEXT_FIELDS_FIELD, new LowerCaseFormatter());

    private static BibDatabaseContext library(Optional<FieldFormatterCleanupActions> saveActions, Optional<LintSettings> formatting) {
        BibDatabaseContext databaseContext = new BibDatabaseContext();
        saveActions.ifPresent(databaseContext.getMetaData()::setSaveActions);
        formatting.ifPresent(databaseContext.getMetaData()::setFormatting);
        return databaseContext;
    }

    private static Optional<FieldFormatterCleanupActions> item(boolean enabled, FieldFormatterCleanup... actions) {
        return Optional.of(new FieldFormatterCleanupActions(enabled, List.of(actions)));
    }

    private static LintSettings migrated(BibDatabaseContext databaseContext) {
        return SaveActionsMigration.migrated(databaseContext, FIELD_PREFERENCES).orElseThrow();
    }

    /// A library that had no configuration was formatted by every rule it has, so the rules JabFix
    /// ships with are written down next to its Save Actions.
    @Test
    void everySaveActionBecomesAnEntryNamingItsFieldFollowedByTheRulesJabFixShipsWith() {
        BibDatabaseContext databaseContext = library(item(true, TITLE_LOWER_CASE, JOURNAL_UPPER_CASE), Optional.empty());

        assertEquals(List.of(
                        RuleSelector.on(StandardField.TITLE, "lower-case"),
                        RuleSelector.on(StandardField.JOURNAL, "upper-case"),
                        RuleSelector.of("surrounding-whitespace"),
                        RuleSelector.of("repeated-whitespace")),
                List.copyOf(migrated(databaseContext).ruleModes().keySet()));
    }

    /// The library said which Save Actions it has and that it does not run them; both halves survive.
    @Test
    void anItemThatWasSwitchedOffBecomesEntriesSayingSo() {
        BibDatabaseContext databaseContext = library(item(false, TITLE_LOWER_CASE, JOURNAL_UPPER_CASE), Optional.empty());

        LintSettings migrated = migrated(databaseContext);

        assertEquals(List.of(RuleMode.OFF, RuleMode.OFF), List.of(
                migrated.modeOf("lower-case", Optional.of(StandardField.TITLE)),
                migrated.modeOf("upper-case", Optional.of(StandardField.JOURNAL))));
    }

    @Test
    void theItemIsGoneAfterwards() {
        BibDatabaseContext databaseContext = library(item(true, TITLE_LOWER_CASE), Optional.empty());

        SaveActionsMigration.migrate(databaseContext, FIELD_PREFERENCES);

        assertEquals(Optional.empty(), databaseContext.getMetaData().getSaveActions());
    }

    /// Nothing to move gives the library no configuration: one that is there but empty would switch
    /// every rule off.
    @Test
    void aLibraryWithoutSaveActionsKeepsHavingNoConfiguration() {
        BibDatabaseContext databaseContext = library(Optional.empty(), Optional.empty());

        SaveActionsMigration.migrate(databaseContext, FIELD_PREFERENCES);

        assertEquals(Optional.empty(), databaseContext.getMetaData().getFormatting());
    }

    @Test
    void anItemWithoutSaveActionsIsDroppedAndGivesTheLibraryNoConfiguration() {
        BibDatabaseContext databaseContext = library(item(true), Optional.empty());

        SaveActionsMigration.migrate(databaseContext, FIELD_PREFERENCES);

        assertEquals(List.of(Optional.empty(), Optional.empty()),
                List.of(databaseContext.getMetaData().getSaveActions(), databaseContext.getMetaData().getFormatting()));
    }

    /// `lower-case` said how far the Save Action went; after the move it would state a Save Action
    /// on every field, so its mode moves onto the one the item had instead.
    @Test
    void theModeTheConfigurationGaveASaveActionMovesOntoItsEntry() {
        BibDatabaseContext databaseContext = library(item(true, TITLE_LOWER_CASE),
                Optional.of(LintSettings.of(RuleSelector.of("lower-case"), RuleMode.CHECK)));

        assertEquals(LintSettings.of(RuleSelector.on(StandardField.TITLE, "lower-case"), RuleMode.CHECK),
                migrated(databaseContext));
    }

    /// The entry gave a mode to a Save Action the library does not have, and would state one after
    /// the move.
    @Test
    void anEntryAboutAFieldNoSaveActionVisitsIsDropped() {
        BibDatabaseContext databaseContext = library(item(true, TITLE_LOWER_CASE),
                Optional.of(LintSettings.of(RuleSelector.on(StandardField.AUTHOR, "lower-case"), RuleMode.FIX)));

        assertEquals(LintSettings.of(RuleSelector.on(StandardField.TITLE, "lower-case"), RuleMode.OFF),
                migrated(databaseContext));
    }

    /// The point of the whole exercise: a run of the migrated library applies what it applied
    /// before, to the values and in what it reports.
    @ParameterizedTest
    @MethodSource
    void aMigratedLibraryIsFormattedTheSame(Optional<FieldFormatterCleanupActions> saveActions, Optional<LintSettings> formatting) {
        BibDatabaseContext before = library(saveActions, formatting);
        BibDatabaseContext after = library(saveActions, formatting);

        SaveActionsMigration.migrate(after, FIELD_PREFERENCES);

        assertEquals(run(before), run(after));
    }

    static Stream<Arguments> aMigratedLibraryIsFormattedTheSame() {
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
                        Optional.of(LintSettings.of(RuleSelector.on(StandardField.TITLE, "lower-case"), RuleMode.FIX))));
    }

    /// What a run that names no rule of its own applies: what the library says, or every rule it has
    /// where it says nothing.
    private static Run run(BibDatabaseContext databaseContext) {
        RuleSet rules = RuleSet.forLibrary(databaseContext, FIELD_PREFERENCES);
        LintSettings settings = databaseContext.getMetaData().getFormatting()
                                               .orElseGet(() -> LintSettings.repairing(rules.ids()));
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, " Knuth, Donald E. ")
                .withField(StandardField.TITLE, "Some  TITLE")
                .withField(StandardField.JOURNAL, "Some Journal")
                .withField(StandardField.DOI, "10.1000/ABC");

        JabFixResult result = new JabFix(rules.asConfiguredBy(settings)).apply(List.of(entry));

        return new Run(entry, result.findings().stream()
                                    .map(finding -> new Reported(finding.rule().id(), finding.field(), finding.message(), finding.isFixable()))
                                    .toList());
    }

    private record Run(BibEntry entry, List<Reported> findings) {
    }

    private record Reported(String ruleId, Optional<Field> field, String message, boolean fixable) {
    }
}
