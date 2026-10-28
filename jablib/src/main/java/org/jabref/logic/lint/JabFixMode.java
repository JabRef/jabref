package org.jabref.logic.lint;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.SequencedMap;

import org.jabref.logic.cleanup.FieldFormatterCleanupActions;
import org.jabref.logic.lint.rule.MagicCommentRule;
import org.jabref.logic.lint.rule.SaveActionIds;
import org.jabref.logic.lint.rules.RepeatedWhitespaceRule;
import org.jabref.logic.lint.rules.SurroundingWhitespaceRule;
import org.jabref.model.metadata.LintSettings;
import org.jabref.model.metadata.RuleMode;
import org.jabref.model.metadata.RuleSelector;

import org.jspecify.annotations.NullMarked;

/// A set of rules JabFix ships, each with how far it goes, which a library or a run names by its key
/// instead of listing the rules one by one.
///
/// A mode is the broadest layer of a configuration: whatever a library or a run says about a single
/// rule outranks it -- see [JabFixConfiguration#applied].
///
/// These modes are placeholders that show the shape; which ones JabFix ships with is still open.
@NullMarked
public enum JabFixMode {
    WHITESPACE("whitespace", "Repairs whitespace around and inside field values.",
            LintSettings.of(RuleSelector.of(SurroundingWhitespaceRule.ID), RuleMode.FIX)
                        .and(RuleSelector.of(RepeatedWhitespaceRule.ID), RuleMode.FIX)
                        .and(RuleSelector.of(MagicCommentRule.ID), RuleMode.CHECK)),

    REPORT("report", "Reports whitespace around and inside field values without repairing it.",
            LintSettings.of(RuleSelector.of(SurroundingWhitespaceRule.ID), RuleMode.CHECK)
                        .and(RuleSelector.of(RepeatedWhitespaceRule.ID), RuleMode.CHECK)
                        .and(RuleSelector.of(MagicCommentRule.ID), RuleMode.CHECK)),

    DEFAULT_SAVE_ACTIONS("default-save-actions", "Repairs what JabRef's default Save Actions repair.",
            repairing(FieldFormatterCleanupActions.DEFAULT_SAVE_ACTIONS.stream().map(SaveActionIds::selectorOf).toList())
                    .and(RuleSelector.of(MagicCommentRule.ID), RuleMode.CHECK));

    private final String key;
    private final String description;
    private final LintSettings settings;

    JabFixMode(String key, String description, LintSettings settings) {
        this.key = key;
        this.description = description;
        this.settings = settings;
    }

    /// How the mode is named in a library's configuration and on the command line.
    public String getKey() {
        return key;
    }

    /// One sentence on what the mode does, for `--help`-style listings.
    public String getDescription() {
        return description;
    }

    /// The rules of the mode, each with how far it goes.
    public LintSettings getSettings() {
        return settings;
    }

    /// The mode named so, or nothing for a key this JabFix does not know -- which a newer one may.
    public static Optional<JabFixMode> fromKey(String key) {
        return Arrays.stream(values())
                     .filter(mode -> mode.key.equals(key))
                     .findFirst();
    }

    private static LintSettings repairing(Iterable<RuleSelector> selectors) {
        SequencedMap<RuleSelector, RuleMode> modes = new LinkedHashMap<>();
        selectors.forEach(selector -> modes.put(selector, RuleMode.FIX));
        return new LintSettings(modes);
    }
}
