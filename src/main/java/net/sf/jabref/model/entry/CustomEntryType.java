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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class is used to represent customized entry types.
 */
public class CustomEntryType implements EntryType {
    private static final Log LOGGER = LogFactory.getLog(CustomEntryType.class);

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

    @Override
    public int compareTo(EntryType o) {
        return getName().compareTo(o.getName());
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
        return Collections.unmodifiableList(EntryUtil.getRemainder(optional, primaryOptional));
    }

    /**
     * Get a String describing the required field set for this entry type.
     *
     * @return Description of required field set for storage in preferences or bib file.
     */
    public String getRequiredFieldsString() {
        StringBuilder serialization = new StringBuilder();

        for (int i = 0; i < required.size(); i++) {
            serialization.append(required.get(i));

            if (i < (required.size() - 1)) {
                serialization.append(';');
            }
        }
        return serialization.toString();
    }
}
