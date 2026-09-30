package org.jabref.logic.lint;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.cleanup.FieldFormatterCleanup;
import org.jabref.logic.cleanup.FieldFormatterCleanupActions;
import org.jabref.logic.formatter.casechanger.LowerCaseFormatter;
import org.jabref.logic.formatter.casechanger.UpperCaseFormatter;
import org.jabref.logic.lint.rule.RuleSet;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.metadata.LintSettings;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.metadata.RuleMode;
import org.jabref.model.metadata.RuleSelector;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SaveActionsMigrationTest {

    private static final FieldPreferences FIELD_PREFERENCES = new FieldPreferences(true, List.of(), List.of());

    private static MetaData withSaveActions(boolean enabled) {
        MetaData metaData = new MetaData();
        metaData.setSaveActions(new FieldFormatterCleanupActions(enabled, List.of(
                new FieldFormatterCleanup(StandardField.TITLE, new LowerCaseFormatter()),
                new FieldFormatterCleanup(StandardField.JOURNAL, new UpperCaseFormatter()))));
        return metaData;
    }

    @Test
    void everySaveActionBecomesAnEntryNamingItsField() {
        assertEquals(List.of(
                        RuleSelector.on(StandardField.TITLE, "lower-case"),
                        RuleSelector.on(StandardField.JOURNAL, "upper-case")),
                List.copyOf(SaveActionsMigration.migrated(withSaveActions(true)).ruleModes().keySet()));
    }

    @Test
    void theOrderTheSaveActionsWereAppliedInIsKept() {
        MetaData metaData = withSaveActions(true);

        SaveActionsMigration.migrate(metaData);

        assertEquals(List.of(RuleMode.FIX, RuleMode.FIX),
                List.copyOf(metaData.getFormatting().orElseThrow().ruleModes().values()));
    }

    /// The library said which Save Actions it has and that it does not run them; both halves survive.
    @Test
    void anItemThatWasSwitchedOffBecomesEntriesSayingSo() {
        MetaData metaData = withSaveActions(false);

        SaveActionsMigration.migrate(metaData);

        assertEquals(List.of(RuleMode.OFF, RuleMode.OFF),
                List.copyOf(metaData.getFormatting().orElseThrow().ruleModes().values()));
    }

    @Test
    void theItemIsGoneAfterwards() {
        MetaData metaData = withSaveActions(true);

        SaveActionsMigration.migrate(metaData);

        assertEquals(Optional.empty(), metaData.getSaveActions());
    }

    /// The configuration is what the library says about itself, so migrating must not overwrite it.
    @Test
    void anEntryTheConfigurationAlreadyMakesIsLeftAsItStands() {
        MetaData metaData = withSaveActions(true);
        metaData.setFormatting(LintSettings.of(RuleSelector.on(StandardField.TITLE, "lower-case"), RuleMode.CHECK));

        SaveActionsMigration.migrate(metaData);

        assertEquals(RuleMode.CHECK,
                metaData.getFormatting().orElseThrow().modeOf("lower-case", Optional.of(StandardField.TITLE)));
    }

    /// The point of the whole exercise: a migrated library goes on applying what it applied.
    @Test
    void aMigratedLibraryAppliesTheSameRules() {
        BibDatabaseContext before = new BibDatabaseContext();
        before.getMetaData().setSaveActions(withSaveActions(true).getSaveActions().orElseThrow());
        List<String> applied = RuleSet.forLibrary(before, FIELD_PREFERENCES).ids();

        BibDatabaseContext after = new BibDatabaseContext();
        after.getMetaData().setSaveActions(withSaveActions(true).getSaveActions().orElseThrow());
        SaveActionsMigration.migrate(after.getMetaData());

        assertEquals(applied, RuleSet.forLibrary(after, FIELD_PREFERENCES).ids());
    }
}
