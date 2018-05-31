package org.jabref.model.entry;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.model.strings.StringUtil;

/**
 * This class is used to represent customized entry types.
 */
public class CustomEntryType implements EntryType {

    public static final String ENTRYTYPE_FLAG = "jabref-entrytype: ";
    private final String name;
    private final Set<String> required;
    private final Set<String> optional;
    private final Set<String> primaryOptional;

    public CustomEntryType(String name, Collection<String> required, Collection<String> primaryOptional, Collection<String> secondaryOptional) {
        this.name = StringUtil.capitalizeFirst(name);
        this.primaryOptional = new LinkedHashSet<>(primaryOptional);
        this.required = new LinkedHashSet<>(required);
        this.optional = Stream.concat(primaryOptional.stream(), secondaryOptional.stream()).collect(Collectors.toSet());
    }

    public CustomEntryType(String name, Collection<String> required, Collection<String> optional) {
        this.name = StringUtil.capitalizeFirst(name);
        this.required = new LinkedHashSet<>(required);
        this.optional = new LinkedHashSet<>(optional);
        this.primaryOptional = new LinkedHashSet<>(optional);
    }

    public CustomEntryType(String name, String required, String optional) {
        this(name, Arrays.asList(required.split(";")), Arrays.asList(optional.split(";")));
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
    public Set<String> getOptionalFields() {
        return Collections.unmodifiableSet(optional);
    }

    @Override
    public Set<String> getRequiredFields() {
        return Collections.unmodifiableSet(required);
    }

    @Override
    public Set<String> getPrimaryOptionalFields() {
        return Collections.unmodifiableSet(primaryOptional);
    }

    @Override
    public Set<String> getSecondaryOptionalFields() {
        Set<String> result = new LinkedHashSet<>(optional);
        result.removeAll(primaryOptional);
        return Collections.unmodifiableSet(result);
    }

    /**
     * Get a String describing the required field set for this entry type.
     *
     * @return Description of required field set for storage in preferences or BIB file.
     */
    public String getRequiredFieldsString() {
        return String.join(";", required);
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
        builder.append(getRequiredFieldsString());
        builder.append("] opt[");
        builder.append(String.join(";", getOptionalFields()));
        builder.append("]");
        return builder.toString();
    }
}
