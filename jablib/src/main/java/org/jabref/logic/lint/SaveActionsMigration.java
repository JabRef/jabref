package org.jabref.logic.lint;

import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.SequencedMap;

import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.cleanup.FieldFormatterCleanup;
import org.jabref.logic.cleanup.FieldFormatterCleanupActions;
import org.jabref.logic.lint.rule.RuleSet;
import org.jabref.logic.lint.rule.SaveActionIds;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.field.Field;
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
/// A run of the library has to apply the same after the move as before it. The same entry does
/// not mean the same on both sides, though: while the item is there, `title:lower-case` only says
/// how far the item's Save Action goes on the title, and once the item is gone, that entry is what
/// states the Save Action at all -- see [RuleSet#forLibrary]. The configuration is therefore
/// rewritten rather than added to:
///
/// - Each Save Action becomes an entry naming its field, `field:formatter-id`, in the order the
///   item applied them, going as far as the library let it go before. One the item switched off
///   becomes [RuleMode#OFF] said out loud: the library said which Save Actions it has and that it
///   does not run them, and both halves of that survive.
/// - An entry about one field a Save Action visits besides its own stays, right after that Save
///   Action, where it goes on saying how far the Save Action goes on that field.
/// - Any other entry naming a formatter is dropped. It gave a mode to a Save Action the library
///   does not have, and after the move it would state one.
/// - Entries about any other rule stay as they were.
///
/// A library without a configuration was formatted by every rule it has, each repairing what it
/// finds, and that is what it is given in writing -- the rules JabFix ships with included, which
/// would otherwise stop running the moment the library names its Save Actions.
@NullMarked
public final class SaveActionsMigration {

    private SaveActionsMigration() {
    }

    /// The configuration the library carries once its Save Actions have moved into it, or nothing
    /// where it has none to move: such a library keeps the configuration it has, or the lack of one.
    ///
    /// @param fieldPreferences tells the whitespace rules which fields hold text that may be wrapped
    public static Optional<LintSettings> migrated(BibDatabaseContext databaseContext, FieldPreferences fieldPreferences) {
        return databaseContext.getMetaData()
                              .getSaveActions()
                              .filter(item -> !item.getConfiguredActions().isEmpty())
                              .map(item -> rewritten(applied(databaseContext, fieldPreferences), item));
    }

    /// Moves the Save Actions into the library's formatting configuration and drops the item.
    ///
    /// @param fieldPreferences tells the whitespace rules which fields hold text that may be wrapped
    public static void migrate(BibDatabaseContext databaseContext, FieldPreferences fieldPreferences) {
        MetaData metaData = databaseContext.getMetaData();
        migrated(databaseContext, fieldPreferences).ifPresent(metaData::setFormatting);
        metaData.clearSaveActions();
    }

    /// What a run of the library applies while it still has the item: what it says, or every rule
    /// it has where it says nothing.
    private static LintSettings applied(BibDatabaseContext databaseContext, FieldPreferences fieldPreferences) {
        return databaseContext.getMetaData()
                              .getFormatting()
                              .orElseGet(() -> LintSettings.repairing(RuleSet.forLibrary(databaseContext, fieldPreferences).ids()));
    }

    private static LintSettings rewritten(LintSettings before, FieldFormatterCleanupActions item) {
        SequencedMap<RuleSelector, RuleMode> moved = new LinkedHashMap<>();
        for (FieldFormatterCleanup action : item.getConfiguredActions()) {
            String ruleId = SaveActionIds.idOf(action.getFormatter());
            RuleSelector stated = RuleSelector.on(action.getField(), ruleId);
            moved.putIfAbsent(stated, item.isEnabled() ? before.modeOf(ruleId, fieldOf(stated)) : RuleMode.OFF);
            if (item.isEnabled()) {
                before.ruleModes().forEach((selector, mode) -> {
                    if (ruleId.equals(selector.ruleId()) && narrows(selector, action)) {
                        moved.putIfAbsent(selector, mode);
                    }
                });
            }
        }
        before.ruleModes().forEach((selector, mode) -> {
            if (SaveActionIds.formatterFor(selector.ruleId()).isEmpty()) {
                moved.putIfAbsent(selector, mode);
            }
        });
        return new LintSettings(moved, before.unreadRules(), before.unreadKeys());
    }

    /// The field a mode is looked up for: none for a Save Action on a whole class of fields, whose
    /// findings each name a field of their own.
    private static Optional<Field> fieldOf(RuleSelector selector) {
        return selector.coversEveryField() ? Optional.empty() : selector.field();
    }

    /// Whether `selector` is about one field that `action` visits besides its own.
    private static boolean narrows(RuleSelector selector, FieldFormatterCleanup action) {
        return !selector.coversEveryField()
                && selector.field()
                           .filter(field -> !field.equals(action.getField()))
                           .filter(action::visits)
                           .isPresent();
    }
}
