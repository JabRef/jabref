package org.jabref.gui.fieldeditors;

import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.preferences.JabRefPreferences;

import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;

public class EditorValidator {

    private final JabRefPreferences preferences;

    public EditorValidator(JabRefPreferences preferences) {
        this.preferences = preferences;
    }

    public void configureValidation(ValidationStatus status, EditorTextArea area) {
        if (preferences.getBoolean(JabRefPreferences.VALIDATE_IN_ENTRY_EDITOR)) {
            ControlsFxVisualizer validationVisualizer = new ControlsFxVisualizer();
            validationVisualizer.setDecoration(new IconValidationDecorator());
            validationVisualizer.initVisualization(status, area);
        }
    }
}
