package org.jabref.model.entry;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.strings.StringUtil;

/**
 * This class is used to represent customized entry types.
 */
public class CustomEntryType implements EntryType {

    public static final String ENTRYTYPE_FLAG = "jabref-entrytype: ";
    private final String name;
    private final Set<Field> required;
    private final Set<Field> optional;
    private final Set<Field> primaryOptional;

    public CustomEntryType(String name, Collection<Field> required, Collection<Field> primaryOptional, Collection<Field> secondaryOptional) {
        this.name = StringUtil.capitalizeFirst(name);
        this.primaryOptional = new LinkedHashSet<>(primaryOptional);
        this.required = new LinkedHashSet<>(required);
        this.optional = Stream.concat(primaryOptional.stream(), secondaryOptional.stream()).collect(Collectors.toSet());
    }

    public CustomEntryType(String name, Collection<Field> required, Collection<Field> optional) {
        this.name = StringUtil.capitalizeFirst(name);
        this.required = new LinkedHashSet<>(required);
        this.optional = new LinkedHashSet<>(optional);
        this.primaryOptional = new LinkedHashSet<>(optional);
    }

    public CustomEntryType(String name, String required, String optional) {
        this(name, FieldFactory.parseFields(required), FieldFactory.parseOrFields(optional));
    }

    public static Optional<CustomEntryType> parse(String comment) {
        String rest = comment.substring(ENTRYTYPE_FLAG.length());
        int indexEndOfName = rest.indexOf(':');
        if (indexEndOfName < 0) {
            return Optional.empty();
        }
        String fieldsDescription = rest.substring(indexEndOfName + 2);

        int indexEndOfRequiredFields = fieldsDescription.indexOf(']');
        int indexEndOfOptionalFields = fieldsDescription.indexOf(']', indexEndOfRequiredFields + 1);
        if ((indexEndOfRequiredFields < 4) || (indexEndOfOptionalFields < (indexEndOfRequiredFields + 6))) {
            return Optional.empty();
        }
        String name = rest.substring(0, indexEndOfName);
        String reqFields = fieldsDescription.substring(4, indexEndOfRequiredFields);
        String optFields = fieldsDescription.substring(indexEndOfRequiredFields + 6, indexEndOfOptionalFields);
        return Optional.of(new CustomEntryType(name, reqFields, optFields));
    }

    @Override
    public int compareTo(EntryType o) {
        return getName().compareTo(o.getName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof CustomEntryType) {
            return Objects.equals(name, ((CustomEntryType) o).name);
        } else {
            return false;
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Set<Field> getOptionalFields() {
        return Collections.unmodifiableSet(optional);
    }

    @Override
    public Set<Field> getRequiredFields() {
        return Collections.unmodifiableSet(required);
    }

    @Override
    public Set<Field> getPrimaryOptionalFields() {
        return Collections.unmodifiableSet(primaryOptional);
    }

    @Override
    public Set<Field> getSecondaryOptionalFields() {
        Set<Field> result = new LinkedHashSet<>(optional);
        result.removeAll(primaryOptional);
        return Collections.unmodifiableSet(result);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    public String getAsString() {
        StringBuilder builder = new StringBuilder();
        builder.append(ENTRYTYPE_FLAG);
        builder.append(getName());
        builder.append(": req[");
        builder.append(FieldFactory.orFields(required));
        builder.append("] opt[");
        builder.append(String.join(";", FieldFactory.orFields(optional)));
        builder.append("]");
        return builder.toString();
    }

    @Override
    public String toString() {
        return getName();
    }
}
