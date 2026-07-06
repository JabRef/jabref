package org.jabref.gui.validation;

import org.jspecify.annotations.NullMarked;

/// The severity of a [ValidationMessage].
///
/// This is purely cosmetic (which icon/style class is used to display a message) — a field with a
/// `WARNING` message is just as invalid as one with an `ERROR` message.
@NullMarked
public enum Severity {
    WARNING,
    ERROR
}
