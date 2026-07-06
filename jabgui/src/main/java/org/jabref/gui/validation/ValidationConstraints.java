package org.jabref.gui.validation;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

import javafx.beans.value.ObservableValue;

import org.jfxcore.validation.Constraint;
import org.jfxcore.validation.Constraints;
import org.jfxcore.validation.ValidationResult;
import org.jspecify.annotations.NullMarked;

/// Adapts JabRef's existing validation-logic shapes ("does this value satisfy a predicate", "does this value
/// map to an error message") into jfxcore [Constraint]s carrying a [ValidationMessage] diagnostic,
/// so call sites don't need to hand-write `Constraints.validate` lambdas everywhere.
///
/// The `dependency`-taking overloads exist for genuine cross-field rules: unlike mvvmfx-validation's
/// `FunctionBasedValidator`, which only reactively re-runs when its own source property changes (any other
/// property read inside the predicate/function is read imperatively, not reactively), these re-evaluate
/// automatically whenever `dependency` changes too.
@NullMarked
public final class ValidationConstraints {

    private ValidationConstraints() {
    }

    public static <T> Constraint<T, ValidationMessage> predicate(Predicate<T> predicate, ValidationMessage message) {
        return Constraints.validate(value ->
                predicate.test(value) ? ValidationResult.valid() : ValidationResult.invalid(message));
    }

    /// The message is derived from the value itself; an empty `Optional` means the value is valid.
    public static <T> Constraint<T, ValidationMessage> function(Function<T, Optional<ValidationMessage>> function) {
        return Constraints.validate(value ->
                function.apply(value)
                        .map(ValidationResult::<ValidationMessage>invalid)
                        .orElseGet(ValidationResult::valid));
    }

    public static <T, P1> Constraint<T, ValidationMessage> predicate(
            BiPredicate<T, P1> predicate, ValidationMessage message, ObservableValue<P1> dependency) {
        return Constraints.validate(
                (T value, P1 dep) -> predicate.test(value, dep) ? ValidationResult.valid() : ValidationResult.invalid(message),
                dependency);
    }

    public static <T, P1> Constraint<T, ValidationMessage> function(
            BiFunction<T, P1, Optional<ValidationMessage>> function, ObservableValue<P1> dependency) {
        return Constraints.validate(
                (T value, P1 dep) -> function.apply(value, dep)
                        .map(ValidationResult::<ValidationMessage>invalid)
                        .orElseGet(ValidationResult::valid),
                dependency);
    }
}
