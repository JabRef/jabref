package org.jabref.model.groups;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.KeywordList;
import org.jabref.model.util.OptionalUtil;

public class AutomaticKeywordGroup extends AutomaticGroup {

    private Character keywordSeperator;
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
    public Set<GroupTreeNode> createSubgroups(BibEntry entry) {
        Optional<KeywordList> keywordList = entry.getLatexFreeField(field)
                .map(fieldValue -> KeywordList.parse(fieldValue, keywordSeperator));
        return OptionalUtil.flatMap(keywordList, KeywordList::toStringList)
                .map(keyword -> new WordKeywordGroup(keyword, GroupHierarchyType.INDEPENDENT, field, keyword, true, keywordSeperator, true))
                .map(GroupTreeNode::new)
                .collect(Collectors.toSet());
    }
}
