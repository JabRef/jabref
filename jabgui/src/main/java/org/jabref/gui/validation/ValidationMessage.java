package org.jabref.gui.validation;

import org.jspecify.annotations.NullMarked;

/// The diagnostic type attached to jfxcore `Constraint`/`ValidationResult` instances throughout
/// JabRef's GUI validation, replacing ControlsFX's/mvvmfx-validation's class of the same name.
@NullMarked
public record ValidationMessage(Severity severity, String message) {
    public static ValidationMessage error(String message) {
        return new ValidationMessage(Severity.ERROR, message);
    }

    public static ValidationMessage warning(String message) {
        return new ValidationMessage(Severity.WARNING, message);
    }
}
