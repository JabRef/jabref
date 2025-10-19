package org.jabref.logic.citationkeypattern;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.jabref.model.entry.types.EntryType;

import org.jspecify.annotations.NonNull;

/// A small table, where an entry type is associated with a citation key pattern.
/// A parent CitationKeyPattern can be set.
public abstract class AbstractCitationKeyPatterns {

    protected CitationKeyPattern defaultPattern = CitationKeyPattern.NULL_CITATION_KEY_PATTERN;

    protected Map<EntryType, CitationKeyPattern> data = new HashMap<>();

    public void addCitationKeyPattern(EntryType type, String pattern) {
        data.put(type, new CitationKeyPattern(pattern));
    }

    @Override
    public String toString() {
        return "AbstractCitationKeyPattern{" +
                "defaultPattern='" + defaultPattern + '\'' +
                ", data='" + data + '\'' +
                "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }
        AbstractCitationKeyPatterns that = (AbstractCitationKeyPatterns) o;
        return Objects.equals(defaultPattern, that.defaultPattern) && Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(defaultPattern, data);
    }

    /**
     * Gets an object for a desired key from this CitationKeyPattern or one of it's parents (in the case of
     * DatabaseCitationKeyPattern). This method first tries to obtain the object from this CitationKeyPattern via the
     * <code>get</code> method of <code>Hashtable</code>. If this fails, we try the default.<br /> If that fails, we try
     * the parent.<br /> If that fails, we return the DEFAULT_LABELPATTERN<br />
     *
     * @param entryType a <code>String</code>
     * @return the list of Strings for the given key. First entry: the complete key
     */
    public CitationKeyPattern getValue(EntryType entryType) {
        CitationKeyPattern result = data.get(entryType);
        //  Test to see if we found anything
        if (result == null) {
            // check default value
            result = getDefaultValue();
            if (result == null || CitationKeyPattern.NULL_CITATION_KEY_PATTERN.equals(result)) {
                // we are the "last" to ask
                // we don't have anything left
                return getLastLevelCitationKeyPattern(entryType);
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
    public CitationKeyPattern getDefaultValue() {
        return this.defaultPattern;
    }

    /**
     * Sets the DEFAULT PATTERN for this key pattern
     *
     * @param bibtexKeyPattern the pattern to store
     */
    public void setDefaultValue(@NonNull String bibtexKeyPattern) {
        this.defaultPattern = new CitationKeyPattern(bibtexKeyPattern);
    }

    public Set<EntryType> getAllKeys() {
        return data.keySet();
    }

    public Map<EntryType, CitationKeyPattern> getPatterns() {
        return data.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public abstract CitationKeyPattern getLastLevelCitationKeyPattern(EntryType key);
}
