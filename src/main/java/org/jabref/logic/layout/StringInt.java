package org.jabref.logic.layout;

import java.io.Serializable;

/**
 * String and integer value.
 */
public class StringInt implements Serializable {

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
