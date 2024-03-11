package org.jabref.model.entry;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.SequencedCollection;
import java.util.SequencedSet;
import java.util.Set;
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
    private SequencedSet<BibField> fields = new LinkedHashSet<>();
    private SequencedSet<OrFields> requiredFields = new LinkedHashSet<>();

    public BibEntryTypeBuilder withType(EntryType type) {
        this.type = type;
        return this;
    }

    public BibEntryTypeBuilder withImportantFields(SequencedSet<Field> newFields) {
        this.fields = Streams.concat(fields.stream(), newFields.stream().map(field -> new BibField(field, FieldPriority.IMPORTANT)))
                             .collect(Collectors.toCollection(LinkedHashSet::new));
        return this;
    }

    public BibEntryTypeBuilder withImportantFields(Field... newFields) {
        return withImportantFields(Arrays.stream(newFields).collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    public BibEntryTypeBuilder withDetailFields(SequencedCollection<Field> newFields) {
        this.fields = Streams.concat(fields.stream(), newFields.stream().map(field -> new BibField(field, FieldPriority.DETAIL)))
                             .collect(Collectors.toCollection(LinkedHashSet::new));
        return this;
    }

    public BibEntryTypeBuilder withDetailFields(Field... fields) {
        return withDetailFields(Arrays.asList(fields));
    }

    public BibEntryTypeBuilder withRequiredFields(SequencedSet<OrFields> requiredFields) {
        this.requiredFields = requiredFields;
        return this;
    }

    public BibEntryTypeBuilder addRequiredFields(OrFields... requiredFields) {
        this.requiredFields.addAll(Arrays.asList(requiredFields));
        return this;
    }

    public BibEntryTypeBuilder addRequiredFields(Field... requiredFields) {
        this.requiredFields.addAll(Arrays.stream(requiredFields).map(OrFields::new).toList());
        return this;
    }

    public BibEntryTypeBuilder withRequiredFields(Field... requiredFields) {
        this.requiredFields = Arrays.stream(requiredFields).map(OrFields::new).collect(Collectors.toCollection(LinkedHashSet::new));
        return this;
    }

    public BibEntryTypeBuilder withRequiredFields(OrFields first, Field... requiredFields) {
        this.requiredFields = Stream.concat(Stream.of(first), Arrays.stream(requiredFields).map(OrFields::new)).collect(Collectors.toCollection(LinkedHashSet::new));
        return this;
    }

    public BibEntryTypeBuilder withRequiredFields(SequencedSet<OrFields> first, Field... requiredFields) {
        this.requiredFields = Stream.concat(first.stream(), Arrays.stream(requiredFields).map(OrFields::new)).collect(Collectors.toCollection(LinkedHashSet::new));
        return this;
    }

    public BibEntryType build() {
        // Treat required fields as important ones
        Stream<BibField> requiredAsImportant = requiredFields.stream()
                .map(OrFields::getFields)
                .flatMap(Set::stream)
                .map(field -> new BibField(field, FieldPriority.IMPORTANT));
        SequencedSet<BibField> allFields = Stream.concat(fields.stream(), requiredAsImportant).collect(Collectors.toCollection(LinkedHashSet::new));
        return new BibEntryType(type, allFields, requiredFields);
    }
}
