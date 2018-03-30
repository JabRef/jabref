package org.jabref.model.entry;

import java.util.EnumSet;
import java.util.Set;

/**
 * Class for keeping properties of a single BibTeX/biblatex field
 */
public class BibtexSingleField {
    public static final int DEFAULT_FIELD_LENGTH = 100;

    private enum FieldType {
        STANDARD,
        PRIVATE,
        DISPLAYABLE,
        WRITEABLE
    }

    // the field name
    private String name;

    /**
     * contains the standard, private, displayable, writable infos
     * default is: not standard, public, displayable and writable
     */
    private final Set<FieldType> fieldTypes = EnumSet.of(FieldType.DISPLAYABLE, FieldType.WRITEABLE);

    private final int length;

    /**
     * properties contains a set of FieldProperty to e.g. tell the EntryEditor to add a specific
     * function to this field, to format names, or to control the integrity checks.
     */
    private Set<FieldProperty> properties = EnumSet.noneOf(FieldProperty.class);

    public BibtexSingleField(String fieldName) {
        this(fieldName, true, DEFAULT_FIELD_LENGTH);
    }

    public BibtexSingleField(String fieldName, boolean standardField) {
        this(fieldName, standardField, DEFAULT_FIELD_LENGTH);
    }

    public BibtexSingleField(String fieldName, boolean standardField, int length) {
        this.name = fieldName;
        this.length = length;
        if (standardField) {
            fieldTypes.add(FieldType.STANDARD);
        }
    }

    public boolean isStandard() {
        return fieldTypes.contains(FieldType.STANDARD);
    }

    public void setPrivate() {
        fieldTypes.add(FieldType.PRIVATE);
    }

    public void setPublic() {
        fieldTypes.remove(FieldType.PRIVATE);
    }

    public boolean isPrivate() {
        return fieldTypes.contains(FieldType.PRIVATE);
    }

    public void setDisplayable(boolean value) {
        if (value) {
            fieldTypes.add(FieldType.DISPLAYABLE);
        } else {
            fieldTypes.remove(FieldType.DISPLAYABLE);
        }
    }

    public boolean isDisplayable() {
        return fieldTypes.contains(FieldType.DISPLAYABLE);
    }

    public void setWriteable(boolean value) {
        if (value) {
            fieldTypes.add(FieldType.WRITEABLE);
        } else {
            fieldTypes.remove(FieldType.WRITEABLE);
        }
    }

    public boolean isWriteable() {
        return fieldTypes.contains(FieldType.WRITEABLE);
    }

    public BibtexSingleField withProperties(FieldProperty first, FieldProperty... rest) {
        properties = EnumSet.of(first, rest);
        return this;
    }

    /**
     * properties contains mappings to tell the EntryEditor to add a specific function to this field,
     * for instance a dropdown for selecting the month for the month field.
     */
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
     * @return this BibtexSingleField instance. Makes it easier to call this
     * method on the fly while initializing without using a local variable.
     */
    public BibtexSingleField setNumeric() {
        properties.add(FieldProperty.NUMERIC);
        return this;
    }

    public boolean isNumeric() {
        return properties.contains(FieldProperty.NUMERIC);
    }

    public void setName(String fieldName) {
        name = fieldName;
    }
}
