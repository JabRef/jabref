package org.jabref.logic.util;

/// Specifies how metadata is read and written.
public class MetadataSerializationConfiguration {
    /// Character used for quoting in the string representation.
    public static final char GROUP_QUOTE_CHAR = '\\';

    /// Group Type suffix (part of the GroupType)
    public static final String GROUP_TYPE_SUFFIX = ":";

    /// For separating units (e.g. name and hierarchic context) in the string representation
    public static final String GROUP_UNIT_SEPARATOR = ";";

    /// Identifier for {@link org.jabref.model.groups.WordKeywordGroup} and {@link org.jabref.model.groups.RegexKeywordGroup}.
    public static final String KEYWORD_GROUP_ID = "KeywordGroup:";

    /// Identifier for {@link org.jabref.model.groups.AllEntriesGroup}.
    public static final String ALL_ENTRIES_GROUP_ID = "AllEntriesGroup:";

    /// Old identifier for {@link org.jabref.model.groups.ExplicitGroup} (explicitly contained a list of {@link org.jabref.model.entry.BibEntry}).
    public static final String LEGACY_EXPLICIT_GROUP_ID = "ExplicitGroup:";

    /// Identifier for {@link org.jabref.model.groups.ExplicitGroup}.
    public static final String EXPLICIT_GROUP_ID = "StaticGroup:";

    /// Identifier for {@link org.jabref.model.groups.SearchGroup}.
    public static final String SEARCH_GROUP_ID = "SearchGroup:";

    /// Identifier for {@link org.jabref.model.groups.AutomaticPersonsGroup}.
    public static final String AUTOMATIC_PERSONS_GROUP_ID = "AutomaticPersonsGroup:";

    /// Identifier for {@link org.jabref.model.groups.AutomaticKeywordGroup}.
    public static final String AUTOMATIC_KEYWORD_GROUP_ID = "AutomaticKeywordGroup:";

    /// Identifier for {@link org.jabref.model.groups.TexGroup}.
    public static final String TEX_GROUP_ID = "TexGroup:";

    /// Identifier for [org.jabref.model.groups.AutomaticDateGroup].
    public static final String AUTOMATIC_DATE_GROUP_ID = "AutomaticDateGroup:";

    /// Identifier for [org.jabref.model.groups.DirectoryGroup].
    public static final String DIRECTORY_GROUP_ID = "DirectoryGroup:";

    private MetadataSerializationConfiguration() {
    }
}
