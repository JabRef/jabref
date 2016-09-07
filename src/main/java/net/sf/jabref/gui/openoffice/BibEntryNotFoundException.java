package net.sf.jabref.gui.openoffice;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: 16-Dec-2007
 * Time: 10:37:23
 * To change this template use File | Settings | File Templates.
 */
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
