package org.jabref.logic.layout;

/**
 * String and integer value.
 */
public class StringInt implements java.io.Serializable {

    /**
     * Description of the Field
     */
    public String s;

    /**
     * Description of the Field
     */
    public final int i;

    /**
     * Constructor for the StringInt object
     */
    public StringInt(final String s, final int i) {
        this.s = s;
        this.i = i;
    }
}
