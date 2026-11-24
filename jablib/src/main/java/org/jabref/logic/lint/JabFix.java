package org.jabref.logic.lint;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.jabref.logic.lint.rule.Finding;
import org.jabref.logic.lint.rule.MagicCommentRule;
import org.jabref.logic.lint.rule.Rule;
import org.jabref.logic.lint.rule.RuleSet;
import org.jabref.logic.lint.rule.Suppressions;
import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NullMarked;

/// Engine behind the `jabkit fix` command: applies a [RuleSet] to the entries of a library.
///
/// The rules decide the questions the `.bib` format leaves open but that survive parsing -- such as
/// whether a value may be padded with whitespace. Serialization is not this class's business: it is
/// left to JabRef's own [org.jabref.logic.exporter.BibDatabaseWriter], which settles everything that
/// does *not* survive parsing -- entry type capitalization, whether values are braced or quoted,
/// field order, indentation -- and which runs this engine when it is given one.
@NullMarked
public class JabFix {

    private final RuleSet ruleSet;

    public JabFix(RuleSet ruleSet) {
        this.ruleSet = ruleSet;
    }

    /// Runs every rule over `entries` and applies the repairs they offer.
    public JabFixResult apply(List<BibEntry> entries) {
        return apply(entries, Runnable::run);
    }

    /// Runs every rule over `entries`, in the order of the [RuleSet], and applies the repairs they
    /// offer.
    ///
    /// The entries are modified in place. Rules are applied as a pipeline: each rule sees what the
    /// rules before it left behind. A rule is therefore only ever run once over an entry -- see
    /// [Rule] for the contract that makes a second pass unnecessary.
    ///
    /// What the magic comments above an entry switch off ([Suppressions]) is dropped before the
    /// repairs are applied, so a suppressed finding is neither reported nor repaired. A rule that
    /// is switched off for one field only still runs on the rest of the entry.
    ///
    /// A [MagicCommentRule] runs ahead of the rule set, since a comment that names no rule of this
    /// run switches nothing off and would otherwise go unnoticed.
    ///
    /// @param mutationScheduler routes the [BibEntry] mutations the repairs make to the correct
    ///                          thread; it has to run each of them synchronously
    public JabFixResult apply(List<BibEntry> entries, Consumer<Runnable> mutationScheduler) {
        List<Rule> rules = Stream.concat(
                Stream.of(new MagicCommentRule(Set.copyOf(ruleSet.ids()))),
                ruleSet.rules().stream()).toList();

        List<Finding> findings = new ArrayList<>();
        List<FieldChange> changes = new ArrayList<>();
        for (BibEntry entry : entries) {
            Suppressions suppressions = Suppressions.in(entry);
            for (Rule rule : rules) {
                List<Finding> reported = rule.scan(entry).stream()
                                             .filter(finding -> !suppressions.suppresses(rule.id(), finding.field()))
                                             .toList();
                for (Finding finding : reported) {
                    // Only the mutation is scheduled; scanning stays on the calling thread, as the
                    // cleanup jobs of JabRef do it.
                    finding.fix().ifPresent(fix -> mutationScheduler.accept(() -> fix.applyTo(entry).ifPresent(changes::add)));
                }
                findings.addAll(reported);
            }
        }
        return new JabFixResult(findings, changes);
    }
}
