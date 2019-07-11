package org.jabref.model.entry;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.model.entry.field.BibField;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldPriority;
import org.jabref.model.entry.field.OrFields;

public class BibEntryTypeBuilder {
    private EntryType type;
    private Set<BibField> fields;
    private Set<OrFields> requiredFields;

    public BibEntryTypeBuilder withType(EntryType type) {
        this.type = type;
        return this;
    }

    public BibEntryTypeBuilder withImportantFields(Set<BibField> fields) {
        this.fields = fields;
        return this;
    }

    public BibEntryTypeBuilder withImportantFields(Field... fields) {
        this.fields = Arrays.stream(fields)
                            .map(field -> new BibField(field, FieldPriority.IMPORTANT))
                            .collect(Collectors.toSet());
        return this;
    }

    public BibEntryTypeBuilder withDetailFields(Set<Field> fields) {
        this.fields = fields.stream()
                            .map(field -> new BibField(field, FieldPriority.DETAIL))
                            .collect(Collectors.toSet());
        return this;
    }

    public BibEntryTypeBuilder withDetailFields(Field... fields) {
        this.fields = Arrays.stream(fields)
                            .map(field -> new BibField(field, FieldPriority.DETAIL))
                            .collect(Collectors.toSet());
        return this;
    }

    public BibEntryTypeBuilder withRequiredFields(Set<OrFields> requiredFields) {
        this.requiredFields = requiredFields;
        return this;
    }

    public BibEntryTypeBuilder withRequiredFields(Field... requiredFields) {
        this.requiredFields = Arrays.stream(requiredFields).map(OrFields::new).collect(Collectors.toSet());
        return this;
    }

    public BibEntryTypeBuilder withRequiredFields(OrFields first, Field... requiredFields) {
        this.requiredFields = Stream.concat(Stream.of(first), Arrays.stream(requiredFields).map(OrFields::new)).collect(Collectors.toSet());
        return this;
    }

    public BibEntryTypeBuilder withRequiredFields(List<OrFields> first, Field... requiredFields) {
        this.requiredFields = Stream.concat(first.stream(), Arrays.stream(requiredFields).map(OrFields::new)).collect(Collectors.toSet());
        return this;
    }

    public BibEntryType build() {
        return new BibEntryType(type, fields, requiredFields);
    }
}
