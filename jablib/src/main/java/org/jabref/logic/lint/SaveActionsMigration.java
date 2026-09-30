package org.jabref.logic.lint;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;

import org.jabref.logic.cleanup.FieldFormatterCleanup;
import org.jabref.logic.cleanup.FieldFormatterCleanupActions;
import org.jabref.logic.lint.rule.SaveActionIds;
import org.jabref.model.metadata.LintSettings;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.metadata.RuleMode;
import org.jabref.model.metadata.RuleSelector;

import org.jspecify.annotations.NullMarked;

/// Moves a library's Save Actions into its formatting configuration.
///
/// A library keeps its `saveActions` item for as long as it has no formatting configuration: it
/// goes on working, and a hand edit to it goes on being read. Once the library is configured, the
/// Save Actions belong in that one place -- two places that can disagree about what a save does is
/// exactly what the configuration replaces -- so they are moved and the item is dropped.
///
/// A Save Action becomes one entry, `field:formatter-id`, always naming the field, so that what is
/// written here is what [org.jabref.logic.lint.rule.RuleSet#forLibrary] builds back.
///
/// An item that was switched off becomes entries saying [RuleMode#OFF] out loud, rather than no
/// entries at all: the library said which Save Actions it has and that it does not run them, and
/// both halves of that survive.
@NullMarked
public final class SaveActionsMigration {

    private SaveActionsMigration() {
    }

    /// The configuration the library should carry once its Save Actions have moved into it.
    ///
    /// An entry the configuration already makes about a Save Action is left as it stands: the
    /// configuration is what the library says about itself, and migrating must not overwrite what
    /// somebody put there.
    public static LintSettings migrated(MetaData metaData) {
        LintSettings configured = metaData.getFormatting().orElseGet(() -> new LintSettings(new LinkedHashMap<>()));
        List<FieldFormatterCleanup> actions = metaData.getSaveActions()
                                                      .map(FieldFormatterCleanupActions::getConfiguredActions)
                                                      .orElse(List.of());
        if (actions.isEmpty()) {
            return configured;
        }

        RuleMode mode = metaData.getSaveActions()
                                .filter(FieldFormatterCleanupActions::isEnabled)
                                .map(_ -> RuleMode.FIX)
                                .orElse(RuleMode.OFF);

        SequencedMap<RuleSelector, RuleMode> moved = new LinkedHashMap<>(configured.ruleModes());
        actions.forEach(action -> moved.putIfAbsent(
                RuleSelector.on(action.getField(), SaveActionIds.idOf(action.getFormatter())), mode));
        return new LintSettings(moved, configured.unreadRules(), configured.unreadKeys());
    }

    /// Moves the Save Actions into the library's formatting configuration and drops the item.
    public static void migrate(MetaData metaData) {
        LintSettings migrated = migrated(metaData);
        metaData.setFormatting(migrated);
        metaData.clearSaveActions();
    }
}
