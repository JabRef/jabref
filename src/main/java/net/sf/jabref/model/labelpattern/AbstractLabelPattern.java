/*  Copyright (C) 2003-2015 JabRef contributors.
                  2003 Ulrik Stervbo (ulriks AT ruc.dk)

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
package net.sf.jabref.model.labelpattern;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * A small table, where an entry type is associated with a label pattern (an
 * <code>ArrayList</code>). A parent LabelPattern can be set.
 */
public abstract class AbstractLabelPattern {

    protected List<String> defaultPattern;

    protected Map<String, List<String>> data = new HashMap<>();

    public void addLabelPattern(String type, String pattern) {
        data.put(type, AbstractLabelPattern.split(pattern));
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("AbstractLabelPattern{");
        sb.append("defaultPattern=").append(defaultPattern);
        sb.append(", data=").append(data);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }
        AbstractLabelPattern that = (AbstractLabelPattern) o;
        return Objects.equals(defaultPattern, that.defaultPattern) && Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(defaultPattern, data);
    }

    /**
     * Remove a label pattern from the LabelPattern.
     *
     * @param type a <code>String</code>
     */
    public void removeLabelPattern(String type) {
        if (data.containsKey(type)) {
            data.remove(type);
        }
    }

    /**
     * Gets an object for a desired label from this LabelPattern or one of it's
     * parents (in the case of DatabaseLAbelPattern). This method first tries to obtain the object from this
     * LabelPattern via the <code>get</code> method of <code>Hashtable</code>.
     * If this fails, we try the default.<br />
     * If that fails, we try the parent.<br />
     * If that fails, we return the DEFAULT_LABELPATTERN<br />
     *
     * @param key a <code>String</code>
     * @return the list of Strings for the given key. First entry: the complete key
     */
    public List<String> getValue(String key) {
        List<String> result = data.get(key);
        //  Test to see if we found anything
        if (result == null) {
            // check default value
            result = getDefaultValue();
            if (result == null) {
                // we are the "last" to ask
                // we don't have anything left
                return getLastLevelLabelPattern(key);
            }
        }
        return result;

    }

    /**
     * This method takes a string of the form [field1]spacer[field2]spacer[field3]...,
     * where the fields are the (required) fields of a BibTex entry. The string is split
     * into fields and spacers by recognizing the [ and ].
     *
     * @param labelPattern a <code>String</code>
     * @return an <code>ArrayList</code> The first item of the list
     * is a string representation of the key pattern (the parameter),
     * the remaining items are the fields
     */
    public static List<String> split(String labelPattern) {
        // A holder for fields of the entry to be used for the key
        List<String> fieldList = new ArrayList<>();

        // Before we do anything, we add the parameter to the ArrayLIst
        fieldList.add(labelPattern);

        StringTokenizer tok = new StringTokenizer(labelPattern, "[]", true);
        while (tok.hasMoreTokens()) {
            fieldList.add(tok.nextToken());
        }
        return fieldList;
    }

    /**
     * Checks whether this pattern is customized or the default value.
     */
    public final boolean isDefaultValue(String key) {
        return data.get(key) == null;
    }

    /**
     * This method is called "...Value" to be in line with the other methods
     *
     * @return null if not available.
     */
    public List<String> getDefaultValue() {
        return this.defaultPattern;
    }

    /**
     * Sets the DEFAULT PATTERN for this label pattern
     * @param labelPattern the pattern to store
     */
    public void setDefaultValue(String labelPattern) {
        this.defaultPattern = AbstractLabelPattern.split(labelPattern);
    }

    public Set<String> getAllKeys() {
        return data.keySet();
    }

    public abstract List<String> getLastLevelLabelPattern(String key);
}
