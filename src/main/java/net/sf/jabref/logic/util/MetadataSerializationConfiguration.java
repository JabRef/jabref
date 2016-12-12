package net.sf.jabref.logic.util;

import net.sf.jabref.model.groups.AllEntriesGroup;
import net.sf.jabref.model.groups.ExplicitGroup;
import net.sf.jabref.model.groups.RegexKeywordGroup;
import net.sf.jabref.model.groups.SearchGroup;
import net.sf.jabref.model.groups.WordKeywordGroup;

/**
 * Specifies how metadata is read and written.
 */
public class MetadataSerializationConfiguration {
    /**
     * Character used for quoting in the string representation.
     */
    public static final char GROUP_QUOTE_CHAR = '\\';

    /**
     * For separating units (e.g. name and hierarchic context) in the string representation
     */
    public static final String GROUP_UNIT_SEPARATOR = ";";

    /**
     * Identifier for {@link WordKeywordGroup} and {@link RegexKeywordGroup}.
     */
    public static final String KEYWORD_GROUP_ID = "KeywordGroup:";

    /**
     * Identifier for {@link AllEntriesGroup}.
     */
    public static final String ALL_ENTRIES_GROUP_ID = "AllEntriesGroup:";

    /**
     * Identifier for {@link ExplicitGroup}.
     */
    public static final String EXPLICIT_GROUP_ID = "ExplicitGroup:";

    /**
     * Identifier for {@link SearchGroup}.
     */
    public static final String SEARCH_GROUP_ID = "SearchGroup:";
}
