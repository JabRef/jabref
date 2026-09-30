package org.jabref.model.metadata;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedMap;

import org.jabref.model.entry.field.Field;

import org.jspecify.annotations.NullMarked;

/// What a library asks JabFix to do when it is saved.
///
/// This is the configuration, not the rules themselves: which rules a run ends up applying is
/// derived from it, together with the library's Save Actions and the rules JabFix ships with.
///
/// A rule runs only where the library asks for it. Carrying these settings at all is what asks for
/// anything to be done, and within them a rule that is not named does not run -- naming one
/// [RuleMode#OFF] says the same thing out loud, where a library wants to record that it is
/// deliberately not run. A library that carries no settings is saved the way it always was.
///
/// One entry per selector is also where the values a rule takes will go, once rules take any: they
/// belong to the rule they configure, not to a list beside it.
///
/// @param ruleModes how far each named selector goes. The order is the one the library wrote them
///                  in, so that saving does not reshuffle a configuration somebody edited by hand.
///                  A [RuleSelector] naming no rule of this JabFix is passed over rather than rejected,
///                  so that a library configured by a newer JabFix still saves with an older one.
@NullMarked
public record LintSettings(SequencedMap<RuleSelector, RuleMode> ruleModes) {
    public LintSettings {
        ruleModes = Collections.unmodifiableSequencedMap(new LinkedHashMap<>(ruleModes));
    }

    /// Settings naming one selector, to be extended with [#and].
    public static LintSettings of(RuleSelector selector, RuleMode mode) {
        return new LintSettings(new LinkedHashMap<>()).and(selector, mode);
    }

    /// The same settings, plus that selector. A selector already named keeps its place and takes
    /// the new mode, the way a repeated key in the stored configuration does.
    public LintSettings and(RuleSelector selector, RuleMode mode) {
        SequencedMap<RuleSelector, RuleMode> extended = new LinkedHashMap<>(ruleModes);
        extended.put(selector, mode);
        return new LintSettings(extended);
    }

    /// How far the rule goes on that field. The most specific selector wins: one naming the field
    /// beats one about every field, so a library can have a rule repair everywhere and only report
    /// on a single field. Among equally specific selectors the last one named wins, the way a
    /// repeated key in a configuration file does. A rule no selector names does not run.
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
    /// on no field, which is also what makes it unknown to the magic comments of an entry: naming
    /// it there is then worth saying out loud.
    public boolean runs(String ruleId) {
        return ruleModes.entrySet().stream()
                        .anyMatch(named -> ruleId.equals(named.getKey().ruleId())
                                && (named.getValue() != RuleMode.OFF));
    }
}
