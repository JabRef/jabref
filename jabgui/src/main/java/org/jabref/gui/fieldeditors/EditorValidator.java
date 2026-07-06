package org.jabref.gui.fieldeditors;

import javafx.scene.control.TextInputControl;

import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.validation.ValidationMessage;
import org.jabref.gui.validation.ValidationVisualizer;

import org.jfxcore.validation.property.ReadOnlyConstrainedProperty;

public class EditorValidator {

    private final GuiPreferences preferences;

    public EditorValidator(GuiPreferences preferences) {
        this.preferences = preferences;
    }

    public void configureValidation(final ReadOnlyConstrainedProperty<?, ValidationMessage> validation, final TextInputControl textInput) {
        if (preferences.getEntryEditorPreferences().shouldEnableValidation()) {
            new ValidationVisualizer().initVisualization(validation, textInput);
        }
    }
}
