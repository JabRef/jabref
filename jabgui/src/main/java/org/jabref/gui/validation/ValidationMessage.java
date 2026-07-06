package org.jabref.gui.validation;

/// The diagnostic type attached to jfxcore {@code Constraint}/{@code ValidationResult} instances throughout
/// JabRef's GUI validation, replacing ControlsFX's/mvvmfx-validation's class of the same name.
public record ValidationMessage(Severity severity, String message) {

    public static ValidationMessage error(String message) {
        return new ValidationMessage(Severity.ERROR, message);
    }

    public static ValidationMessage warning(String message) {
        return new ValidationMessage(Severity.WARNING, message);
    }
}
