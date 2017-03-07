package org.jabref.gui.preftabs;

/**
 * A prefsTab is a component displayed in the PreferenceDialog.
 *
 * It needs to extend from Component.
 */
interface PrefsTab {

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
     * settings presented get stored in JabRefPreferences.
     */
    void storeSettings();

    /**
     * This method is called before the {@link #storeSettings()} method,
     * to check if there are illegal settings in the tab, or if is ready
     * to be closed.
     * If the tab is *not* ready, it should display a message to the user
     * informing about the illegal setting.
     */
    boolean validateSettings();

    /**
     * Should return the localized identifier to use for the tab.
     *
     * @return Identifier for the tab (for instance "General", "Appearance" or "External Files").
     */
    String getTabName();
}
