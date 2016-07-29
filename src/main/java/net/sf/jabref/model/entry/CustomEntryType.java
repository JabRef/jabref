/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.model.entry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Objects;

/**
 * This class is used to represent customized entry types.
 */
public class CustomEntryType implements EntryType {

    public static final String ENTRYTYPE_FLAG = "jabref-entrytype: ";
    private final String name;
    private final List<String> required;
    private final List<String> optional;
    private final List<String> primaryOptional;

    public CustomEntryType(String name, List<String> required, List<String> primaryOptional, List<String> secondaryOptional) {
        this.name = EntryUtil.capitalizeFirst(name);
        this.primaryOptional = primaryOptional;
        this.required = required;
        this.optional = Stream.concat(primaryOptional.stream(), secondaryOptional.stream()).collect(Collectors.toList());
    }

    public CustomEntryType(String name, List<String> required, List<String> optional) {
        this.name = EntryUtil.capitalizeFirst(name);
        this.required = required;
        this.optional = optional;
        this.primaryOptional = optional;
    }

    public CustomEntryType(String name, String required, String optional) {
        this(name, Arrays.asList(required.split(";")), Arrays.asList(optional.split(";")));
    }

    public static Optional<CustomEntryType> parse(String comment) {
        String rest = comment.substring(ENTRYTYPE_FLAG.length());
        int indexEndOfName = rest.indexOf(':');
        if(indexEndOfName < 0) {
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
            return Objects.equal(name, ((CustomEntryType) o).name);
        } else {
            return false;
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<String> getOptionalFields() {
        return Collections.unmodifiableList(optional);
    }

    @Override
    public List<String> getRequiredFields() {
        return Collections.unmodifiableList(required);
    }

    @Override
    public List<String> getPrimaryOptionalFields() {
        return Collections.unmodifiableList(primaryOptional);
    }

    @Override
    public List<String> getSecondaryOptionalFields() {
        List<String> result = new ArrayList<>(optional);
        result.removeAll(primaryOptional);
        return Collections.unmodifiableList(result);
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
