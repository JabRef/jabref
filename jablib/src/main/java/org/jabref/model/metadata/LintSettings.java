package org.jabref.model.metadata;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedMap;

import org.jabref.model.entry.field.Field;

import org.jspecify.annotations.NullMarked;

/// What a library, or one run, asks JabFix to do.
///
/// This is the configuration, not the rules themselves: which rules a run ends up applying is
/// derived from it, together with the library's Save Actions -- see
/// `org.jabref.logic.lint.JabFixConfiguration`.
///
/// It may name a mode, a set of rules with their modes that JabFix ships, and it states the mode of
/// single rules, which outranks what the mode says about them. A rule neither names does not run --
/// naming one [RuleMode#OFF] says the same thing out loud, which is also how a library switches off
/// a rule its mode runs.
///
/// One entry per selector is also where the values a rule takes will go, once rules take any: they
/// belong to the rule they configure, not to a list beside it.
///
/// @param mode        the key of the mode the rules are laid over, if any. A key naming no mode of
///                    this JabFix is passed over rather than rejected, like a rule it does not know.
/// @param ruleModes   how far each named selector goes. The order is the one the library wrote them
///                    in, so that saving does not reshuffle a configuration somebody edited by hand.
///                    A [RuleSelector] naming no rule of this JabFix is passed over rather than
///                    rejected, so that a library configured by a newer JabFix still saves here.
/// @param unreadRules the entries of the configuration this JabFix could not read -- a selector
///                    spelled in a way it does not accept, or a mode it does not know -- by key,
///                    each as the JSON text of its value. They are written back as they came.
/// @param unreadKeys  what stood beside the mode and the rules in the block, kept the same way
@NullMarked
public record LintSettings(Optional<String> mode,
                           SequencedMap<RuleSelector, RuleMode> ruleModes,
                           SequencedMap<String, String> unreadRules,
                           SequencedMap<String, String> unreadKeys) {
    /// Settings that ask for nothing.
    public static final LintSettings NONE = new LintSettings(new LinkedHashMap<>());

    public LintSettings {
        ruleModes = Collections.unmodifiableSequencedMap(new LinkedHashMap<>(ruleModes));
        unreadRules = Collections.unmodifiableSequencedMap(new LinkedHashMap<>(unreadRules));
        unreadKeys = Collections.unmodifiableSequencedMap(new LinkedHashMap<>(unreadKeys));
    }

    /// Settings of rule modes alone.
    public LintSettings(SequencedMap<RuleSelector, RuleMode> ruleModes) {
        this(Optional.empty(), ruleModes, new LinkedHashMap<>(), new LinkedHashMap<>());
    }

    /// Settings naming one selector, to be extended with [#and].
    public static LintSettings of(RuleSelector selector, RuleMode mode) {
        return NONE.and(selector, mode);
    }

    /// The same settings, laid over the mode with that key.
    public LintSettings withMode(String modeKey) {
        return new LintSettings(Optional.of(modeKey), ruleModes, unreadRules, unreadKeys);
    }

    /// The same settings, plus that selector. A selector already named keeps its place and takes
    /// the new mode, the way a repeated key in the stored configuration does.
    public LintSettings and(RuleSelector selector, RuleMode mode) {
        SequencedMap<RuleSelector, RuleMode> extended = new LinkedHashMap<>(ruleModes);
        extended.put(selector, mode);
        return new LintSettings(this.mode, extended, unreadRules, unreadKeys);
    }

    /// These settings with `other` laid over them, which is how the more specific layer of a
    /// configuration outranks the broader one.
    ///
    /// The mode of `other` replaces this one where it names one. A selector of `other` replaces what
    /// this says about the same rule: all of it, for a selector about every field the rule looks at,
    /// and otherwise about that one field. Leaving a field-scoped entry standing beside a broader one
    /// of `other` would let it win, since it is the more specific.
    public LintSettings overriddenBy(LintSettings other) {
        SequencedMap<RuleSelector, RuleMode> overridden = new LinkedHashMap<>();
        ruleModes.forEach((selector, ruleMode) -> {
            if (!other.speaksFor(selector)) {
                overridden.put(selector, ruleMode);
            }
        });
        overridden.putAll(other.ruleModes);
        return new LintSettings(other.mode.or(() -> mode), overridden, merged(unreadRules, other.unreadRules), merged(unreadKeys, other.unreadKeys));
    }

    private boolean speaksFor(RuleSelector selector) {
        return ruleModes.containsKey(selector)
                || ruleModes.keySet().stream()
                            .anyMatch(named -> named.ruleId().equals(selector.ruleId()) && named.coversEveryField());
    }

    private static SequencedMap<String, String> merged(SequencedMap<String, String> under, SequencedMap<String, String> over) {
        SequencedMap<String, String> merged = new LinkedHashMap<>(under);
        merged.putAll(over);
        return merged;
    }

    /// Whether there is nothing here at all -- no mode, no rule, and nothing kept for a newer JabFix.
    public boolean isEmpty() {
        return mode.isEmpty() && ruleModes.isEmpty() && unreadRules.isEmpty() && unreadKeys.isEmpty();
    }

    /// How far the rule goes on that field. The most specific selector wins: one naming the field
    /// beats one about every field, so a library can have a rule repair everywhere and only report
    /// on a single field. Among equally specific selectors the last one named wins, the way a
    /// repeated key in a configuration file does. A rule no selector names does not run.
    ///
    /// The mode is not looked at here: it is expanded into rule modes before a run asks this.
    ///
    /// @param field the field a finding is about, or empty for one that is about the whole entry
    public RuleMode modeOf(String ruleId, Optional<Field> field) {
        return field.flatMap(named -> Optional.ofNullable(ruleModes.get(RuleSelector.on(named, ruleId))))
                    .or(() -> modeEverywhere(ruleId))
                    .orElse(RuleMode.OFF);
    }

    private Optional<RuleMode> modeEverywhere(String ruleId) {
        return ruleModes.entrySet().stream()
                        .filter(named -> ruleId.equals(named.getKey().ruleId())
                                && named.getKey().coversEveryField())
                        .map(Map.Entry::getValue)
                        .reduce((earlier, later) -> later);
    }

    /// Whether any selector lets this rule do anything, on any field.
    ///
    /// A rule switched off everywhere is left out of the run entirely rather than run and reported
    /// on no field.
    public boolean runs(String ruleId) {
        return ruleModes.entrySet().stream()
                        .anyMatch(named -> ruleId.equals(named.getKey().ruleId())
                                && (named.getValue() != RuleMode.OFF));
    }
}
