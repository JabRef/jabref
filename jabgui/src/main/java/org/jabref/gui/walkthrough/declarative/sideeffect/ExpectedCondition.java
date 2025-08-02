package org.jabref.gui.walkthrough.declarative.sideeffect;

/// Predicate for whether a side effect can be executed.
@FunctionalInterface
public interface ExpectedCondition {
    /// Indicate a side effect can always be executed.
    ExpectedCondition ALWAYS_TRUE = () -> true;

    /// Evaluates whether the side effect can be executed.
    boolean evaluate();
}
