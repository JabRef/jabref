package org.jabref.model.entry;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.model.entry.field.BibField;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldPriority;
import org.jabref.model.entry.field.OrFields;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;

import com.google.common.collect.Streams;

public class BibEntryTypeBuilder {
    private EntryType type = StandardEntryType.Misc;
    private Set<BibField> fields = new HashSet<>();
    private Set<OrFields> requiredFields = new HashSet<>();

    public BibEntryTypeBuilder withType(EntryType type) {
        this.type = type;
        return this;
    }

    public BibEntryTypeBuilder withImportantFields(Set<BibField> newFields) {
        return withImportantFields(newFields.stream().map(BibField::getField).collect(Collectors.toSet()));
    }

    public BibEntryTypeBuilder withImportantFields(Collection<Field> newFields) {
        this.fields = Streams.concat(fields.stream(), newFields.stream().map(field -> new BibField(field, FieldPriority.IMPORTANT)))
                            .collect(Collectors.toSet());
        return this;
    }

    public BibEntryTypeBuilder withImportantFields(Field... newFields) {
        return withImportantFields(Arrays.asList(newFields));
    }

    public BibEntryTypeBuilder withDetailFields(Collection<Field> newFields) {
        this.fields = Streams.concat(fields.stream(), newFields.stream().map(field -> new BibField(field, FieldPriority.DETAIL)))
                             .collect(Collectors.toSet());
        return this;
    }

    public BibEntryTypeBuilder withDetailFields(Field... fields) {
        return withDetailFields(Arrays.asList(fields));
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
        // Treat required fields as important ones
        Stream<BibField> requiredAsImportant = requiredFields.stream()
                                                             .flatMap(TreeSet::stream)
                                                             .map(field -> new BibField(field, FieldPriority.IMPORTANT));
        Set<BibField> allFields = Stream.concat(fields.stream(), requiredAsImportant).collect(Collectors.toSet());
        return new BibEntryType(type, allFields, requiredFields);
    }
}
