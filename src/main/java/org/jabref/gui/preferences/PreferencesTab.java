package org.jabref.gui.preferences;

import java.util.List;

import javafx.scene.Node;

/**
 * A prefsTab is a component displayed in the PreferenceDialog.
 * <p>
 * It needs to extend from Component.
 */
public interface PreferencesTab {

    Node getBuilder();

    /**
     * Should return the localized identifier to use for the tab.
     *
     * @return Identifier for the tab (for instance "General", "Appearance" or "External Files").
     */
    String getTabName();

    /**
     * This method is called when the dialog is opened, or if it is made
     * visible after being hidden. This calls the appropriate method in the
     * ViewModel.
     */
    void setValues();

    /**
     * This method is called when the user presses OK in the Preferences
     * dialog. This calls the appropriate method in the ViewModel.
     */
    void storeSettings();

    /**
     * This method is called before the {@link #storeSettings()} method,
     * to check if there are illegal settings in the tab, or if is ready
     * to be closed. This calls the appropriate method in the ViewModel.
     */
    boolean validateSettings();

    /**
     * This method should be called after storing the preferences, This
     * calls the appropriate method in the ViewModel.
     *
     * @return The messages for the changed properties (e. g. "Changed language: English")
     */
    List<String> getRestartWarnings();
}
