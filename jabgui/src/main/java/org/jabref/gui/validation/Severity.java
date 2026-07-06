package org.jabref.gui.validation;

/// The severity of a {@link ValidationMessage}.
///
/// This is purely cosmetic (which icon/style class is used to display a message) — a field with a
/// {@link #WARNING} message is just as invalid as one with an {@link #ERROR} message.
public enum Severity {
    WARNING,
    ERROR
}
