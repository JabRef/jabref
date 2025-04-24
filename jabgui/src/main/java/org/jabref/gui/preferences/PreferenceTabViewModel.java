package org.jabref.gui.preferences;

import java.util.Collections;
import java.util.List;

public interface PreferenceTabViewModel {

    /**
     * This method is called when the dialog is opened, or if it is made
     * visible after being hidden. The tab should update all its values.
     *
     * This is the ONLY PLACE to set values for the fields in the tab. It
     * is ILLEGAL to set values only at construction time, because the dialog
     * will be reused and updated.
     */
    void setValues();

    /**
     * This method is called when the user presses OK in the
     * Preferences dialog. Implementing classes must make sure all
     * settings presented get stored in PreferencesService.
     */
    void storeSettings();

    /**
     * This method is called before the {@link #storeSettings()} method,
     * to check if there are illegal settings in the tab, or if is ready
     * to be closed.
     * If the tab is *not* ready, it should display a message to the user
     * informing about the illegal setting.
     */
    default boolean validateSettings() {
        return true;
    }

    /**
     * This method should be called after storing the preferences, to
     * collect the properties, which require a restart of JabRef to load
     *
     * @return The messages for the changed properties (e. g. "Changed language: English")
     */
    default List<String> getRestartWarnings() {
        return Collections.emptyList();
    }
}
