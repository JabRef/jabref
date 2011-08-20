package net.sf.jabref.oo;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: 16-Dec-2007
 * Time: 10:37:23
 * To change this template use File | Settings | File Templates.
 */
public class BibtexEntryNotFoundException extends Exception {
    private String bibtexKey;

    public BibtexEntryNotFoundException(String bibtexKey, String message) {
        super(message);

        this.bibtexKey = bibtexKey;
    }

    public String getBibtexKey() {
        return bibtexKey;
    }
}
