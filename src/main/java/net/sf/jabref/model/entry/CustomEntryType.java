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
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class is used to represent customized entry types.
 */
public class CustomEntryType implements EntryType {
    private static final Log LOGGER = LogFactory.getLog(CustomEntryType.class);

    public static final String ENTRYTYPE_FLAG = "jabref-entrytype: ";
    private final String name;
    private final String[] required;
    private final String[] optional;
    private final String[] priOpt;

    public CustomEntryType(String name, List<String> required, List<String> priOpt, List<String> secOpt) {
        this(name, required.toArray(new String[required.size()]), priOpt.toArray(new String[priOpt.size()]),
                secOpt.toArray(new String[secOpt.size()]));
    }

    public CustomEntryType(String name, String[] required, String[] priOpt, String[] secOpt) {
        this.name = EntryUtil.capitalizeFirst(name);
        this.priOpt = priOpt;
        this.required = required;
        optional = EntryUtil.arrayConcat(priOpt, secOpt);
    }

    public CustomEntryType(String name, List<String> required, List<String> optional) {
        this(name, required.toArray(new String[required.size()]), optional.toArray(new String[optional.size()]));
    }

    public CustomEntryType(String name, String[] required, String[] optional) {
        this(name, required, optional, new String[0]);
    }

    public CustomEntryType(String name, String required, String optional) {
        this(name, required.split(";"), optional.split(";"), new String[0]);
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
        return Arrays.asList(optional);
    }

    @Override
    public List<String> getRequiredFields() {
        return Arrays.asList(required);
    }

    @Override
    public List<String> getPrimaryOptionalFields() {
        return Arrays.asList(priOpt);
    }

    @Override
    public List<String> getSecondaryOptionalFields() {
        return Arrays.asList(EntryUtil.getRemainder(optional, priOpt));
    }

    /**
     * Get a String describing the required field set for this entry type.
     *
     * @return Description of required field set for storage in preferences or bib file.
     */
    public String getRequiredFieldsString() {
        StringBuilder serialization = new StringBuilder();

        for (int i = 0; i < required.length; i++) {
            serialization.append(required[i]);

            if (i < (required.length - 1)) {
                serialization.append(';');
            }
        }
        return serialization.toString();
    }

}
