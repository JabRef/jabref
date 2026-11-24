package org.jabref.model.metadata;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.SequencedMap;

import org.jspecify.annotations.NullMarked;

/// What one entry of a library's formatting configuration says about the rule it selects.
///
/// @param mode   how far the rule goes
/// @param values what the rule takes beyond that, by name, in the order the library wrote them. What
///               they mean is up to the rule: see `org.jabref.logic.lint.rule.Rule#configuredWith`.
@NullMarked
public record RuleSettings(RuleMode mode, SequencedMap<String, String> values) {
    public RuleSettings {
        values = Collections.unmodifiableSequencedMap(new LinkedHashMap<>(values));
    }

    /// A mode alone, which is all most rules take.
    public RuleSettings(RuleMode mode) {
        this(mode, new LinkedHashMap<>());
    }
}
