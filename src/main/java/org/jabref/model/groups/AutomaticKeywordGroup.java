package org.jabref.model.groups;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Keyword;
import org.jabref.model.entry.KeywordList;
import org.jabref.model.util.OptionalUtil;

public class AutomaticKeywordGroup extends AutomaticGroup {

    private Character keywordSeperator;
    private Character keywordHierarchicalDelimiter = '>';
    private String field;

    public AutomaticKeywordGroup(String name, GroupHierarchyType context, String field, Character keywordSeperator) {
        super(name, context);
        this.field = field;
        this.keywordSeperator = keywordSeperator;
    }

    public Character getKeywordSeperator() {
        return keywordSeperator;
    }

    public String getField() {
        return field;
    }

    @Override
    public AbstractGroup deepCopy() {
        return new AutomaticKeywordGroup(this.name, this.context, field, this.keywordSeperator);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AutomaticKeywordGroup that = (AutomaticKeywordGroup) o;
        return Objects.equals(keywordSeperator, that.keywordSeperator) &&
                Objects.equals(field, that.field);
    }

    @Override
    public int hashCode() {
        return Objects.hash(keywordSeperator, field);
    }

    @Override
    public Set<GroupTreeNode> createSubgroups(BibEntry entry) {
        Optional<KeywordList> keywordList = entry.getLatexFreeField(field)
                .map(fieldValue -> KeywordList.parse(fieldValue, keywordSeperator));
        return OptionalUtil.toStream(keywordList)
                .flatMap(KeywordList::stream)
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
                keywordSeperator,
                true);
        GroupTreeNode root = new GroupTreeNode(rootGroup);
        keywordChain.getChild()
                .map(this::createGroup)
                .ifPresent(root::addChild);
        return root;
    }
}
