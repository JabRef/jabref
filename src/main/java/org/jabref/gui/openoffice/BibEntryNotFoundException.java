package org.jabref.gui.openoffice;

class BibEntryNotFoundException extends Exception {

    private final String bibtexKey;


    public BibEntryNotFoundException(String bibtexKey, String message) {
        super(message);

        this.bibtexKey = bibtexKey;
    }

    public String getBibtexKey() {
        return bibtexKey;
    }
}
