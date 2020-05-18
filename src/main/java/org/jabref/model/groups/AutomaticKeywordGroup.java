package org.jabref.model.groups;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Keyword;
import org.jabref.model.entry.KeywordList;
import org.jabref.model.entry.field.Field;
import org.jabref.model.strings.StringUtil;

public class AutomaticKeywordGroup extends AutomaticGroup {

    private final Character keywordDelimiter;
    private final Character keywordHierarchicalDelimiter;
    private final Field field;

    public AutomaticKeywordGroup(String name, GroupHierarchyType context, Field field, Character keywordDelimiter, Character keywordHierarchicalDelimiter) {
        super(name, context);
        this.field = field;
        this.keywordDelimiter = keywordDelimiter;
        this.keywordHierarchicalDelimiter = keywordHierarchicalDelimiter;
    }

    public Character getKeywordHierarchicalDelimiter() {
        return keywordHierarchicalDelimiter;
    }

    public Character getKeywordDelimiter() {
        return keywordDelimiter;
    }

    public Field getField() {
        return field;
    }

    @Override
    public AbstractGroup deepCopy() {
        return new AutomaticKeywordGroup(this.name.getValue(), this.context, field, this.keywordDelimiter, keywordHierarchicalDelimiter);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AutomaticKeywordGroup that = (AutomaticKeywordGroup) o;
        return Objects.equals(keywordDelimiter, that.keywordDelimiter) &&
                Objects.equals(field, that.field);
    }

    @Override
    public int hashCode() {
        return Objects.hash(keywordDelimiter, field);
    }

    @Override
    public Set<GroupTreeNode> createSubgroups(BibEntry entry) {
        KeywordList keywordList = entry.getFieldAsKeywords(field, keywordDelimiter);
        return keywordList.stream()
                          .filter(keyword -> StringUtil.isNotBlank(keyword.get()))
                          .map(this::createGroup)
                          .collect(Collectors.toSet());
    }

    private GroupTreeNode createGroup(Keyword keywordChain) {
        WordKeywordGroup rootGroup = new WordKeywordGroup(
                keywordChain.get(),
                GroupHierarchyType.INCLUDING,
                field,
                keywordChain.getPathFromRootAsString(keywordHierarchicalDelimiter),
                true,
                keywordDelimiter,
                true);
        GroupTreeNode root = new GroupTreeNode(rootGroup);
        keywordChain.getChild()
                    .map(this::createGroup)
                    .ifPresent(root::addChild);
        return root;
    }
}
