
package net.sf.jabref;


interface PrefsTab {

    /**
     * This method is called when the user presses OK in the
     * Preferences dialog. Implementing classes must make sure all
     * settings presented get stored in JabRefPreferences.
     *
     */
    public void storeSettings();

    /**
     * This method is called before the {@ling storeSettings()} method, 
     * to check if there are illegal settings in the tab, or if is ready
     * to be closed.
     * If the tab is *not* ready, it should display a message to the user 
     * informing about the illegal setting.
     */
    public boolean readyToClose();

}
