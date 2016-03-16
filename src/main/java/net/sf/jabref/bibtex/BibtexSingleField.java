package net.sf.jabref.bibtex;

import java.util.EnumSet;
import java.util.Set;

import net.sf.jabref.gui.GUIGlobals;

// --------------------------------------------------------------------------
// a container class for all properties of a bibtex-field
// --------------------------------------------------------------------------
public class BibtexSingleField {

    // some fieldname constants
    public static final double DEFAULT_FIELD_WEIGHT = 1;
    public static final double MAX_FIELD_WEIGHT = 2;

    public static final double SMALL_W = 0.30;
    public static final double MEDIUM_W = 0.5;
    public static final double LARGE_W = 1.5;


    private enum Flag {
        STANDARD,
        PRIVATE,
        DISPLAYABLE,
        WRITEABLE;
    }


    // the field name
    private final String name;

    // contains the standard, private, displayable, writable infos
    // default is: not standard, public, displayable and writable
    private final Set<Flag> flag = EnumSet.of(Flag.DISPLAYABLE, Flag.WRITEABLE);

    private int length = GUIGlobals.DEFAULT_FIELD_LENGTH;
    private double weight = DEFAULT_FIELD_WEIGHT;

    // the extras data
    // fieldExtras contains mappings to tell the EntryEditor to add a specific
    // function to this field, for instance a "browse" button for the "pdf" field.
    private Set<BibtexSingleFieldProperties> extras = EnumSet.noneOf(BibtexSingleFieldProperties.class);

    // a comma separated list of alternative bibtex-fieldnames, e.g.
    // "LCCN" is the same like "lib-congress"
    // private String otherNames = null ;


    // a Hashmap for a lot of additional "not standard" properties
    // todo: add the handling in a key=value manner
    // private HashMap props = new HashMap() ;

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

    // -----------------------------------------------------------------------
    // -----------------------------------------------------------------------

    /**
     * Sets or onsets the given flag
     * @param setToOn if true, set the flag; if false, unset the flat
     * @param flagID, the id of the flag
     */
    private void setFlag(boolean setToOn, Flag flagID) {
        if (setToOn) {
            // set the flag
            flag.add(flagID);
        } else {
            // unset the flag
            flag.remove(flagID);
        }
    }

    // -----------------------------------------------------------------------
    public boolean isStandard() {
        return flag.contains(Flag.STANDARD);
    }

    public void setPrivate() {
        flag.add(Flag.PRIVATE);
    }

    public boolean isPrivate() {
        return flag.contains(Flag.PRIVATE);
    }

    public void setDisplayable(boolean value) {
        setFlag(value, Flag.DISPLAYABLE);
    }

    public boolean isDisplayable() {
        return flag.contains(Flag.DISPLAYABLE);
    }

    public void setWriteable(boolean value) {
        setFlag(value, Flag.WRITEABLE);
    }

    public boolean isWriteable() {
        return flag.contains(Flag.WRITEABLE);
    }

    // -----------------------------------------------------------------------

    public void setExtras(Set<BibtexSingleFieldProperties> pExtras) {
        extras = pExtras;
    }

    // fieldExtras contains mappings to tell the EntryEditor to add a specific
    // function to this field, for instance a "browse" button for the "pdf" field.
    public Set<BibtexSingleFieldProperties> getExtras() {
        return extras;
    }

    // -----------------------------------------------------------------------

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

    // -----------------------------------------------------------------------

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
            extras.add(BibtexSingleFieldProperties.NUMERIC);
        } else {
            extras.remove(BibtexSingleFieldProperties.NUMERIC);
        }
        return this;
    }

    public boolean isNumeric() {
        return extras.contains(BibtexSingleFieldProperties.NUMERIC);
    }

}
