package net.sf.jabref.model.groups;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import net.sf.jabref.logic.util.OptionalUtil;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.KeywordList;

public class AutomaticGroup extends AbstractGroup {

    public AutomaticGroup(String name, GroupHierarchyType context, String field, Character keywordSeperator) {
        super(name, context);
        this.field = field;
        this.keywordSeperator = keywordSeperator;
    }

    private Character keywordSeperator;
    private String field;

    @Override
    public boolean contains(BibEntry entry) {
        return false;
    }

    @Override
    public boolean isDynamic() {
        return false;
    }

    @Override
    public AbstractGroup deepCopy() {
        return new AutomaticGroup(this.name, this.context, field, this.keywordSeperator);
    }

    public Set<GroupTreeNode> createSubgroups(BibEntry entry) {
        Optional<KeywordList> keywordList = entry.getLatexFreeField(field)
                .map(fieldValue -> KeywordList.parse(fieldValue, keywordSeperator));
        return OptionalUtil.flatMap(keywordList, KeywordList::toStringList)
                .map(keyword -> new WordKeywordGroup(keyword, GroupHierarchyType.INDEPENDENT, field, keyword, true, keywordSeperator, true))
                .map(GroupTreeNode::new)
                .collect(Collectors.toSet());
    }
}
