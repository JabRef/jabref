package org.jabref.logic.util;

import org.jabref.model.groups.AllEntriesGroup;
import org.jabref.model.groups.AutomaticKeywordGroup;
import org.jabref.model.groups.AutomaticPersonsGroup;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.RegexKeywordGroup;
import org.jabref.model.groups.SearchGroup;
import org.jabref.model.groups.TexGroup;
import org.jabref.model.groups.WordKeywordGroup;

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
     * Old identifier for {@link ExplicitGroup} (explicitly contained a list of {@link org.jabref.model.entry.BibEntry}).
     */
    public static final String LEGACY_EXPLICIT_GROUP_ID = "ExplicitGroup:";

    /**
     * Identifier for {@link ExplicitGroup}.
     */
    public static final String EXPLICIT_GROUP_ID = "StaticGroup:";

    /**
     * Identifier for {@link SearchGroup}.
     */
    public static final String SEARCH_GROUP_ID = "SearchGroup:";

    /**
     * Identifier for {@link AutomaticPersonsGroup}.
     */
    public static final String AUTOMATIC_PERSONS_GROUP_ID = "AutomaticPersonsGroup:";

    /**
     * Identifier for {@link AutomaticKeywordGroup}.
     */
    public static final String AUTOMATIC_KEYWORD_GROUP_ID = "AutomaticKeywordGroup:";

    /**
     * Identifier for {@link TexGroup}.
     */
    public static final String TEX_GROUP_ID = "TexGroup:";

    private MetadataSerializationConfiguration() {
    }
}
