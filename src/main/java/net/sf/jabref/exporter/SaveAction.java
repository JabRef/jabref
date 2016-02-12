package net.sf.jabref.exporter;

import net.sf.jabref.logic.formatter.Formatter;

/**
 * Defines a mapping between a formatter and a field for which a save action can be applied
 */
public final class SaveAction {

    private final String fieldName;

    private final Formatter formatter;

    public SaveAction(String fieldName, Formatter formatter) {
        this.fieldName = fieldName;
        this.formatter = formatter;

    }

    public Formatter getFormatter() {
        return formatter;
    }

    public String getFieldName() {
        return fieldName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SaveAction that = (SaveAction) o;

        if (fieldName != null ? !fieldName.equals(that.fieldName) : that.fieldName != null) return false;
        return !(formatter != null ? !formatter.equals(that.formatter) : that.formatter != null);

    }

    @Override
    public int hashCode() {
        int result = fieldName != null ? fieldName.hashCode() : 0;
        result = 31 * result + (formatter != null ? formatter.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return fieldName + ": " + formatter.getKey();
    }
}
