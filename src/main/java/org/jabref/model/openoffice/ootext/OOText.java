package org.jabref.model.openoffice.ootext;

import java.util.Objects;

/**
 * Text with HTML-like markup as understood by OOTextIntoOO.write
 *
 * Some of the tags can be added using OOFormat methods. Others come
 * from the layout engine, either by interpreting LaTeX markup or from
 * settings in the jstyle file.
 */
public class OOText {

    private final String data;

    private OOText(String data) {
        Objects.requireNonNull(data);
        this.data = data;
    }

    /* null input is passed through */
    public static OOText fromString(String s) {
        if (s == null) {
            return null;
        }
        return new OOText(s);
    }

    /* null input is passed through */
    public static String toString(OOText s) {
        if (s == null) {
            return null;
        }
        return s.data;
    }

    public String asString() {
        return data;
    }

    /* Object.equals */
    @Override
    public boolean equals(Object o) {

        if (o == this) {
            return true;
        }

        if (!(o instanceof OOText)) {
            return false;
        }

        OOText c = (OOText) o;

        return data.equals(c.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }
}
