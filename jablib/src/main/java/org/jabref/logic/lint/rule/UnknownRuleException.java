package org.jabref.logic.lint.rule;

import java.util.List;

import org.jabref.logic.JabRefException;

import org.jspecify.annotations.NullMarked;

/// Signals that a rule was selected by an id no rule carries -- in practice, a typo in a
/// configuration or on the command line.
///
/// Quietly ignoring such an id would be the worse outcome: the user would be left believing a rule
/// had been switched off while it went on running, and would blame the rule for the result.
///
/// The message is deliberately plain English. Callers that show it to a user format their own
/// localized text from [#getUnknownIds] and [#getKnownIds].
@NullMarked
public class UnknownRuleException extends JabRefException {

    private final List<String> unknownIds;
    private final List<String> knownIds;

    public UnknownRuleException(List<String> unknownIds, List<String> knownIds) {
        super("Unknown rule: %s. Known rules: %s".formatted(String.join(", ", unknownIds), String.join(", ", knownIds)));
        this.unknownIds = List.copyOf(unknownIds);
        this.knownIds = List.copyOf(knownIds);
    }

    /// @return the ids that were asked for but do not exist, in the order they were given
    public List<String> getUnknownIds() {
        return unknownIds;
    }

    /// @return every id that would have been accepted
    public List<String> getKnownIds() {
        return knownIds;
    }
}
