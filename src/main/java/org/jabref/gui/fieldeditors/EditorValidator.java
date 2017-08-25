package org.jabref.gui.fieldeditors;

import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;
import org.jabref.Globals;
import org.jabref.preferences.JabRefPreferences;

public class EditorValidator {

    public static void configureValidation(ValidationStatus status, EditorTextArea area) {
        if (Globals.prefs.getBoolean(JabRefPreferences.VALIDATE_IN_ENTRY_EDITOR)) {
            ControlsFxVisualizer validationVisualizer = new ControlsFxVisualizer();
            validationVisualizer.initVisualization(status, area);
        }
    }
}
