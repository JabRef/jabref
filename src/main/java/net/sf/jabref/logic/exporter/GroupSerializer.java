package net.sf.jabref.logic.exporter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import net.sf.jabref.logic.util.MetadataSerializationConfiguration;
import net.sf.jabref.model.groups.AbstractGroup;
import net.sf.jabref.model.groups.AllEntriesGroup;
import net.sf.jabref.model.groups.ExplicitGroup;
import net.sf.jabref.model.groups.GroupTreeNode;
import net.sf.jabref.model.groups.KeywordGroup;
import net.sf.jabref.model.groups.RegexKeywordGroup;
import net.sf.jabref.model.groups.SearchGroup;
import net.sf.jabref.model.strings.StringUtil;

public class GroupSerializer {
    private static String serializeAllEntriesGroup(AllEntriesGroup group) {
        return MetadataSerializationConfiguration.ALL_ENTRIES_GROUP_ID;
    }

    private String serializeExplicitGroup(ExplicitGroup group) {
        StringBuilder sb = new StringBuilder();
        sb.append(MetadataSerializationConfiguration.EXPLICIT_GROUP_ID);
        sb.append(StringUtil.quote(group.getName(), MetadataSerializationConfiguration.GROUP_UNIT_SEPARATOR, MetadataSerializationConfiguration.GROUP_QUOTE_CHAR));
        sb.append(MetadataSerializationConfiguration.GROUP_UNIT_SEPARATOR);
        sb.append(group.getHierarchicalContext().ordinal());
        sb.append(MetadataSerializationConfiguration.GROUP_UNIT_SEPARATOR);

        // write legacy entry keys in well-defined order for CVS compatibility
        Set<String> sortedKeys = new TreeSet<>();
        sortedKeys.addAll(group.getLegacyEntryKeys());

        for (String sortedKey : sortedKeys) {
            sb.append(StringUtil.quote(sortedKey, MetadataSerializationConfiguration.GROUP_UNIT_SEPARATOR, MetadataSerializationConfiguration.GROUP_QUOTE_CHAR));
            sb.append(MetadataSerializationConfiguration.GROUP_UNIT_SEPARATOR);
        }
        return sb.toString();
    }

    private String serializeKeywordGroup(KeywordGroup group) {
        Boolean isRegex = group instanceof RegexKeywordGroup;
        StringBuilder sb = new StringBuilder();
        sb.append(MetadataSerializationConfiguration.KEYWORD_GROUP_ID);
        sb.append(StringUtil.quote(group.getName(), MetadataSerializationConfiguration.GROUP_UNIT_SEPARATOR, MetadataSerializationConfiguration.GROUP_QUOTE_CHAR));
        sb.append(MetadataSerializationConfiguration.GROUP_UNIT_SEPARATOR);
        sb.append(group.getHierarchicalContext().ordinal());
        sb.append(MetadataSerializationConfiguration.GROUP_UNIT_SEPARATOR);
        sb.append(StringUtil.quote(group.getSearchField(), MetadataSerializationConfiguration.GROUP_UNIT_SEPARATOR, MetadataSerializationConfiguration.GROUP_QUOTE_CHAR));
        sb.append(MetadataSerializationConfiguration.GROUP_UNIT_SEPARATOR);
        sb.append(StringUtil.quote(group.getSearchExpression(), MetadataSerializationConfiguration.GROUP_UNIT_SEPARATOR, MetadataSerializationConfiguration.GROUP_QUOTE_CHAR));
        sb.append(MetadataSerializationConfiguration.GROUP_UNIT_SEPARATOR);
        sb.append(StringUtil.booleanToBinaryString(group.isCaseSensitive()));
        sb.append(MetadataSerializationConfiguration.GROUP_UNIT_SEPARATOR);
        sb.append(StringUtil.booleanToBinaryString(isRegex));
        sb.append(MetadataSerializationConfiguration.GROUP_UNIT_SEPARATOR);
        return sb.toString();
    }

    private String serializeSearchGroup(SearchGroup group) {
        StringBuilder sb = new StringBuilder();
        sb.append(MetadataSerializationConfiguration.SEARCH_GROUP_ID);
        sb.append(StringUtil.quote(group.getName(), MetadataSerializationConfiguration.GROUP_UNIT_SEPARATOR, MetadataSerializationConfiguration.GROUP_QUOTE_CHAR));
        sb.append(MetadataSerializationConfiguration.GROUP_UNIT_SEPARATOR);
        sb.append(group.getHierarchicalContext().ordinal());
        sb.append(MetadataSerializationConfiguration.GROUP_UNIT_SEPARATOR);
        sb.append(StringUtil.quote(group.getSearchExpression(), MetadataSerializationConfiguration.GROUP_UNIT_SEPARATOR, MetadataSerializationConfiguration.GROUP_QUOTE_CHAR));
        sb.append(MetadataSerializationConfiguration.GROUP_UNIT_SEPARATOR);
        sb.append(StringUtil.booleanToBinaryString(group.isCaseSensitive()));
        sb.append(MetadataSerializationConfiguration.GROUP_UNIT_SEPARATOR);
        sb.append(StringUtil.booleanToBinaryString(group.isRegularExpression()));
        sb.append(MetadataSerializationConfiguration.GROUP_UNIT_SEPARATOR);
        return sb.toString();
    }


    /**
     * Returns a textual representation of this node and its children. This
     * representation contains both the tree structure and the textual
     * representations of the group associated with each node.
     * Every node is one entry in the list of strings.
     *
     * @return a representation of the tree based at this node as a list of strings
     */
    public List<String> serializeTree(GroupTreeNode node) {

        List<String> representation = new ArrayList<>();

        // Append current node
        representation.add(String.valueOf(node.getLevel()) + ' ' + serializeGroup(node.getGroup()));

        // Append children
        for(GroupTreeNode child : node.getChildren()) {
            representation.addAll(serializeTree(child));
        }

        return representation;
    }

    private String serializeGroup(AbstractGroup group) {
        if (group instanceof AllEntriesGroup) {
            return serializeAllEntriesGroup((AllEntriesGroup)group);
        } else if (group instanceof ExplicitGroup) {
            return serializeExplicitGroup((ExplicitGroup)group);
        } else if (group instanceof KeywordGroup) {
            return serializeKeywordGroup((KeywordGroup)group);
        } else if (group instanceof SearchGroup) {
            return serializeSearchGroup((SearchGroup)group);
        } else {
            throw new UnsupportedOperationException("Don't know how to serialize group" + group.getClass().getName());
        }
    }
}
