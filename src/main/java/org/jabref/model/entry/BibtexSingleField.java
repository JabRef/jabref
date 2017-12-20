package org.jabref.model.entry;

import java.util.EnumSet;
import java.util.Set;

/**
 * Class for keeping properties of a single BibTeX/biblatex field
 */
public class BibtexSingleField {

    public static final int DEFAULT_FIELD_LENGTH = 100;

    private enum Flag {
        STANDARD,
        PRIVATE,
        DISPLAYABLE,
        WRITEABLE
    }

    // the field name
    private String name;

    // contains the standard, private, displayable, writable infos
    // default is: not standard, public, displayable and writable
    private final Set<Flag> flags = EnumSet.of(Flag.DISPLAYABLE, Flag.WRITEABLE);

    private final int length;

    // properties contains a set of FieldProperty to e.g. tell the EntryEditor to add a specific
    // function to this field, to format names, or to control the integrity checks.
    private Set<FieldProperty> properties = EnumSet.noneOf(FieldProperty.class);

    // a comma separated list of alternative bibtex-fieldnames, e.g.
    // "LCCN" is the same like "lib-congress"
    // private String otherNames = null ;

    public BibtexSingleField(String fieldName, boolean pStandard) {
        this(fieldName, pStandard, DEFAULT_FIELD_LENGTH);
    }

    public BibtexSingleField(String fieldName, boolean pStandard, int pLength) {
        name = fieldName;
        setFlag(pStandard, Flag.STANDARD);
        length = pLength;
    }

    /**
     * Sets or onsets the given flag
     * @param setToOn if true, set the flag; if false, unset the flat
     * @param flagID, the id of the flag
     */
    private void setFlag(boolean setToOn, Flag flagID) {
        if (setToOn) {
            // set the flag
            flags.add(flagID);
        } else {
            // unset the flag
            flags.remove(flagID);
        }
    }

    public boolean isStandard() {
        return flags.contains(Flag.STANDARD);
    }

    public void setPrivate() {
        flags.add(Flag.PRIVATE);
    }

    public void setPublic() {
        flags.remove(Flag.PRIVATE);
    }

    public boolean isPrivate() {
        return flags.contains(Flag.PRIVATE);
    }

    public void setDisplayable(boolean value) {
        setFlag(value, Flag.DISPLAYABLE);
    }

    public boolean isDisplayable() {
        return flags.contains(Flag.DISPLAYABLE);
    }

    public void setWriteable(boolean value) {
        setFlag(value, Flag.WRITEABLE);
    }

    public boolean isWriteable() {
        return flags.contains(Flag.WRITEABLE);
    }

    public BibtexSingleField withProperties(FieldProperty first, FieldProperty... rest) {
        properties = EnumSet.of(first, rest);
        return this;
    }

    // fieldExtras contains mappings to tell the EntryEditor to add a specific
    // function to this field, for instance a "browse" button for the "pdf" field.
    public Set<FieldProperty> getProperties() {
        return properties;
    }

    /**
     * @return The maximum (expected) length of the field value; <em>not</em> the length of the field name
     */
    public int getLength() {
        return this.length;
    }

    public String getName() {
        return name;
    }

    /**
     * Set this field's numeric property
     *
     * @param numeric true to indicate that this is a numeric field.
     * @return this BibtexSingleField instance. Makes it easier to call this
     * method on the fly while initializing without using a local variable.
     */
    public BibtexSingleField setNumeric(boolean numeric) {
        if (numeric) {
            properties.add(FieldProperty.NUMERIC);
        } else {
            properties.remove(FieldProperty.NUMERIC);
        }
        return this;
    }

    public boolean isNumeric() {
        return properties.contains(FieldProperty.NUMERIC);
    }

    public void setName(String fieldName) {
        name = fieldName;
    }

}
