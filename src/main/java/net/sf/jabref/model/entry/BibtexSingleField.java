package net.sf.jabref.model.entry;

import java.util.EnumSet;
import java.util.Set;

/**
 *
 * Class for keeping properties of a single BibTeX/BibLatex field
 *
 */
public class BibtexSingleField {

    // some field constants
    public static final double DEFAULT_FIELD_WEIGHT = 1;
    public static final double MAX_FIELD_WEIGHT = 2;

    public static final double SMALL_W = 0.30;
    public static final double MEDIUM_W = 0.5;
    public static final double LARGE_W = 1.5;

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

    private int length = DEFAULT_FIELD_LENGTH;
    private double weight = DEFAULT_FIELD_WEIGHT;

    // properties contains a set of FieldProperty to e.g. tell the EntryEditor to add a specific
    // function to this field, to format names, or to control the integrity checks.
    private Set<FieldProperty> properties = EnumSet.noneOf(FieldProperty.class);

    // a comma separated list of alternative bibtex-fieldnames, e.g.
    // "LCCN" is the same like "lib-congress"
    // private String otherNames = null ;

    public BibtexSingleField(String fieldName, boolean pStandard) {
        name = fieldName;
        setFlag(pStandard, Flag.STANDARD);
    }

    public BibtexSingleField(String fieldName, boolean pStandard, double pWeight) {
        name = fieldName;
        setFlag(pStandard, Flag.STANDARD);
        weight = pWeight;
    }

    public BibtexSingleField(String fieldName, boolean pStandard, int pLength) {
        name = fieldName;
        setFlag(pStandard, Flag.STANDARD);
        length = pLength;
    }

    public BibtexSingleField(String fieldName, boolean pStandard, double pWeight, int pLength) {
        name = fieldName;
        setFlag(pStandard, Flag.STANDARD);
        weight = pWeight;
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

    public void setExtras(Set<FieldProperty> pExtras) {
        properties = pExtras;
    }

    // fieldExtras contains mappings to tell the EntryEditor to add a specific
    // function to this field, for instance a "browse" button for the "pdf" field.
    public Set<FieldProperty> getFieldProperties() {
        return properties;
    }

    public void setWeight(double value) {
        this.weight = value;
    }

    public double getWeight() {
        return this.weight;
    }

    /**
     * @return The maximum (expected) length of the field value; <em>not</em> the length of the field name
     */
    public int getLength() {
        return this.length;
    }

    public String getFieldName() {
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
