package org.jabref.gui.fieldeditors;

import javafx.scene.control.TextInputControl;

import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.preferences.PreferencesService;

import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;

public class EditorValidator {

    private final PreferencesService preferences;

    public EditorValidator(PreferencesService preferences) {
        this.preferences = preferences;
    }

    public void configureValidation(final ValidationStatus status, final TextInputControl textInput) {
        if (preferences.getEntryEditorPreferences().shouldEnableValidation()) {
            ControlsFxVisualizer validationVisualizer = new ControlsFxVisualizer();
            validationVisualizer.setDecoration(new IconValidationDecorator());
            validationVisualizer.initVisualization(status, textInput);
        }
    }
}
