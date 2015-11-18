/*
Copyright (C) 2003 David Weitzman, Morten O. Alver

All programs in this directory and
subdirectories are published under the GNU General Public License as
described below.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or (at
your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
USA

Further information about the GNU GPL is available at:
http://www.gnu.org/copyleft/gpl.ja.html

Note:
Modified for use in JabRef.

*/
package net.sf.jabref.model.entry;

import net.sf.jabref.model.database.BibtexDatabase;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Abstract base class for all BibTex entry types.
 */
public abstract class BibtexEntryType implements EntryType {
    private final List<String> requiredFields;
    private final List<String> optionalFields;

    public BibtexEntryType() {
        requiredFields = new ArrayList<>();
        optionalFields = new ArrayList<>();

        // key is always required
        requiredFields.add("bibtexkey");
    }

    @Override
    public EntryTypes getEntryType() {
        return EntryTypes.BIBTEX;
    }

    @Override
    public List<String> getOptionalFields() {
        return Collections.unmodifiableList(optionalFields);
    }

    @Override
    public List<String> getRequiredFields() {
        return Collections.unmodifiableList(requiredFields);
    }

    void addAllOptional(String... fieldNames) {
        optionalFields.addAll(Arrays.asList(fieldNames));
    }

    void addAllRequired(String... fieldNames) {
        requiredFields.addAll(Arrays.asList(fieldNames));
    }

    @Override
    public boolean isRequired(String field) {
        List<String> requiredFields = getRequiredFields();

        if (requiredFields == null) {
            return false;
        }
        return requiredFields.contains(field);
    }

    @Override
    public boolean isOptional(String field) {
        List<String> optionalFields = getOptionalFields();

        if (optionalFields == null) {
            return false;
        }
        return optionalFields.contains(field);
    }

    @Override
    public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
        return entry.allFieldsPresent(getRequiredFields(), database);
    }

    /**
     * Get an array of the required fields in a form appropriate for the entry customization
     * dialog - that is, the either-or fields together and separated by slashes.
     *
     * @return Array of the required fields in a form appropriate for the entry customization dialog.
     */
    @Override
    public List<String> getRequiredFieldsForCustomization() {
        return getRequiredFields();
    }

    @Override
    public int compareTo(EntryType o) {
        return getName().compareTo(o.getName());
    }

    /**
     * TODO: remove all methods from here on
     * @return
     */
    public List<String> getPrimaryOptionalFields() {
        return getOptionalFields();
    }

    public List<String> getSecondaryOptionalFields() {
        List<String> optionalFields = getOptionalFields();

        if (optionalFields == null) {
            return new ArrayList<>(0);
        }

        return optionalFields.stream().filter(field -> !isPrimary(field)).collect(Collectors.toList());
    }

    private boolean isPrimary(String field) {
        List<String> primaryFields = getPrimaryOptionalFields();

        if (primaryFields == null) {
            return false;
        }
        return primaryFields.contains(field);
    }
}
