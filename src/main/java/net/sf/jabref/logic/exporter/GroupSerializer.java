package net.sf.jabref.logic.exporter;

import java.util.Set;
import java.util.TreeSet;

import net.sf.jabref.logic.util.MetadataSerializationConfiguration;
import net.sf.jabref.model.groups.ExplicitGroup;
import net.sf.jabref.model.groups.KeywordGroup;
import net.sf.jabref.model.groups.RegexKeywordGroup;
import net.sf.jabref.model.strings.StringUtil;

/**
 * Created by Tobias on 12/5/2016.
 */
public class GroupSerializer {
    public static String serializeAllEntriesGroup() {
        return MetadataSerializationConfiguration.ALL_ENTRIES_GROUP_ID;
    }

    public String serializeExplicitGroup(ExplicitGroup group) {
        StringBuilder sb = new StringBuilder();
        sb.append(MetadataSerializationConfiguration.EXPLICIT_GROUP_ID);
        sb.append(StringUtil.quote(group.getName(), MetadataSerializationConfiguration.GROUP_UNIT_SEPARATOR,
                MetadataSerializationConfiguration.GROUP_QUOTE_CHAR));
        sb.append(MetadataSerializationConfiguration.GROUP_UNIT_SEPARATOR);
        sb.append(group.getContext().ordinal());
        sb.append(MetadataSerializationConfiguration.GROUP_UNIT_SEPARATOR);

        // write legacy entry keys in well-defined order for CVS compatibility
        Set<String> sortedKeys = new TreeSet<>();
        sortedKeys.addAll(group.getLegacyEntryKeys());

        for (String sortedKey : sortedKeys) {
            sb.append(StringUtil.quote(sortedKey, MetadataSerializationConfiguration.GROUP_UNIT_SEPARATOR,
                    MetadataSerializationConfiguration.GROUP_QUOTE_CHAR));
            sb.append(MetadataSerializationConfiguration.GROUP_UNIT_SEPARATOR);
        }
        return sb.toString();
    }

    public String serializeKeywordGroup(KeywordGroup group) {
        Boolean isRegex = group instanceof RegexKeywordGroup;
        StringBuilder sb = new StringBuilder();
        sb.append(MetadataSerializationConfiguration.KEYWORD_GROUP_ID);
        sb.append(StringUtil.quote(group.getName(),
                MetadataSerializationConfiguration.GROUP_UNIT_SEPARATOR,
                MetadataSerializationConfiguration.GROUP_QUOTE_CHAR));
        sb.append(MetadataSerializationConfiguration.GROUP_UNIT_SEPARATOR);
        sb.append(group.getContext().ordinal());
        sb.append(MetadataSerializationConfiguration.GROUP_UNIT_SEPARATOR);
        sb.append(StringUtil.quote(group.getSearchField(),
                MetadataSerializationConfiguration.GROUP_UNIT_SEPARATOR,
                MetadataSerializationConfiguration.GROUP_QUOTE_CHAR));
        sb.append(MetadataSerializationConfiguration.GROUP_UNIT_SEPARATOR);
        sb.append(StringUtil.quote(group.getSearchExpression(),
                MetadataSerializationConfiguration.GROUP_UNIT_SEPARATOR,
                MetadataSerializationConfiguration.GROUP_QUOTE_CHAR));
        sb.append(MetadataSerializationConfiguration.GROUP_UNIT_SEPARATOR);
        sb.append(StringUtil.booleanToBinaryString(group.isCaseSensitive()));
        sb.append(MetadataSerializationConfiguration.GROUP_UNIT_SEPARATOR);
        sb.append(StringUtil.booleanToBinaryString(isRegex));
        sb.append(MetadataSerializationConfiguration.GROUP_UNIT_SEPARATOR);
        return sb.toString();
    }
}
