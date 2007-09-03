/*
 * Created on 09-Dec-2003
 */
package net.sf.jabref.labelPattern;

import java.util.ArrayList;
import java.util.Hashtable;

/**
 * A small table, where an entry type is associated with a label pattern (an
 * <code>ArrayList</code>). A parent LabelPattern can be set.
 * 
 * @author Ulrik Stervbo (ulriks AT ruc.dk)
 */
public class LabelPattern extends Hashtable<String, ArrayList<String>> {

    /**
     * The parent of this LabelPattern.
     */
    protected LabelPattern parent = null;

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
        put(type, LabelPatternUtil.split(pattern));
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
        if (containsKey(type) && parent != null) {
            remove(type);
        }
    }

    public void removeLabelPattern(String type, boolean sure) {

        if (containsKey(type) && sure) {
            remove(type);
        }
    }

    /**
     * Gets an object for a desired label from this LabelPattern or one of it's
     * parents. This method first tries to obtain the object from this
     * LabelPattern via the <code>get</code> method of <code>Hashtable</code>.
     * If this fails, we try the parent.
     * 
     * @param key
     *            a <code>String</code>
     * @return the object for the given key
     * @throws NullPointerException
     */
    public final ArrayList<String> getValue(String key) {
        ArrayList<String> result = get(key); // throws the
                                                // NullPointerException
        // Test to see if we found anything
        if (result == null) {
            if (parent != null) {
                result = parent.getValue(key);
            }
            if (result == null) {
                // Not found - return the default value
                return LabelPatternUtil.DEFAULT_LABELPATTERN;
            }
        }

        return result;
    }

    /**
     * Checks whether this pattern is customized or the default value.
     */
    public final boolean isDefaultValue(String key) {
        Object _obj = get(key);
        return _obj == null;
    }
}
