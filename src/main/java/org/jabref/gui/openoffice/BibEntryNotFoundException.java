package org.jabref.gui.openoffice;

class BibEntryNotFoundException extends Exception {

    private final String citationKey;

    public BibEntryNotFoundException(String citationKey, String message) {
        super(message);

        this.citationKey = citationKey;
    }

    public String getCitationKey() {
        return citationKey;
    }
}
