package net.sf.jabref.model.groups;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import net.sf.jabref.logic.util.OptionalUtil;
import net.sf.jabref.model.entry.Author;
import net.sf.jabref.model.entry.AuthorList;
import net.sf.jabref.model.entry.BibEntry;

public class AutomaticPersonsGroup extends AutomaticGroup {

    private String field;

    public AutomaticPersonsGroup(String name, GroupHierarchyType context, String field) {
        super(name, context);
        this.field = field;
    }

    @Override
    public AbstractGroup deepCopy() {
        return new AutomaticPersonsGroup(this.name, this.context, this.field);
    }

    @Override
    public Set<GroupTreeNode> createSubgroups(BibEntry entry) {
        Optional<AuthorList> authorList = entry.getLatexFreeField(field)
                .map(AuthorList::parse);
        return OptionalUtil.flatMap(authorList, AuthorList::getAuthors)
                .map(Author::getLast)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(lastName -> !lastName.isEmpty())
                .map(lastName -> new WordKeywordGroup(lastName, GroupHierarchyType.INDEPENDENT, field, lastName, true, ' ', true))
                .map(GroupTreeNode::new)
                .collect(Collectors.toSet());
    }

    public String getField() {
        return field;
    }
}
