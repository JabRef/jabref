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
package net.sf.jabref.logic.labelPattern;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * A small table, where an entry type is associated with a label pattern (an
 * <code>ArrayList</code>). A parent LabelPattern can be set.
 */
public class LabelPattern {

    private ArrayList<String> defaultPattern;

    /**
     * The parent of this LabelPattern.
     */
    private LabelPattern parent;

    private Hashtable<String, ArrayList<String>> data = new Hashtable<>();

    public LabelPattern() {
    }

    public LabelPattern(LabelPattern parent) {
        this.parent = parent;
    }

    /**
     * Sets the parent LabelPattern.
     * 
     * @param parent
     *            a <code>String</code>
     */
    public void setParent(LabelPattern parent) {
        this.parent = parent;
    }

    /**
     * Get the parent LabelPattern
     * 
     * @return the parent LabelPattern
     */
    public LabelPattern getParent() {
        return parent;
    }

    public void addLabelPattern(String type, String pattern) {
        data.put(type, LabelPatternUtil.split(pattern));
    }

    /**
     * Remove a label pattern from the LabelPattern. No key patterns can be
     * removed from the very parent LabelPattern since is thought of as a
     * default. To do this, use the removeKeyPattern(String type, boolean sure)
     * 
     * @param type
     *            a <code>String</code>
     */
    public void removeLabelPattern(String type) {
        if (data.containsKey(type) && parent != null) {
            data.remove(type);
        }
    }

    public void removeLabelPattern(String type, boolean sure) {
        if (data.containsKey(type) && sure) {
            data.remove(type);
        }
    }

    /**
     * Gets an object for a desired label from this LabelPattern or one of it's
     * parents. This method first tries to obtain the object from this
     * LabelPattern via the <code>get</code> method of <code>Hashtable</code>.
     * If this fails, we try the default.<br />
     * If that fails, we try the parent.<br />
     * If that fails, we return the DEFAULT_LABELPATTERN<br />
     * 
     * @param key a <code>String</code>
     * @return the list of Strings for the given key
     */
    public final ArrayList<String> getValue(String key) {
        ArrayList<String> result = data.get(key);
        // Test to see if we found anything
        if (result == null) {
            // check default value
            result = getDefaultValue();
            if (result == null) {
                // no default value, ask parent
                if (parent != null) {
                    result = parent.getValue(key);
                    // parent will definitely return something != null
                } else {
                    // we are the "last" parent
                    // we don't have anything left
                    // return the global default pattern
                    return LabelPatternUtil.DEFAULT_LABELPATTERN;
                }
            }
        }
        return result;
    }

    /**
     * Checks whether this pattern is customized or the default value.
     */
    public final boolean isDefaultValue(String key) {
        Object _obj = data.get(key);
        return _obj == null;
    }

    /**
     * This method is called "...Value" to be in line with the other methods
     * @return
     */
    public ArrayList<String> getDefaultValue() {
        return this.defaultPattern;
    }

    /**
     * Sets the DEFAULT PATTERN for this label pattern
     * @param labelPattern the pattern to store
     */
    public void setDefaultValue(String labelPattern) {
        this.defaultPattern = LabelPatternUtil.split(labelPattern);
    }

    public Enumeration<String> getAllKeys() {
        return data.keys();
    }
}
