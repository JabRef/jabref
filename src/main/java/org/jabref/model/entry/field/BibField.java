package org.jabref.model.entry.field;

import java.util.Optional;
import java.util.Set;

public class BibField implements Field<BibField> {

    private final FieldPriority priority;
    private final Field field;

    public BibField(Field field, FieldPriority priority) {
        this.priority = priority;
        this.field = field;
    }

    @Override
    public Set<FieldProperty> getProperties() {
        return field.getProperties();
    }

    @Override
    public String getDisplayName() {
        return field.getDisplayName();
    }

    @Override
    public String getName() {
        return field.getName();
    }

    @Override
    public boolean isStandardField() {
        return field.isStandardField();
    }

    @Override
    public boolean isDeprecated() {
        return field.isDeprecated();
    }

    @Override
    public Optional<Field> getAlias() {
        return field.getAlias();
    }

    @Override
    public int compareTo(BibField o) {
        return field.compareTo(o);
    }

    @Override
    public boolean isNumeric() {
        return field.isNumeric();
    }

    public FieldPriority getPriority() {
        return priority;
    }
}
