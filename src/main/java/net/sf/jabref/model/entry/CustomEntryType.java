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

import net.sf.jabref.model.database.BibtexDatabase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class is used to represent customized entry types.
 */
public class CustomEntryType extends BibtexEntryType {
    private static final Log LOGGER = LogFactory.getLog(CustomEntryType.class);

    public static final String ENTRYTYPE_FLAG = "jabref-entrytype: ";
    private final String name;
    private String[] required;
    private final String[] optional;
    private String[] priOpt;
    private String[][] reqSets; // Sets of either-or required fields, if any

    public CustomEntryType(String name, List<String> required, List<String> priOpt, List<String> secOpt) {
        this(name, required.toArray(new String[required.size()]), priOpt.toArray(new String[priOpt.size()]),
                secOpt.toArray(new String[secOpt.size()]));
    }

    public CustomEntryType(String name, String[] required, String[] priOpt, String[] secOpt) {
        this.name = EntryUtil.capitalizeFirst(name);
        parseRequiredFields(required);
        this.priOpt = priOpt;
        optional = EntryUtil.arrayConcat(priOpt, secOpt);
    }

    public CustomEntryType(String name, List<String> required, List<String> optional) {
        this(name, required.toArray(new String[required.size()]), optional.toArray(new String[optional.size()]));
    }

    public CustomEntryType(String name, String[] required, String[] optional) {
        this(name, required, optional, new String[0]);
    }

    public CustomEntryType(String name, String required, String optional) {
        this.name = EntryUtil.capitalizeFirst(name);
        if (required.isEmpty()) {
            this.required = new String[0];
        } else {
            parseRequiredFields(required);

        }
        if (optional.isEmpty()) {
            this.optional = new String[0];
        } else {
            this.optional = optional.split(";");
        }
    }

    private void parseRequiredFields(String req) {
        String[] parts = req.split(";");
        parseRequiredFields(parts);
    }

    private void parseRequiredFields(String[] parts) {
        ArrayList<String> fields = new ArrayList<>();
        ArrayList<String[]> sets = new ArrayList<>();
        for (String part : parts) {
            String[] subParts = part.split("/");
            Collections.addAll(fields, subParts);
            // Check if we have either/or fields:
            if (subParts.length > 1) {
                sets.add(subParts);
            }
        }
        required = fields.toArray(new String[fields.size()]);
        if (!sets.isEmpty()) {
            reqSets = sets.toArray(new String[sets.size()][]);
        }
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

    @Override
    public List<String> getRequiredFieldsForCustomization() {
        return Arrays.asList(getRequiredFieldsString().split(";"));
    }

    /**
     * Check whether this entry's required fields are set, taking crossreferenced entries and
     * either-or fields into account:
     *
     * @param entry    The entry to check.
     * @param database The entry's database.
     * @return True if required fields are set, false otherwise.
     */
    @Override
    public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
        // First check if the bibtex key is set:
        if (entry.getCiteKey() == null) {
            return false;
        }
        // Then check other fields:
        boolean[] isSet = new boolean[required.length];
        // First check for all fields, whether they are set here or in a crossref'd entry:
        for (int i = 0; i < required.length; i++) {
            isSet[i] = BibtexDatabase.getResolvedField(required[i], entry, database) != null;
        }
        // Then go through all fields. If a field is not set, see if it is part of an either-or
        // set where another field is set. If not, return false:
        for (int i = 0; i < required.length; i++) {
            if (!isSet[i]) {
                if (!isCoupledFieldSet(required[i], entry, database)) {
                    return false;
                }
            }
        }
        // Passed all fields, so return true:
        return true;
    }

    private boolean isCoupledFieldSet(String field, BibtexEntry entry, BibtexDatabase database) {
        if (reqSets == null) {
            return false;
        }
        for (String[] reqSet : reqSets) {
            boolean takesPart = false;
            boolean oneSet = false;
            for (String aReqSet : reqSet) {
                // If this is the field we're looking for, note that the field is part of the set:
                if (aReqSet.equalsIgnoreCase(field)) {
                    takesPart = true;
                } else if (BibtexDatabase.getResolvedField(aReqSet, entry, database) != null) {
                    oneSet = true;
                }
            }
            // Ths the field is part of the set, and at least one other field is set, return true:
            if (takesPart && oneSet) {
                return true;
            }
        }
        // No hits, so return false:
        return false;
    }

    /**
     * Get a String describing the required field set for this entry type.
     *
     * @return Description of required field set for storage in preferences or bib file.
     */
    public String getRequiredFieldsString() {
        StringBuilder sb = new StringBuilder();
        int reqSetsPiv = 0;
        for (int i = 0; i < required.length; i++) {
            if ((reqSets == null) || (reqSetsPiv == reqSets.length)) {
                sb.append(required[i]);
            } else if (required[i].equals(reqSets[reqSetsPiv][0])) {
                for (int j = 0; j < reqSets[reqSetsPiv].length; j++) {
                    sb.append(reqSets[reqSetsPiv][j]);
                    if (j < (reqSets[reqSetsPiv].length - 1)) {
                        sb.append('/');
                    }
                }
                // Skip next n-1 fields:
                i += reqSets[reqSetsPiv].length - 1;
                reqSetsPiv++;
            } else {
                sb.append(required[i]);
            }
            if (i < (required.length - 1)) {
                sb.append(';');
            }

        }
        return sb.toString();
    }

}
