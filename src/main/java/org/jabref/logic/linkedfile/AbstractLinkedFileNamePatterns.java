package org.jabref.logic.linkedfile;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.jabref.logic.citationkeypattern.Pattern;
import org.jabref.model.entry.types.EntryType;

/**
 * A small table, where an entry type is associated with a Bibtex key pattern (an
 * <code>ArrayList</code>). A parent LinkedFileNamePattern can be set.
 */
public abstract class AbstractLinkedFileNamePatterns {

    protected Pattern defaultPattern = Pattern.NULL_PATTERN;

    protected Map<EntryType, Pattern> data = new HashMap<>();

    public void addLinkedFileNamePattern(EntryType type, String pattern) {
        data.put(type, new Pattern(pattern));
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("AbstractLinkedFileNamePattern{");
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
        AbstractLinkedFileNamePatterns that = (AbstractLinkedFileNamePatterns) o;
        return Objects.equals(defaultPattern, that.defaultPattern) && Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(defaultPattern, data);
    }

    /**
     * Gets an object for a desired key from this LinkedFileNamePattern or one of it's parents (in the case of
     * DatabaseLinkedFileNamePattern). This method first tries to obtain the object from this LinkedFileNamePattern via the
     * <code>get</code> method of <code>Hashtable</code>. If this fails, we try the default.<br /> If that fails, we try
     * the parent.<br /> If that fails, we return the DEFAULT_LABELPATTERN<br />
     *
     * @param entryType a <code>String</code>
     * @return the list of Strings for the given key. First entry: the complete key
     */
    public Pattern getValue(EntryType entryType) {
        Pattern result = data.get(entryType);
        //  Test to see if we found anything
        if (result == null) {
            // check default value
            result = getDefaultValue();
            if (result == null || Pattern.NULL_PATTERN.equals(result)) {
                // we are the "last" to ask
                // we don't have anything left
                return getLastLevelLinkedFileNamePattern(entryType);
            }
        }
        return result;
    }

    /**
     * Checks whether this pattern is customized or the default value.
     */
    public final boolean isDefaultValue(EntryType entryType) {
        return data.get(entryType) == null;
    }

    /**
     * This method is called "...Value" to be in line with the other methods
     *
     * @return null if not available.
     */
    public Pattern getDefaultValue() {
        return this.defaultPattern;
    }

    /**
     * Sets the DEFAULT PATTERN for this key pattern
     *
     * @param bibtexKeyPattern the pattern to store
     */
    public void setDefaultValue(String bibtexKeyPattern) {
        Objects.requireNonNull(bibtexKeyPattern);
        this.defaultPattern = new Pattern(bibtexKeyPattern);
    }

    public Set<EntryType> getAllKeys() {
        return data.keySet();
    }

    public Map<EntryType, Pattern> getPatterns() {
        return data.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public abstract Pattern getLastLevelLinkedFileNamePattern(EntryType key);
}
