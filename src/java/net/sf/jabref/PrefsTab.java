package net.sf.jabref;


interface PrefsTab {

    /**
     * This method is called when the user presses OK in the
     * Preferences dialog. Implementing classes must make sure all
     * settings presented get stored in JabRefPreferences.
     *
     */
    public void storeSettings();

}
