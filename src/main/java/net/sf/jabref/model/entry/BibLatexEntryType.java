/*
Copyright (C) 2003 David Weitzman, Morten O. Alver
Copyright (C) 2015 JabRef contributors

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Abstract base class for all BibLaTex entry types.
 */
public abstract class BibLatexEntryType implements EntryType {

    private final List<String> requiredFields;
    private final List<String> optionalFields;


    public BibLatexEntryType() {
        requiredFields = new ArrayList<>();
        optionalFields = new ArrayList<>();
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
    public List<String> getPrimaryOptionalFields() {
        return getOptionalFields();
    }

    @Override
    public List<String> getSecondaryOptionalFields() {
        List<String> myOptionalFields = getOptionalFields();

        if (myOptionalFields == null) {
            return Collections.emptyList();
        }

        return myOptionalFields.stream().filter(field -> !isPrimary(field)).collect(Collectors.toList());
    }

    private boolean isPrimary(String field) {
        List<String> primaryFields = getPrimaryOptionalFields();

        if (primaryFields == null) {
            return false;
        }
        return primaryFields.contains(field);
    }

    @Override
    public int compareTo(EntryType o) {
        return getName().compareTo(o.getName());
    }
}
