package org.jabref.logic.lint.rule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.cleanup.FieldFormatterCleanup;
import org.jabref.logic.cleanup.FieldFormatterCleanupActions;
import org.jabref.logic.lint.rules.RepeatedWhitespaceRule;
import org.jabref.logic.lint.rules.SurroundingWhitespaceRule;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.metadata.LintSettings;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.metadata.RuleMode;
import org.jabref.model.metadata.RuleSelector;

import org.jspecify.annotations.NullMarked;

/// The rules a JabFix run applies, in the order it applies them.
///
/// This is the seam configuration hangs off: [#all] is the built-in default, [#without] switches
/// individual rules off by id, and [#with] adds a rule that does not ship with JabFix at all.
///
/// Order is part of the contract, not an implementation detail. Each rule sees what the rules
/// before it left behind, so rules that tidy a value up belong before rules that pattern-match it:
/// otherwise the latter are defeated by noise the former would have removed.
@NullMarked
public class RuleSet {

    private final List<Rule> rules;

    private RuleSet(List<Rule> rules) {
        this.rules = List.copyOf(rules);
    }

    /// The default: every rule JabFix ships with, in the order a save has always applied them.
    ///
    /// @param fieldPreferences tells the whitespace rules which fields hold text that may be wrapped
    public static RuleSet all(FieldPreferences fieldPreferences) {
        return new RuleSet(List.of(
                new SurroundingWhitespaceRule(),
                new RepeatedWhitespaceRule(fieldPreferences)));
    }

    /// The rules a save of `databaseContext` applies: the library's own Save Actions first -- a save
    /// has always applied them before anything else -- and then every rule JabFix ships with.
    ///
    /// Save Actions the library switched off contribute no rule, just as they change nothing on save.
    ///
    /// Where the library states them is what it still has: its `saveActions` item while it has one,
    /// and its formatting configuration once that item is gone. A library keeps the item until
    /// something migrates it, so nothing about an existing library changes by being opened.
    ///
    /// The two are not read at once, which is what keeps one entry from meaning two things. While
    /// the item is there, `title:lower-case` in the configuration says how far the Save Action goes
    /// on the title; once it is gone, the same entry is what says there is such a Save Action at
    /// all. That shift happens exactly at the migration, and only there.
    ///
    /// @param fieldPreferences tells the whitespace rules which fields hold text that may be wrapped
    public static RuleSet forLibrary(BibDatabaseContext databaseContext, FieldPreferences fieldPreferences) {
        MetaData metaData = databaseContext.getMetaData();
        List<FieldFormatterCleanup> saveActions =
                metaData.getSaveActions()
                        .map(actions -> configured(metaData, actions))
                        .orElseGet(() -> statedInFormatting(metaData));

        return new RuleSet(Stream.concat(
                saveActions.stream().map(SaveActionRule::new),
                all(fieldPreferences).rules().stream()).toList());
    }

    /// What the library's `saveActions` item says, which is nothing where the item says they are
    /// switched off.
    private static List<FieldFormatterCleanup> configured(MetaData metaData, FieldFormatterCleanupActions actions) {
        if (!actions.isEnabled()) {
            return List.of();
        }
        return withKeywordSeparatorOf(metaData, actions);
    }

    /// What the library states in its formatting configuration: one Save Action per entry naming a
    /// formatter, on the field the entry names, or on every field where it names none.
    ///
    /// An entry switched off states no Save Action rather than a switched-off one, so that a
    /// `saveActions` item that was switched off migrates to entries saying so and comes back as the
    /// same nothing.
    private static List<FieldFormatterCleanup> statedInFormatting(MetaData metaData) {
        List<FieldFormatterCleanup> stated =
                metaData.getFormatting()
                        .map(settings -> settings.ruleModes().entrySet().stream()
                                                 .filter(named -> named.getValue() != RuleMode.OFF)
                                                 .flatMap(named -> asSaveAction(named.getKey()).stream())
                                                 .toList())
                        .orElse(List.of());

        // Through JabRef's own configuration step, so that a formatter needing the library's
        // keyword separator is given it, exactly as one from the `saveActions` item would be.
        return withKeywordSeparatorOf(metaData, new FieldFormatterCleanupActions(true, stated));
    }

    private static Optional<FieldFormatterCleanup> asSaveAction(RuleSelector selector) {
        return SaveActionIds.formatterFor(selector.ruleId())
                            .map(formatter -> new FieldFormatterCleanup(
                                    selector.field().orElse(InternalField.INTERNAL_ALL_FIELD), formatter));
    }

    private static List<FieldFormatterCleanup> withKeywordSeparatorOf(MetaData metaData, FieldFormatterCleanupActions actions) {
        return metaData.getKeywordSeparator()
                       .map(actions::getConfiguredActions)
                       .orElseGet(actions::getConfiguredActions);
    }

    /// Exactly the given rules, in the given order. An empty set reformats without applying any
    /// rule at all.
    public static RuleSet of(Rule... rules) {
        return new RuleSet(List.of(rules));
    }

    /// The same rules, minus the ones named.
    ///
    /// @param ruleIds the [Rule#id]s to switch off
    /// @throws UnknownRuleException if any id names no rule in this set, rather than passing over
    ///                              it and leaving the user to wonder why nothing changed
    public RuleSet without(Collection<String> ruleIds) throws UnknownRuleException {
        rejectUnknown(ruleIds);
        return new RuleSet(rules.stream()
                                .filter(rule -> !ruleIds.contains(rule.id()))
                                .toList());
    }

    /// Holds the ids against the rules of this set, for ids a user has just typed: passing over one
    /// silently would leave them to wonder why nothing changed.
    ///
    /// What a library stores about itself is treated more leniently -- see [#asConfiguredBy].
    ///
    /// @throws UnknownRuleException if any id names no rule in this set
    public void rejectUnknown(Collection<String> ruleIds) throws UnknownRuleException {
        List<String> unknown = ruleIds.stream()
                                      .distinct()
                                      .filter(ruleId -> !ids().contains(ruleId))
                                      .toList();
        if (!unknown.isEmpty()) {
            throw new UnknownRuleException(unknown, ids());
        }
    }

    /// The rules of this set the library lets run, each as far as it lets it go on each field: a
    /// rule it switches off everywhere is dropped, and where it wants one checked only, the repair
    /// is taken off what that rule finds there.
    ///
    /// How far a rule goes cannot be settled once for the whole rule, since the library says it per
    /// field, so each kept rule is wrapped in one that settles it per finding.
    ///
    /// A selector naming no rule of this set is passed over. The settings may have been written by
    /// a JabFix that knows a rule this one does not, and such a library still has to save.
    public RuleSet asConfiguredBy(LintSettings settings) {
        return new RuleSet(rules.stream()
                                .filter(rule -> settings.runs(rule.id()))
                                .<Rule>map(rule -> new ConfiguredRule(rule, settings))
                                .toList());
    }

    /// Appends a rule, which is how a rule implemented outside this module joins a run.
    public RuleSet with(Rule rule) {
        List<Rule> extended = new ArrayList<>(rules);
        extended.add(rule);
        return new RuleSet(extended);
    }

    /// @return the rules, in application order
    public List<Rule> rules() {
        return rules;
    }

    /// @return the [Rule#id] of every rule in this set, in application order
    public List<String> ids() {
        return rules.stream().map(Rule::id).toList();
    }
}
