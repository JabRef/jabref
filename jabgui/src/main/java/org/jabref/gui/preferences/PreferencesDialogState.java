package org.jabref.gui.preferences;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

/// Live, unsaved state shared between tabs while the preferences dialog is open.
///
/// Tabs must not read each other's view models directly: that couples them and forces a
/// construction order. Instead, a tab publishes a value here (e.g. the AI tab binds
/// {@link #aiEnabledProperty()} to its master switch) and any other tab observes it.
public class PreferencesDialogState {

    private final BooleanProperty aiEnabled = new SimpleBooleanProperty();

    /// The AI master switch as currently shown in the AI tab (not yet stored).
    public BooleanProperty aiEnabledProperty() {
        return aiEnabled;
    }

    public ReadOnlyBooleanProperty aiEnabledReadOnlyProperty() {
        return aiEnabled;
    }
}
